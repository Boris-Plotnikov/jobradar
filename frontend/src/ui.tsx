import { ReactNode, useEffect, useState } from "react";
import type { WorkMode } from "./types";

export function Button({
  children, onClick, variant = "primary", type = "button", disabled, small, className = "",
}: {
  children: ReactNode;
  onClick?: () => void;
  variant?: "primary" | "secondary" | "ghost" | "danger";
  type?: "button" | "submit";
  disabled?: boolean;
  small?: boolean;
  className?: string;
}) {
  const styles = {
    primary: "bg-brand text-white hover:bg-brand-dark",
    secondary: "border border-slate-300 bg-white text-ink hover:bg-slate-50",
    ghost: "text-brand hover:bg-brand/10",
    danger: "border border-red-200 bg-white text-red-700 hover:bg-red-50",
  }[variant];
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`inline-flex items-center justify-center gap-2 rounded-lg font-medium transition disabled:cursor-not-allowed disabled:opacity-50 ${
        small ? "px-2.5 py-1 text-xs" : "px-4 py-2 text-sm"
      } ${styles} ${className}`}
    >
      {children}
    </button>
  );
}

export function scoreColor(score?: number): string {
  if (score === undefined || score === null) return "bg-slate-100 text-slate-500";
  if (score >= 80) return "bg-emerald-100 text-emerald-800";
  if (score >= 60) return "bg-teal-100 text-teal-800";
  if (score >= 40) return "bg-amber-100 text-amber-800";
  return "bg-slate-100 text-slate-600";
}

export function ScoreBadge({ score, by, large }: { score?: number; by?: string; large?: boolean }) {
  return (
    <div
      title={by === "AI" ? "Scored by AI" : by === "KEYWORDS" ? "Keyword estimate (no AI)" : "Not scored yet"}
      className={`flex shrink-0 flex-col items-center justify-center rounded-xl font-bold ${scoreColor(score)} ${
        large ? "h-20 w-20 text-3xl" : "h-12 w-12 text-lg"
      }`}
    >
      {score ?? "-"}
      <span className="text-[9px] font-semibold uppercase tracking-wide opacity-70">{by === "AI" ? "AI" : by ? "kw" : ""}</span>
    </div>
  );
}

export function Tag({ children, tone = "slate" }: { children: ReactNode; tone?: "slate" | "brand" | "amber" | "red" | "green" }) {
  const tones = {
    slate: "bg-slate-100 text-slate-700",
    brand: "bg-brand/10 text-brand-dark",
    amber: "bg-amber-100 text-amber-800",
    red: "bg-red-100 text-red-700",
    green: "bg-emerald-100 text-emerald-800",
  }[tone];
  return <span className={`inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium ${tones}`}>{children}</span>;
}

export function workModeLabel(mode: WorkMode): string {
  return { REMOTE: "Remote", HYBRID: "Hybrid", ONSITE: "On-site" }[mode];
}

export function salary(min?: number, max?: number, currency?: string): string | null {
  if (!min && !max) return null;
  const k = (n?: number) => (n ? `${Math.round(n / 1000)}k` : "?");
  return `${k(min)}-${k(max)} ${currency ?? ""}`.trim();
}

export function timeAgo(iso?: string): string {
  if (!iso) return "";
  const minutes = Math.round((Date.now() - new Date(iso).getTime()) / 60000);
  if (minutes < 60) return `${Math.max(minutes, 1)} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} h ago`;
  const days = Math.round(hours / 24);
  return `${days} day${days === 1 ? "" : "s"} ago`;
}

export function Spinner({ label = "Loading…" }: { label?: string }) {
  return (
    <div className="flex items-center gap-3 p-8 text-sm text-muted">
      <span className="h-4 w-4 animate-spin rounded-full border-2 border-brand border-t-transparent" />
      {label}
    </div>
  );
}

export function Empty({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="card flex flex-col items-center gap-2 p-10 text-center">
      <div className="text-3xl">📭</div>
      <div className="font-semibold">{title}</div>
      {children && <div className="max-w-md text-sm text-muted">{children}</div>}
    </div>
  );
}

export function ErrorBox({ message }: { message: string }) {
  return <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800">{message}</div>;
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-muted">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  );
}

export function Stat({ label, value, hint }: { label: string; value: ReactNode; hint?: string }) {
  return (
    <div className="card p-4">
      <div className="text-xs font-semibold uppercase tracking-wide text-muted">{label}</div>
      <div className="mt-1 text-2xl font-bold">{value}</div>
      {hint && <div className="mt-1 text-xs text-muted">{hint}</div>}
    </div>
  );
}

export function useToast(): [ReactNode, (text: string) => void] {
  const [text, setText] = useState<string | null>(null);
  useEffect(() => {
    if (!text) return;
    const timer = setTimeout(() => setText(null), 3000);
    return () => clearTimeout(timer);
  }, [text]);
  const node = text ? (
    <div className="fixed bottom-6 left-1/2 z-50 -translate-x-1/2 rounded-lg bg-ink px-4 py-2 text-sm text-white shadow-lg">{text}</div>
  ) : null;
  return [node, setText];
}
