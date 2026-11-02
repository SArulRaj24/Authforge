import { ShieldCheck } from "lucide-react";
export default function Brand({ compact = false }) {
  return <div className={`brand ${compact ? "brand-compact" : ""}`}>
    <span className="brand-mark"><ShieldCheck size={22} /></span>
    <span>Auth<span className="brand-accent">Forge</span></span>
  </div>;
}