import { DragEvent, useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import type { AppStatus, Application } from "../types";
import { Empty, PageHeader, Spinner, scoreColor, timeAgo, useToast } from "../ui";

const COLUMNS: { status: AppStatus; title: string; color: string }[] = [
  { status: "SAVED", title: "Saved", color: "border-t-slate-400" },
  { status: "APPLIED", title: "Applied", color: "border-t-sky-500" },
  { status: "INTERVIEW", title: "Interview", color: "border-t-violet-500" },
  { status: "OFFER", title: "Offer", color: "border-t-emerald-500" },
  { status: "REJECTED", title: "Rejected", color: "border-t-red-400" },
];

export default function ApplicationsPage() {
  const [apps, setApps] = useState<Application[] | null>(null);
  const [dragOver, setDragOver] = useState<AppStatus | null>(null);
  const [toast, showToast] = useToast();

  const load = useCallback(() => api<Application[]>("/api/applications").then(setApps), []);
  useEffect(() => {
    load();
  }, [load]);

  const move = async (id: number, status: AppStatus) => {
    setApps((list) => list?.map((a) => (a.id === id ? { ...a, status } : a)) ?? null);
    try {
      await api(`/api/applications/${id}`, { method: "PATCH", body: { status } });
      load();
    } catch (e) {
      showToast((e as Error).message);
      load();
    }
  };

  const saveNotes = async (id: number, notes: string) => {
    await api(`/api/applications/${id}`, { method: "PATCH", body: { notes } });
    showToast("Notes saved");
  };

  const remove = async (id: number) => {
    await api(`/api/applications/${id}`, { method: "DELETE" });
    load();
  };

  const onDrop = (e: DragEvent, status: AppStatus) => {
    e.preventDefault();
    setDragOver(null);
    const id = Number(e.dataTransfer.getData("text/plain"));
    if (id) move(id, status);
  };

  if (!apps) return <Spinner />;
  const followUps = apps.filter((a) => a.needsFollowUp);

  return (
    <div>
      {toast}
      <PageHeader title="Applications" subtitle="Drag cards between columns. Applied 10+ days ago without an answer? The card asks for a follow-up." />
      {followUps.length > 0 && (
        <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
          ⏰ {followUps.length} application{followUps.length > 1 ? "s" : ""} waiting 10+ days: time to send a short follow-up email.
        </div>
      )}
      {apps.length === 0 ? (
        <Empty title="No applications yet">Save interesting vacancies from the feed with ☆ Save — they appear here.</Empty>
      ) : (
        <div className="grid gap-4 overflow-x-auto pb-4 md:grid-cols-5">
          {COLUMNS.map((col) => {
            const cards = apps.filter((a) => a.status === col.status);
            return (
              <div
                key={col.status}
                onDragOver={(e) => { e.preventDefault(); setDragOver(col.status); }}
                onDragLeave={() => setDragOver(null)}
                onDrop={(e) => onDrop(e, col.status)}
                className={`min-h-[200px] rounded-xl border-t-4 bg-slate-100/70 p-2 transition ${col.color} ${dragOver === col.status ? "ring-2 ring-brand/40" : ""}`}
              >
                <div className="flex items-center justify-between px-2 py-1 text-sm font-semibold">
                  {col.title}<span className="text-xs text-muted">{cards.length}</span>
                </div>
                <div className="space-y-2">
                  {cards.map((a) => (
                    <AppCard key={a.id} app={a} onMove={move} onNotes={saveNotes} onRemove={remove} />
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function AppCard({ app, onMove, onNotes, onRemove }: {
  app: Application;
  onMove: (id: number, s: AppStatus) => void;
  onNotes: (id: number, notes: string) => void;
  onRemove: (id: number) => void;
}) {
  const [open, setOpen] = useState(false);
  const [notes, setNotes] = useState(app.notes ?? "");
  return (
    <div
      draggable
      onDragStart={(e) => e.dataTransfer.setData("text/plain", String(app.id))}
      className={`cursor-grab rounded-lg border bg-white p-3 shadow-sm active:cursor-grabbing ${app.needsFollowUp ? "border-amber-300" : "border-slate-200"}`}
    >
      <div className="flex items-start gap-2">
        <span className={`rounded-md px-1.5 py-0.5 text-xs font-bold ${scoreColor(app.matchScore)}`}>{app.matchScore ?? "–"}</span>
        <Link to={`/vacancy/${app.vacancyId}`} className="text-sm font-medium leading-snug hover:text-brand">{app.title}</Link>
      </div>
      <div className="mt-1 text-xs text-muted">{app.company}{app.country ? ` · ${app.country}` : ""}</div>
      <div className="mt-1 text-xs text-muted">{app.appliedAt ? `Applied ${timeAgo(app.appliedAt)}` : `Moved ${timeAgo(app.statusChangedAt)}`}</div>
      {app.needsFollowUp && <div className="mt-1 text-xs font-medium text-amber-700">⏰ Follow up</div>}
      <select className="mt-2 w-full rounded border border-slate-200 px-1 py-0.5 text-xs md:hidden" value={app.status} onChange={(e) => onMove(app.id, e.target.value as AppStatus)}>
        {COLUMNS.map((c) => <option key={c.status} value={c.status}>{c.title}</option>)}
      </select>
      <button onClick={() => setOpen(!open)} className="mt-2 text-xs text-brand">{open ? "Close" : app.notes ? "📝 Notes" : "+ Notes"}</button>
      {open && (
        <div className="mt-2 space-y-2">
          <textarea className="input h-24 text-xs" value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Contact person, interview date, salary talk…" />
          <div className="flex justify-between">
            <button className="text-xs font-medium text-brand" onClick={() => onNotes(app.id, notes)}>Save</button>
            <button className="text-xs text-red-600" onClick={() => onRemove(app.id)}>Remove card</button>
          </div>
        </div>
      )}
    </div>
  );
}
