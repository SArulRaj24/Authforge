import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { Activity, LayoutDashboard, LogOut, ShieldCheck, ScrollText } from "lucide-react";
import Brand from "./Brand";
import { useAuth } from "../state/AuthContext";

export default function Layout() {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();
  const handleLogout = async () => { await logout(); navigate("/login"); };
  return <div className="app-shell">
    <aside className="sidebar">
      <Brand />
      <div className="workspace-label">WORKSPACE</div>
      <nav className="side-nav">
        <NavLink to="/dashboard"><LayoutDashboard size={18} /> Overview</NavLink>
        {isAdmin && <NavLink to="/admin/audit-logs"><ScrollText size={18} /> Audit logs</NavLink>}
      </nav>
      <div className="sidebar-bottom">
        <div className="secure-note"><ShieldCheck size={17} /><span>Protected session<br/><small>JWT authentication</small></span></div>
        <button className="nav-logout" onClick={handleLogout}><LogOut size={17} /> Sign out</button>
      </div>
    </aside>
    <main className="main-area">
      <header className="topbar">
        <div><span className="topbar-kicker">IDENTITY CONSOLE</span><strong>AuthForge</strong></div>
        <div className="profile-chip"><span className="avatar">{(user?.email || "U").charAt(0).toUpperCase()}</span><span>{user?.email}<small>{user?.roles?.join(", ") || "Authenticated user"}</small></span></div>
      </header>
      <div className="page-content"><Outlet /></div>
    </main>
  </div>;
}