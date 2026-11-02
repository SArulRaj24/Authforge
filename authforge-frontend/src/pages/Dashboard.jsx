import { useEffect, useState } from "react";
import { Activity, KeyRound, ShieldCheck, UserRound } from "lucide-react";
import { authApi } from "../api/client";
import { useAuth } from "../state/AuthContext";
import Alert from "../components/Alert";

export default function Dashboard() {
  const { user, isAdmin } = useAuth();
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    let active = true;
    authApi.me().then(({ data }) => { if (active) setProfile(data); })
      .catch(err => { if (active) setError(err.response?.data?.message || "Could not load /api/v1/users/me. Check that this endpoint exists in your backend."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);
  return <div className="content-wrap">
    <div className="page-heading"><div><span className="section-kicker">OVERVIEW</span><h1>Your workspace</h1><p className="muted">Authentication status and account information.</p></div><span className="status-pill"><span className="status-dot" /> SESSION ACTIVE</span></div>
    <Alert message={error} />
    <div className="welcome-panel"><div><span className="section-kicker">AUTHENTICATED AS</span><h2>{profile?.fullName || user?.email || "User"}</h2><p>{profile?.email || user?.email}</p></div><div className="welcome-icon"><ShieldCheck size={34} /></div></div>
    <div className="stats-grid">
      <article className="stat-card"><span className="stat-icon"><UserRound size={19} /></span><span className="stat-label">ACCOUNT</span><strong>{loading ? "Loading…" : "Active"}</strong><small>Identity verified by API</small></article>
      <article className="stat-card"><span className="stat-icon"><KeyRound size={19} /></span><span className="stat-label">AUTH METHOD</span><strong>Bearer JWT</strong><small>Access token authentication</small></article>
      <article className="stat-card"><span className="stat-icon"><Activity size={19} /></span><span className="stat-label">ACCESS LEVEL</span><strong>{isAdmin ? "Administrator" : "User"}</strong><small>{user?.roles?.join(", ") || "Role not found in token"}</small></article>
    </div>
    <section className="panel"><div className="panel-heading"><div><h3>Account details</h3><p className="muted">Data returned by your protected profile endpoint.</p></div><span className="mini-tag">API RESPONSE</span></div>
      {loading ? <p className="muted">Loading account…</p> : profile ? <div className="detail-grid">
        <div><label>Full name</label><strong>{profile.fullName || "—"}</strong></div>
        <div><label>Email</label><strong>{profile.email || user?.email || "—"}</strong></div>
        <div><label>User ID</label><strong>{profile.id ?? user?.id ?? "—"}</strong></div>
        <div><label>Roles</label><strong>{Array.isArray(profile.roles) ? profile.roles.join(", ") : user?.roles?.join(", ") || "—"}</strong></div>
      </div> : <p className="muted">Profile endpoint did not return data. The session may still be valid; verify your backend route and response DTO.</p>}
    </section>
    <p className="bottom-note"><ShieldCheck size={15} /> Protected by AuthForge role-based access control.</p>
  </div>;
}