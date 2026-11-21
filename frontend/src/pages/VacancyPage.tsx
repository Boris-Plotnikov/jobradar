import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api";
import type { AppStatus, CoverLetter, Vacancy } from "../types";
import { Button, ErrorBox, ScoreBadge, Spinner, Tag, salary, timeAgo, useToast, workModeLabel } from "../ui";

const STATUSES: AppStatus[] = ["SAVED", "APPLIED", "INTERVIEW", "OFFER", "REJECTED"];

export default function VacancyPage() {
  const { id } = useParams();
  const [vacancy, setVacancy] = useState<Vacancy | null>(null);
  const [letters, setLetters] = useState<CoverLetter[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [toast, showToast] = useToast();

  const load = useCallback(async () => {
    try {
      setVacancy(await api<Vacancy>(`/api/vacancies/${id}`));
      setLetters(await api<CoverLetter[]>(`/api/vacancies/${id}/letters`));
    } catch (e) {
      setError((e as Error).message);
    }
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  const action = async (name: string, fn: () => Promise<unknown>, message: string) => {
    setBusy(name);
    try {
      await fn();
      showToast(message);
      await load();
    } catch (e) {
      showToast((e as Error).message);
    } finally {
      setBusy(null);
    }
  };

  const setStatus = async (status: AppStatus) => {
    const apps = await api<{ id: number; vacancyId: number }[]>("/api/applications");
    let app = apps.find((a) => a.vacancyId === Number(id));
    if (!app) app = await api<{ id: number; vacancyId: number }>("/api/applications", { method: "POST", body: { vacancyId: Number(id) } });
    await api(`/api/applications/${app.id}`, { method: "PATCH", body: { status } });
  };

  if (error) return <ErrorBox message={error} />;
  if (!vacancy) return <Spinner />;
  const v = vacancy;

  return (
    <div>
      {toast}
      <Link to="/feed" className="text-sm text-muted hover:text-brand">← Back to feed</Link>

      <div className="mt-3 grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <div className="card flex gap-5 p-6">
            <ScoreBadge score={v.matchScore} by={v.scoredBy} large />
            <div className="min-w-0">
              <h1 className="text-xl font-bold">{v.title}</h1>
              <div className="mt-1 text-sm text-muted">
                {v.company} · {[v.city, v.country].filter(Boolean).join(", ") || "Location n/a"} · {workModeLabel(v.workMode)} · ad in {v.language}
              </div>
              <div className="mt-3 flex flex-wrap gap-1.5">
                {v.direction && <Tag tone="brand">{v.direction}</Tag>}
                {salary(v.salaryMin, v.salaryMax, v.salaryCurrency) && <Tag tone="amber">💶 {salary(v.salaryMin, v.salaryMax, v.salaryCurrency)}</Tag>}
                {v.skills.map((s) => <Tag key={s}>{s}</Tag>)}
              </div>
            </div>
          </div>

          <div className="card p-6">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="font-semibold">Why this score</h2>
              <Button small variant="ghost" disabled={busy !== null} onClick={() => action("rescore", () => api(`/api/vacancies/${id}/rescore`, { method: "POST" }), "Scored again")}>
                {busy === "rescore" ? "Scoring…" : "↻ Score again"}
              </Button>
            </div>
            {v.matchSummary ? <p className="text-sm">{v.matchSummary}</p> : <p className="text-sm text-muted">Not scored yet.</p>}
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <div>
                <div className="label">Strengths</div>
                <ul className="space-y-1 text-sm">{v.matchStrengths?.map((s) => <li key={s}>✅ {s}</li>)}</ul>
              </div>
              <div>
                <div className="label">Gaps</div>
                <ul className="space-y-1 text-sm">
                  {v.matchGaps?.length ? v.matchGaps.map((s) => <li key={s}>⚠️ {s}</li>) : <li className="text-muted">None found</li>}
                </ul>
              </div>
            </div>
            {v.matchAdvice && <div className="mt-4 rounded-lg bg-brand/5 p-3 text-sm"><b>Tip:</b> {v.matchAdvice}</div>}
            <div className="mt-3 text-xs text-muted">{v.scoredBy === "AI" ? "Analysed by Claude (AI)." : "Keyword estimate — AI was not used for this vacancy."}</div>
          </div>

          <div className="card p-6">
            <h2 className="mb-3 font-semibold">Job description</h2>
            <div className="prose-job">{v.description}</div>
            <div className="mt-4 border-t border-slate-100 pt-3 text-xs text-muted">
              Source: <a href={v.url} target="_blank" rel="noreferrer" className="text-brand underline">{v.source}</a> · collected {timeAgo(v.fetchedAt)}. Apply on the original page.
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="card space-y-3 p-5">
            <a href={v.url} target="_blank" rel="noreferrer" className="flex w-full items-center justify-center rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-dark">
              Open on {v.source} ↗
            </a>
            <div>
              <div className="label">Application status</div>
              <select className="input" value={v.applicationStatus ?? ""} onChange={(e) => action("status", () => setStatus(e.target.value as AppStatus), "Status updated")}>
                <option value="" disabled>Not on the board yet</option>
                {STATUSES.map((s) => <option key={s} value={s}>{s.charAt(0) + s.slice(1).toLowerCase()}</option>)}
              </select>
            </div>
            <Button small variant="secondary" className="w-full" onClick={() => action("hide", () => api(`/api/vacancies/${id}/${v.hidden ? "unhide" : "hide"}`, { method: "POST" }), v.hidden ? "Shown in feed again" : "Hidden from feed")}>
              {v.hidden ? "Show in feed again" : "Hide from feed"}
            </Button>
          </div>

          <LetterPanel vacancyId={Number(id)} letters={letters} defaultLanguage={v.language === "DE" ? "DE" : "EN"} onChange={load} showToast={showToast} />
        </div>
      </div>
    </div>
  );
}

function LetterPanel({ vacancyId, letters, defaultLanguage, onChange, showToast }: {
  vacancyId: number;
  letters: CoverLetter[];
  defaultLanguage: "EN" | "DE";
  onChange: () => Promise<void>;
  showToast: (t: string) => void;
}) {
  const [language, setLanguage] = useState<"EN" | "DE">(defaultLanguage);
  const forLanguage = letters.filter((l) => l.language === language);
  const [selected, setSelected] = useState<CoverLetter | null>(null);
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const latest = letters.filter((l) => l.language === language)[0] ?? null;
    setSelected(latest);
    setText(latest?.body ?? "");
  }, [letters, language]);

  const generate = async () => {
    setBusy(true);
    try {
      const letter = await api<CoverLetter>(`/api/vacancies/${vacancyId}/letters/generate`, { method: "POST", body: { language } });
      showToast(letter.generatedBy === "AI" ? "Letter written by AI" : "AI is off — template letter created");
      await onChange();
    } catch (e) {
      showToast((e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const saveEdit = async () => {
    await api(`/api/vacancies/${vacancyId}/letters`, { method: "POST", body: { language, body: text } });
    showToast("Saved as a new version");
    await onChange();
  };

  return (
    <div className="card space-y-3 p-5">
      <div className="flex items-center justify-between">
        <h2 className="font-semibold">Cover letter</h2>
        <div className="flex rounded-lg border border-slate-200 p-0.5 text-xs">
          {(["EN", "DE"] as const).map((l) => (
            <button key={l} onClick={() => setLanguage(l)} className={`rounded-md px-2 py-1 ${language === l ? "bg-brand text-white" : "text-muted"}`}>{l}</button>
          ))}
        </div>
      </div>
      <Button onClick={generate} disabled={busy} className="w-full">{busy ? "Writing…" : forLanguage.length ? "✨ Write a new version" : "✨ Write cover letter"}</Button>
      {selected && (
        <>
          <select className="input" value={selected.id} onChange={(e) => {
            const l = forLanguage.find((x) => x.id === Number(e.target.value)) ?? null;
            setSelected(l);
            setText(l?.body ?? "");
          }}>
            {forLanguage.map((l) => <option key={l.id} value={l.id}>Version {l.version} · {l.generatedBy.toLowerCase()} · {timeAgo(l.createdAt)}</option>)}
          </select>
          <textarea className="input h-72 font-mono text-xs leading-relaxed" value={text} onChange={(e) => setText(e.target.value)} />
          <div className="flex gap-2">
            <Button small variant="secondary" disabled={text === selected.body} onClick={saveEdit}>Save as new version</Button>
            <Button small variant="ghost" onClick={() => { navigator.clipboard.writeText(text); showToast("Copied"); }}>Copy</Button>
          </div>
        </>
      )}
      <p className="text-xs text-muted">The instructions for the AI are editable in Settings → Letter templates.</p>
    </div>
  );
}
