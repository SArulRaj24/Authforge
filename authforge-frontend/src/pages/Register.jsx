import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowRight, UserPlus } from "lucide-react";
import AuthShell from "../components/AuthShell";
import Field from "../components/Field";
import Alert from "../components/Alert";
import { useAuth } from "../state/AuthContext";

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: "", email: "", password: "", confirmPassword: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const update = e => setForm({ ...form, [e.target.name]: e.target.value });
  const submit = async e => {
    e.preventDefault(); setError("");
    if (form.password !== form.confirmPassword) { setError("Passwords do not match."); return; }
    if (form.password.length < 8) { setError("Password must contain at least 8 characters."); return; }
    setBusy(true);
    try { await register(form.fullName.trim(), form.email.trim(), form.password); navigate("/dashboard"); }
    catch (err) { setError(err.response?.data?.message || "Registration failed. Please check your details."); }
    finally { setBusy(false); }
  };
  return <AuthShell footer={<>Already registered? <Link to="/login">Sign in</Link></>}>
    <div className="auth-eyebrow"><UserPlus size={15} /> CREATE YOUR ACCOUNT</div>
    <h1>Build your access<span className="text-accent">.</span></h1>
    <p className="muted">Create a secure AuthForge identity.</p>
    <form onSubmit={submit} className="form-stack">
      <Field label="Full name" name="fullName" autoComplete="name" placeholder="Your full name" value={form.fullName} onChange={update} maxLength="100" required />
      <Field label="Email address" name="email" type="email" autoComplete="email" placeholder="you@example.com" value={form.email} onChange={update} required />
      <Field label="Password" name="password" type="password" autoComplete="new-password" placeholder="At least 8 characters" value={form.password} onChange={update} minLength="8" required />
      <Field label="Confirm password" name="confirmPassword" type="password" autoComplete="new-password" placeholder="Re-enter password" value={form.confirmPassword} onChange={update} required />
      <Alert message={error} />
      <button className="btn btn-primary btn-full" disabled={busy}>{busy ? "Creating account…" : "Create account"} <ArrowRight size={17} /></button>
    </form>
    <p className="form-hint">Your account is assigned the default role configured by the AuthForge backend.</p>
  </AuthShell>;
}