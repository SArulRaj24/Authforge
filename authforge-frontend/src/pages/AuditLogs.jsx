import { useEffect, useState } from "react";
import { RefreshCw, Search, ScrollText } from "lucide-react";
import { api } from "../api/client";
import Alert from "../components/Alert";

export default function AuditLogs() {
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [eventType, setEventType] = useState("");
  const [userId, setUserId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const load = async (nextPage = page) => {
    setBusy(true); setError("");
    try {
      const params = { page: nextPage, size: 10 };
      if (eventType.trim()) params.eventType = eventType.trim();
      if (userId.trim()) params.userId = userId.trim();
      const { data } = await api.get("/api/v1/admin/audit-logs", { params });
      setRows(data.content || []);
      setTotalPages(data.totalPages || 0);
      setPage(data.number ?? nextPage);
    } catch (err) {
      setError(err.response?.data?.message || "Unable to load audit logs. Verify that this account has ROLE_ADMIN.");
    } finally { setBusy(false); }
  };

  useEffect(() => { load(0); }, []);

  const submit = e => { e.preventDefault(); load(0); };
  return <div className="content-wrap">
    <div className="page-heading"><div><span className="section-kicker">ADMINISTRATION</span><h1>Audit logs</h1><p className="muted">Review authentication and account activity.</p></div><span className="mini-tag"><ScrollText size={14} /> ADMIN ONLY</span></div>
    <Alert message={error} />
    <section className="panel">
      <form className="filter-row" onSubmit={submit}>
        <label className="filter-field"><span>Event type</span><input value={eventType} onChange={e => setEventType(e.target.value)} placeholder="LOGIN_SUCCESS" /></label>
        <label className="filter-field"><span>User ID</span><input value={userId} onChange={e => setUserId(e.target.value)} type="number" min="1" placeholder="Any user" /></label>
        <button className="btn btn-primary" disabled={busy}><Search size={16} /> Filter</button>
        <button type="button" className="btn btn-secondary" onClick={() => { setEventType(""); setUserId(""); setTimeout(() => load(0), 0); }}><RefreshCw size={16} /> Reset</button>
      </form>
      <div className="table-top"><div><h3>Activity stream</h3><p className="muted">Newest events first</p></div><button className="icon-button" onClick={() => load(page)} title="Refresh"><RefreshCw size={17} /></button></div>
      <div className="table-scroll"><table><thead><tr><th>Event</th><th>User ID</th><th>IP address</th><th>Details</th><th>Timestamp</th></tr></thead>
        <tbody>{busy ? <tr><td colSpan="5" className="empty-cell">Loading audit events…</td></tr> : rows.length === 0 ? <tr><td colSpan="5" className="empty-cell">No audit events found.</td></tr> : rows.map(row => <tr key={row.id}><td><span className="event-pill">{row.eventType}</span></td><td>{row.userId ?? "—"}</td><td>{row.ipAddress || "—"}</td><td className="details-cell">{row.details || "—"}</td><td>{row.createdAt ? new Date(row.createdAt).toLocaleString() : "—"}</td></tr>)}</tbody>
      </table></div>
      <div className="pagination"><span>Page {totalPages ? page + 1 : 0} of {totalPages}</span><div><button className="btn btn-secondary btn-small" disabled={page <= 0 || busy} onClick={() => load(page - 1)}>Previous</button><button className="btn btn-secondary btn-small" disabled={page + 1 >= totalPages || busy} onClick={() => load(page + 1)}>Next</button></div></div>
    </section>
  </div>;
}