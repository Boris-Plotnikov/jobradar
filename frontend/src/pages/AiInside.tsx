import { ReactNode, useEffect, useState } from "react";
import { api, query } from "../api";
import { openTour } from "../codetour/store";
import type { AiTrace } from "../types";
import { ScoreBadge, Spinner } from "../ui";

function usd(value: number): string {
  if (value === 0) return "$0";
  return value < 0.01 ? `$${value.toFixed(5)}` : `$${value.toFixed(2)}`;
}

function Panel({ step, title, children }: { step: number; title: string; children: ReactNode }) {
  return (
    <div className="card flex flex-col p-5">
      <div className="mb-3 flex items-center gap-2">
        <span className="flex h-6 w-6 items-center justify-center rounded-full bg-brand text-xs font-bold text-white">{step}</span>
        <h3 className="font-semibold">{title}</h3>
      </div>
      {children}
    </div>
  );
}

function Mono({ children, className = "" }: { children: ReactNode; className?: string }) {
  return <pre className={`overflow-auto whitespace-pre-wrap rounded-lg bg-slate-50 p-3 font-mono text-[12px] leading-relaxed text-slate-700 ${className}`}>{children}</pre>;
}

function Figure({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="rounded-lg bg-slate-50 p-3">
      <div className="text-[11px] uppercase tracking-wide text-muted">{label}</div>
      <div className="mt-0.5 font-mono text-sm font-semibold">{value}</div>
    </div>
  );
}

export default function AiInside() {
  const [trace, setTrace] = useState<AiTrace | null>(null);
  const [vacancyId, setVacancyId] = useState<number | undefined>();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<AiTrace>("/api/public/ai-trace" + query({ vacancyId }))
      .then((t) => { setTrace(t); setError(null); })
      .catch((e) => setError(e.message));
  }, [vacancyId]);

  if (error) return <p className="text-sm text-muted">{error}</p>;
  if (!trace) return <Spinner />;
  if (!trace.answer || !trace.userPrompt) {
    return <p className="text-sm text-muted">No job on this page has been scored by AI yet.</p>;
  }

  const answerJson = JSON.stringify(trace.answer, null, 2);
  const u = trace.usage;
  const budgetShare = u.monthlyLimitUsd > 0 ? Math.min(100, (u.spentThisMonthUsd / u.monthlyLimitUsd) * 100) : 0;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <label className="text-sm text-muted" htmlFor="ai-job">Job</label>
        <select
          id="ai-job"
          className="input max-w-xl"
          value={trace.vacancyId}
          onChange={(e) => setVacancyId(Number(e.target.value))}
        >
          {trace.candidates.map((c) => (
            <option key={c.id} value={c.id}>{c.score} · {c.title} - {c.company}</option>
          ))}
        </select>
        <button onClick={() => openTour("ai")} className="text-sm font-medium text-brand hover:underline">
          Code behind this →
        </button>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Panel step={1} title="What is sent to Claude">
          <div className="mb-2 flex flex-wrap gap-1.5 text-[11px]">
            <span className="rounded bg-fuchsia-100 px-2 py-0.5 font-mono text-fuchsia-800">{trace.model}</span>
            <span className="rounded bg-slate-100 px-2 py-0.5 text-slate-700">structured output</span>
            <span className="rounded bg-slate-100 px-2 py-0.5 text-slate-700">prompt caching</span>
          </div>
          <div className="label">System prompt</div>
          <Mono className="max-h-56">{trace.instructions}</Mono>
          <div className="mt-2 rounded-lg border border-dashed border-slate-300 p-3 text-xs text-muted">
            Candidate CV: {trace.cvCharacters.toLocaleString()} characters, hidden on this page. The system prompt is identical
            for every job, so after the first call it is read from the cache at 10% of the price.
          </div>
          <div className="label mt-4">User message</div>
          <Mono className="max-h-64">{trace.userPrompt}</Mono>
        </Panel>

        <div className="flex flex-col gap-4">
          <Panel step={2} title="Required answer shape">
            <p className="mb-3 text-xs text-muted">
              Generated from the Java record <span className="font-mono">MatchResult</span>. The API is forced to answer in exactly
              this JSON shape; the backend validates it before saving.
            </p>
            <table className="w-full text-left text-xs">
              <tbody>
                {trace.schema.map((f) => (
                  <tr key={f.name} className="border-t border-slate-100 align-top">
                    <td className="py-1.5 pr-3 font-mono font-semibold">{f.name}</td>
                    <td className="py-1.5 pr-3 font-mono text-muted">{f.type}</td>
                    <td className="py-1.5 text-slate-600">{f.description}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Panel>

          <Panel step={3} title="What came back">
            <div className="flex gap-4">
              <div className="flex flex-col items-center gap-1">
                <ScoreBadge score={trace.answer.score} by="AI" large />
                <span className="text-[11px] text-muted">AI</span>
              </div>
              <div className="flex flex-col items-center gap-1">
                <ScoreBadge score={trace.keywordScore} by="KEYWORDS" large />
                <span className="text-[11px] text-muted">keywords</span>
              </div>
              <p className="text-xs leading-relaxed text-muted">
                Keyword scoring is the fallback when the AI is off or the monthly budget is used up. It needs no API, but only
                counts skills; the AI also weighs seniority, hours and location.
              </p>
            </div>
            <Mono className="mt-3 max-h-60">{answerJson}</Mono>
          </Panel>
        </div>
      </div>

      <Panel step={4} title="What it cost">
        {trace.call ? (
          <div className="grid grid-cols-2 gap-2 md:grid-cols-6">
            <Figure label="Input tokens" value={trace.call.inputTokens.toLocaleString()} />
            <Figure label="From cache" value={trace.call.cacheReadTokens.toLocaleString()} />
            <Figure label="Written to cache" value={trace.call.cacheWriteTokens.toLocaleString()} />
            <Figure label="Output tokens" value={trace.call.outputTokens.toLocaleString()} />
            <Figure label="Cost" value={usd(trace.call.costUsd)} />
            <Figure label="Time" value={trace.call.durationMs != null ? `${(trace.call.durationMs / 1000).toFixed(1)} s` : "-"} />
          </div>
        ) : (
          <p className="text-xs text-muted">This job was scored before calls were logged per job; the averages below cover all jobs.</p>
        )}
        <div className="mt-4 grid grid-cols-2 gap-2 md:grid-cols-4">
          <Figure label="Jobs scored, 30 days" value={u.scoringCalls.toLocaleString()} />
          <Figure label="Average per job" value={usd(u.avgCostUsd)} />
          <Figure label="Total, 30 days" value={usd(u.totalCostUsd)} />
          <Figure
            label="Without prompt caching"
            value={<>{usd(u.costWithoutCacheUsd)} {u.cacheSavingPercent > 0 && <span className="text-emerald-700">(−{u.cacheSavingPercent}%)</span>}</>}
          />
          <Figure label="Avg tokens in / cached / out" value={`${u.avgInputTokens} / ${u.avgCacheReadTokens} / ${u.avgOutputTokens}`} />
          <Figure label="Cover letters, 30 days" value={`${u.letterCalls} · ${usd(u.avgLetterCostUsd)} each`} />
          <div className="col-span-2 rounded-lg bg-slate-50 p-3">
            <div className="flex justify-between text-[11px] uppercase tracking-wide text-muted">
              <span>Monthly AI budget</span>
              <span className="font-mono normal-case">{usd(u.spentThisMonthUsd)} of {usd(u.monthlyLimitUsd)}</span>
            </div>
            <div className="mt-2 h-2 overflow-hidden rounded-full bg-slate-200">
              <div className="h-full rounded-full bg-brand" style={{ width: `${Math.max(budgetShare, 1)}%` }} />
            </div>
          </div>
        </div>
      </Panel>
    </div>
  );
}
