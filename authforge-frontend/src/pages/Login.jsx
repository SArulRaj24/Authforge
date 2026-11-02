import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowRight, Chrome, LockKeyhole } from "lucide-react";
import AuthShell from "../components/AuthShell";
import Field from "../components/Field";
import Alert from "../components/Alert";
import { useAuth } from "../state/AuthContext";
import { authApi } from "../api/client";

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const update = e => setForm({ ...form, [e.target.name]: e.target.value });
  const submit = async e => {
    e.preventDefault(); setError(""); setBusy(true);
    try { await login(form.email.trim(), form.password); navigate("/dashboard"); }
    catch (err) { setError(err.response?.data?.message || "Unable to sign in. Check your credentials and API server."); }
    finally { setBusy(false); }
  };
  return <AuthShell footer={<>New to AuthForge? <Link to="/register">Create an account</Link></>}>
    <div className="auth-eyebrow"><LockKeyhole size={15} /> SECURE SIGN IN</div>
    <h1>Welcome back<span className="text-accent">.</span></h1>
    <p className="muted">Sign in to access your identity workspace.</p>
    <form onSubmit={submit} className="form-stack">
      <Field label="Email address" name="email" type="email" autoComplete="email" placeholder="you@example.com" value={form.email} onChange={update} required />
      <Field label="Password" name="password" type="password" autoComplete="current-password" placeholder="Enter your password" value={form.password} onChange={update} required />
      <Alert message={error} />
      <button className="btn btn-primary btn-full" disabled={busy}>{busy ? "Signing in…" : "Sign in"} <ArrowRight size={17} /></button>
    </form>
    <div className="or-divider"><span>OR CONTINUE WITH</span></div>
    <a className="btn btn-google btn-full" href={authApi.googleLoginUrl}><Chrome size={17} /> Continue with Google</a>
    <p className="form-hint">Google sign-in requires valid OAuth credentials in your backend configuration.</p>
  </AuthShell>;
}