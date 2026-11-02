import { AlertCircle, CheckCircle2 } from "lucide-react";
export default function Alert({ message, type = "error" }) {
  if (!message) return null;
  const Icon = type === "success" ? CheckCircle2 : AlertCircle;
  return <div className={`alert alert-${type}`} role="alert"><Icon size={17} />{message}</div>;
}