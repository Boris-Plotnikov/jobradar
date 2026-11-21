import { useEffect, useState } from "react";
import { api } from "../api";
import type { Insights, Summary, Vacancy } from "../types";
import { ScoreBadge, Spinner, Tag, timeAgo, workModeLabel } from "../ui";
import InsightsCharts from "./InsightsCharts";
import AiInside from "./AiInside";
import { openTour } from "../codetour/store";
import { useFlows } from "../codetour/UnderTheHood";

const STEPS = [
  { icon: "📥", title: "Collect", flow: "pipeline", text: "Every morning the service calls the open APIs of Arbeitnow, Remotive, Jobicy and Himalayas." },
  { icon: "🧹", title: "Normalize & dedupe", flow: "pipeline", text: "Country, city, work mode, language and skills are extracted; the same job from two sources is merged." },
  { icon: "🔎", title: "Prefilter", flow: "pipeline", text: "Rules drop senior roles, wrong countries and other professions — no AI cost for them." },
  { icon: "🤖", title: "AI match", flow: "ai", text: "Claude Haiku compares each remaining job with the CV and returns a score, strengths and gaps as strict JSON." },
  { icon: "📬", title: "Act", flow: "applications", text: "Best matches go to Telegram; applications are tracked on a board; cover letters are drafted in English or German." },
];

export default function ShowcasePage() {
  const [summary, setSummary] = useState<Summary | null>(null);
  const [feed, setFeed] = useState<Vacancy[] | null>(null);
  const [insights, setInsights] = useState<Insights | null>(null);
  const flows = useFlows();

  useEffect(() => {
    api<Summary>("/api/public/summary").then(setSummary);
    api<Vacancy[]>("/api/public/feed").then(setFeed);
    api<Insights>("/api/public/insights").then(setInsights);
  }, []);

  return (
    <div className="min-h-screen">
      <header className="bg-gradient-to-br from-brand-dark to-brand text-white">
        <div className="mx-auto max-w-6xl px-4 py-12">
          <div className="flex items-center gap-3">
            <img src="./favicon.svg" alt="" className="h-10 w-10 rounded-lg ring-2 ring-white/30" />
            <span className="text-lg font-semibold">JobRadar</span>
            <span className="ml-auto text-sm"><a href="#/" className="opacity-80 hover:opacity-100">Owner sign in →</a></span>
          </div>
          <h1 className="mt-8 max-w-3xl text-3xl font-bold leading-tight md:text-4xl">
            A real service that finds junior developer jobs in Europe every day and explains how well each one fits.
          </h1>
          <p className="mt-3 max-w-2xl text-white/80">
            Built by Boris Plotnikov with Java 21 and Spring Boot. This page shows live data from the running system; personal notes, letters and application statuses stay private.
          </p>
          {summary ? (
            <div className="mt-8 grid max-w-3xl grid-cols-2 gap-4 md:grid-cols-4">
              <Counter value={summary.processedLast30Days} label="jobs analysed in 30 days" />
              <Counter value={summary.relevantLast30Days} label="relevant for a junior" />
              <Counter value={summary.strongMatchesLast30Days} label="matches above 80%" />
              <Counter value={summary.sources.length} label={`sources: ${summary.sources.join(", ")}`} />
            </div>
          ) : <Spinner />}
          {summary?.lastRunAt && <p className="mt-4 text-xs text-white/70">Last pipeline run {timeAgo(summary.lastRunAt)}.</p>}
        </div>
      </header>

      <main className="mx-auto max-w-6xl space-y-12 px-4 py-10">
        <section>
          <h2 className="mb-4 text-xl font-bold">How it works</h2>
          <div className="grid gap-3 md:grid-cols-5">
            {STEPS.map((s, i) => (
              <button key={s.title} onClick={() => openTour(s.flow)} className="card group relative p-4 text-left transition hover:border-brand/40">
                <div className="text-2xl">{s.icon}</div>
                <div className="mt-2 text-sm font-semibold">{i + 1}. {s.title}</div>
                <p className="mt-1 text-xs leading-relaxed text-muted">{s.text}</p>
                <div className="mt-2 font-mono text-[11px] text-brand opacity-70 group-hover:opacity-100">&lt;/&gt; see the code</div>
              </button>
            ))}
          </div>
          <div className="mt-3 flex flex-wrap gap-2 text-xs">
            {["Java 21", "Spring Boot 3", "Spring Data JPA", "PostgreSQL", "Flyway", "Claude API (structured output, prompt caching)", "React + TypeScript", "Docker", "GitHub Actions"].map((t) => <Tag key={t}>{t}</Tag>)}
          </div>
        </section>

        <section>
          <h2 className="mb-1 text-xl font-bold">AI inside: one real request</h2>
          <p className="mb-4 text-sm text-muted">
            The exact prompt, the answer schema, the answer and its cost for a job from the list below.
          </p>
          <AiInside />
        </section>

        <section>
          <h2 className="mb-1 text-xl font-bold">Code tour</h2>
          <p className="mb-4 text-sm text-muted">
            How each feature is built, layer by layer: controller, service, repository and the test that checks it.
            The snippets are the real source code, taken from the repository when the app is built.
          </p>
          {!flows ? <Spinner /> : (
            <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
              {flows.map((f) => (
                <button key={f.id} onClick={() => openTour(f.id)} className="card p-4 text-left transition hover:border-brand/40">
                  <div className="font-semibold">{f.title}</div>
                  <p className="mt-1 line-clamp-3 text-xs leading-relaxed text-muted">{f.summary}</p>
                  <div className="mt-3 font-mono text-[11px] text-brand">
                    {f.steps.length} snippets · {f.steps.reduce((n, st) => n + st.code.split("\n").length, 0)} lines →
                  </div>
                </button>
              ))}
            </div>
          )}
        </section>

        <section>
          <h2 className="mb-1 text-xl font-bold">Best matches of the last 14 days</h2>
          <p className="mb-4 text-sm text-muted">Scored against Boris's CV. Links go to the original job ads.</p>
          {!feed ? <Spinner /> : (
            <div className="grid gap-3 md:grid-cols-2">
              {feed.slice(0, 12).map((v) => (
                <a key={v.id} href={v.url} target="_blank" rel="noreferrer" className="card flex gap-3 p-4 hover:border-brand/40">
                  <ScoreBadge score={v.matchScore} by={v.scoredBy} />
                  <div className="min-w-0">
                    <div className="truncate font-medium">{v.title}</div>
                    <div className="text-xs text-muted">{v.company} · {v.country ?? "Europe"} · {workModeLabel(v.workMode)} · via {v.source}</div>
                    {v.matchSummary && <p className="mt-1 line-clamp-2 text-xs text-slate-600">{v.matchSummary}</p>}
                  </div>
                </a>
              ))}
            </div>
          )}
        </section>

        <section>
          <h2 className="mb-4 text-xl font-bold">Market insights</h2>
          {!insights ? <Spinner /> : <InsightsCharts data={insights} />}
        </section>

        <footer className="border-t border-slate-200 pt-6 text-xs text-muted">
          Job data: Arbeitnow, Remotive, Jobicy and <a href="https://himalayas.app" className="text-brand underline">Himalayas</a> — used via their public APIs with links to the original ads. No scraping of job boards.
          {" "}<a href="./swagger-ui.html" className="text-brand underline">API documentation</a>
        </footer>
      </main>
    </div>
  );
}

function Counter({ value, label }: { value: number; label: string }) {
  return (
    <div className="rounded-xl bg-white/10 p-4 backdrop-blur">
      <div className="text-3xl font-bold">{value.toLocaleString()}</div>
      <div className="mt-1 text-xs text-white/80">{label}</div>
    </div>
  );
}
