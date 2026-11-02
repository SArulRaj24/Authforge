import Brand from "./Brand";
export default function AuthShell({ children, footer }) {
  return <main className="auth-page">
    <div className="auth-glow auth-glow-one" /><div className="auth-glow auth-glow-two" />
    <div className="auth-wrap">
      <Brand />
      <section className="auth-card">{children}</section>
      {footer && <p className="auth-footer">{footer}</p>}
      <p className="auth-legal">Secure identity · Access management · AuthForge</p>
    </div>
  </main>;
}