import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, query } from "../api";
import type { Page, Vacancy, WorkMode } from "../types";
import { Button, Empty, ErrorBox, PageHeader, ScoreBadge, Spinner, Tag, salary, timeAgo, useToast, workModeLabel } from "../ui";

interface Filters {
  search: string;
  country: string;
  workMode: "" | WorkMode;
  source: string;
  direction: string;
  minScore: number;
  days: number;
  sort: "score" | "date";
  includeRejected: boolean;
}

const DEFAULT_FILTERS: Filters = {
  search: "", country: "", workMode: "", source: "", direction: "", minScore: 0, days: 30, sort: "score", includeRejected: false,
};

export default function FeedPage() {
  const [filters, setFilters] = useState<Filters>(DEFAULT_FILTERS);
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<Vacancy> | null>(null);
  const [options, setOptions] = useState<{ sources: string[]; countries: string[]; directions: string[] }>();
  const [error, setError] = useState<string | null>(null);
  const [running, setRunning] = useState(false);
  const [toast, showToast] = useToast();

  const load = useCallback(async () => {
    setError(null);
    try {
      const result = await api<Page<Vacancy>>(`/api/vacancies${query({ ...filters, workMode: filters.workMode || undefined, page, size: 20 })}`);
      setData(result);
    } catch (e) {
      setError((e as Error).message);
    }
  }, [filters, page]);

  useEffect(() => {
    const timer = setTimeout(load, 250);
    return () => clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    api<typeof options>("/api/vacancies/filters").then(setOptions).catch(() => undefined);
    api<{ running: boolean }>("/api/pipeline/runs").then((r) => setRunning(r.running)).catch(() => undefined);
  }, []);

  useEffect(() => {
    if (!running) return;
    const timer = setInterval(async () => {
      const r = await api<{ running: boolean }>("/api/pipeline/runs");
      if (!r.running) {
        setRunning(false);
        showToast("Pipeline finished");
        load();
      }
    }, 3000);
    return () => clearInterval(timer);
  }, [running, load, showToast]);

  const set = <K extends keyof Filters>(key: K, value: Filters[K]) => {
    setFilters((f) => ({ ...f, [key]: value }));
    setPage(0);
  };

  const runPipeline = async () => {
    try {
      await api("/api/pipeline/run", { method: "POST" });
      setRunning(true);
      showToast("Collecting fresh vacancies…");
    } catch (e) {
      showToast((e as Error).message);
    }
  };

  const save = async (v: Vacancy) => {
    await api("/api/applications", { method: "POST", body: { vacancyId: v.id } });
    showToast(`Saved "${v.title}" to applications`);
    load();
  };

  const hide = async (v: Vacancy) => {
    await api(`/api/vacancies/${v.id}/hide`, { method: "POST" });
    showToast("Hidden from the feed");
    load();
  };

  const total = data?.page?.totalElements ?? data?.totalElements ?? 0;
  const pages = data?.page?.totalPages ?? data?.totalPages ?? 0;

  return (
    <div>
      {toast}
      <PageHeader
        title="Feed"
        subtitle={`${total} relevant vacancies, best matches first. Sources are checked every morning at 06:00.`}
        actions={
          <Button onClick={runPipeline} disabled={running}>
            {running ? (<><span className="h-3 w-3 animate-spin rounded-full border-2 border-white border-t-transparent" /> Running…</>) : "▶ Run pipeline now"}
          </Button>
        }
      />

      <div className="card mb-6 grid gap-3 p-4 sm:grid-cols-2 lg:grid-cols-4">
        <input className="input sm:col-span-2" placeholder="Search title, company, skill…" value={filters.search} onChange={(e) => set("search", e.target.value)} />
        <select className="input" value={filters.country} onChange={(e) => set("country", e.target.value)}>
          <option value="">All countries</option>
          {options?.countries.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
        <select className="input" value={filters.workMode} onChange={(e) => set("workMode", e.target.value as Filters["workMode"])}>
          <option value="">Any work mode</option>
          <option value="REMOTE">Remote</option>
          <option value="HYBRID">Hybrid</option>
          <option value="ONSITE">On-site</option>
        </select>
        <select className="input" value={filters.direction} onChange={(e) => set("direction", e.target.value)}>
          <option value="">All directions</option>
          {options?.directions.map((d) => <option key={d} value={d}>{d}</option>)}
        </select>
        <select className="input" value={filters.source} onChange={(e) => set("source", e.target.value)}>
          <option value="">All sources</option>
          {options?.sources.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
        <select className="input" value={filters.days} onChange={(e) => set("days", Number(e.target.value))}>
          <option value={3}>Last 3 days</option>
          <option value={7}>Last 7 days</option>
          <option value={30}>Last 30 days</option>
          <option value={365}>All time</option>
        </select>
        <select className="input" value={filters.sort} onChange={(e) => set("sort", e.target.value as Filters["sort"])}>
          <option value="score">Best match first</option>
          <option value="date">Newest first</option>
        </select>
        <label className="flex items-center gap-3 text-sm sm:col-span-2">
          <span className="whitespace-nowrap text-muted">Min. match {filters.minScore}%</span>
          <input type="range" min={0} max={90} step={10} value={filters.minScore} onChange={(e) => set("minScore", Number(e.target.value))} className="w-full accent-brand" />
        </label>
        <label className="flex items-center gap-2 text-sm text-muted sm:col-span-2">
          <input type="checkbox" checked={filters.includeRejected} onChange={(e) => set("includeRejected", e.target.checked)} className="accent-brand" />
          Show vacancies rejected by the prefilter (with the reason)
        </label>
      </div>

      {error && <ErrorBox message={error} />}
      {!data && !error && <Spinner />}
      {data && data.content.length === 0 && (
        <Empty title="No vacancies for these filters">Lower the minimum match, choose a longer period or run the pipeline.</Empty>
      )}

      <div className="space-y-3">
        {data?.content.map((v) => (
          <div key={v.id} className={`card flex gap-4 p-4 transition hover:border-brand/40 ${v.prefilterPassed ? "" : "opacity-60"}`}>
            <ScoreBadge score={v.matchScore} by={v.scoredBy} />
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div className="min-w-0">
                  <Link to={`/vacancy/${v.id}`} className="font-semibold hover:text-brand">{v.title}</Link>
                  <div className="text-sm text-muted">
                    {v.company} · {[v.city, v.country].filter(Boolean).join(", ") || "Location n/a"} · {workModeLabel(v.workMode)} · {v.language}
                    {salary(v.salaryMin, v.salaryMax, v.salaryCurrency) && <> · 💶 {salary(v.salaryMin, v.salaryMax, v.salaryCurrency)}</>}
                  </div>
                </div>
                <div className="text-right text-xs text-muted">
                  via {v.source}<br />{timeAgo(v.fetchedAt)}
                </div>
              </div>
              {v.matchSummary && <p className="mt-2 text-sm text-slate-700">{v.matchSummary}</p>}
              {!v.prefilterPassed && v.prefilterReason && <p className="mt-2 text-sm text-red-700">Rejected: {v.prefilterReason}</p>}
              <div className="mt-2 flex flex-wrap items-center gap-1.5">
                {v.direction && v.direction !== "Other" && <Tag tone="brand">{v.direction}</Tag>}
                {v.skills.slice(0, 8).map((s) => <Tag key={s}>{s}</Tag>)}
                {v.applicationStatus && <Tag tone="green">✓ {v.applicationStatus.toLowerCase()}</Tag>}
                <span className="ml-auto flex gap-1">
                  {!v.applicationStatus && <Button small variant="secondary" onClick={() => save(v)}>☆ Save</Button>}
                  <Button small variant="ghost" onClick={() => hide(v)}>Hide</Button>
                  <a href={v.url} target="_blank" rel="noreferrer" className="rounded-lg px-2.5 py-1 text-xs font-medium text-brand hover:bg-brand/10">Original ↗</a>
                </span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {pages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-3 text-sm">
          <Button small variant="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>← Previous</Button>
          <span className="text-muted">Page {page + 1} of {pages}</span>
          <Button small variant="secondary" disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>Next →</Button>
        </div>
      )}
    </div>
  );
}
