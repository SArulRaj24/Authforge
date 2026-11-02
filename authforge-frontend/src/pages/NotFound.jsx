import { Link } from "react-router-dom";
export default function NotFound() {
  return <main className="not-found"><span className="section-kicker">404 / NOT FOUND</span><h1>Page not found.</h1><p className="muted">The page you requested does not exist.</p><Link className="btn btn-primary" to="/dashboard">Return to dashboard</Link></main>;
}