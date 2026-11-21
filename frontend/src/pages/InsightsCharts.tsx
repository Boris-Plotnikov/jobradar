import { Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { Insights } from "../types";
import { Stat, Tag } from "../ui";

const BRAND = "#0f766e";
const PALETTE = ["#0f766e", "#f59e0b", "#6366f1", "#ec4899", "#0ea5e9", "#84cc16", "#a855f7"];

export default function InsightsCharts({ data }: { data: Insights }) {
  const trendSkills = data.skillTrends.length ? Object.keys(data.skillTrends[0]).filter((k) => k !== "week") : [];
  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Vacancies analysed" value={data.totalVacancies.toLocaleString()} hint={`last ${data.days} days`} />
        <Stat label="Relevant for junior dev" value={data.relevantVacancies.toLocaleString()} hint={`${data.totalVacancies ? Math.round((100 * data.relevantVacancies) / data.totalVacancies) : 0}% passed the prefilter`} />
        <Stat label="Average match" value={data.avgScore ? `${data.avgScore}%` : "-"} hint="of relevant vacancies" />
        <Stat label="Countries" value={data.countries.length} hint={data.countries.slice(0, 3).map((c) => c.name).join(", ")} />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="card p-5">
          <h3 className="mb-1 font-semibold">Most requested skills - relevant jobs</h3>
          <p className="mb-3 text-xs text-muted">Share of junior / working student vacancies that mention the skill.</p>
          <ResponsiveContainer width="100%" height={340}>
            <BarChart data={data.topSkillsRelevant} layout="vertical" margin={{ left: 20 }}>
              <CartesianGrid strokeDasharray="3 3" horizontal={false} />
              <XAxis type="number" unit="%" fontSize={12} />
              <YAxis type="category" dataKey="name" width={100} fontSize={12} />
              <Tooltip formatter={(v: number) => `${v}%`} />
              <Bar dataKey="share" name="Share" radius={[0, 4, 4, 0]}>
                {data.topSkillsRelevant.map((s) => (
                  <Cell key={s.name} fill={data.cvCoverage.find((c) => c.skill === s.name)?.inCv ? BRAND : "#f59e0b"} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
          <div className="mt-2 flex gap-4 text-xs text-muted">
            <span><span className="mr-1 inline-block h-2 w-2 rounded-full bg-brand" />in the CV</span>
            <span><span className="mr-1 inline-block h-2 w-2 rounded-full bg-amber-500" />learning opportunity</span>
          </div>
        </div>

        <div className="card p-5">
          <h3 className="mb-1 font-semibold">Where the jobs are</h3>
          <p className="mb-3 text-xs text-muted">All collected vacancies by country.</p>
          <ResponsiveContainer width="100%" height={340}>
            <BarChart data={data.countries.map((c) => ({ ...c, label: c.name }))}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="label" fontSize={11} interval={0} angle={-30} textAnchor="end" height={50} />
              <YAxis fontSize={12} />
              <Tooltip />
              <Bar dataKey="count" name="Vacancies" fill={BRAND} radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <PieCard title="Work mode" data={data.workModes} />
        <PieCard title="Language of the ad" data={data.languages} />
        <div className="card p-5">
          <h3 className="mb-3 font-semibold">Directions</h3>
          <div className="space-y-2">
            {data.directions.map((d) => (
              <div key={d.name}>
                <div className="flex justify-between text-sm"><span>{d.name}</span><span className="text-muted">{d.count}</span></div>
                <div className="h-1.5 rounded-full bg-slate-100"><div className="h-1.5 rounded-full bg-brand" style={{ width: `${d.share}%` }} /></div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {trendSkills.length > 0 && data.skillTrends.length > 1 && (
        <div className="card p-5">
          <h3 className="mb-3 font-semibold">Skill demand by week</h3>
          <ResponsiveContainer width="100%" height={280}>
            <LineChart data={data.skillTrends}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="week" fontSize={12} />
              <YAxis fontSize={12} />
              <Tooltip />
              <Legend />
              {trendSkills.map((s, i) => <Line key={s} type="monotone" dataKey={s} stroke={PALETTE[i % PALETTE.length]} strokeWidth={2} dot={false} />)}
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="card p-5">
          <h3 className="mb-3 font-semibold">Salaries (EUR per year, where published)</h3>
          {data.salaries.length === 0 ? <p className="text-sm text-muted">No salary data in this period yet.</p> : (
            <table className="w-full text-sm">
              <thead><tr className="text-left text-xs uppercase text-muted"><th className="py-1">Country</th><th>Jobs</th><th>Avg. from</th><th>Avg. to</th></tr></thead>
              <tbody>
                {data.salaries.map((s) => (
                  <tr key={s.country} className="border-t border-slate-100">
                    <td className="py-1.5">{s.country}</td><td>{s.vacancies}</td>
                    <td>{s.avgMin.toLocaleString()}</td><td>{s.avgMax.toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        <div className="card p-5">
          <h3 className="mb-3 font-semibold">Skill gaps to close</h3>
          <p className="mb-3 text-xs text-muted">In-demand skills that are not in the CV yet - the most useful things to learn next.</p>
          <div className="flex flex-wrap gap-2">
            {data.cvCoverage.filter((c) => !c.inCv).map((c) => <Tag key={c.skill} tone="amber">{c.skill} · {c.demand}</Tag>)}
            {data.cvCoverage.every((c) => c.inCv) && <span className="text-sm text-muted">All top skills are covered.</span>}
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            {data.cvCoverage.filter((c) => c.inCv).map((c) => <Tag key={c.skill} tone="green">✓ {c.skill}</Tag>)}
          </div>
        </div>
      </div>
    </div>
  );
}

function PieCard({ title, data }: { title: string; data: { name: string; count: number }[] }) {
  return (
    <div className="card p-5">
      <h3 className="mb-1 font-semibold">{title}</h3>
      <ResponsiveContainer width="100%" height={200}>
        <PieChart>
          <Pie data={data} dataKey="count" nameKey="name" innerRadius={45} outerRadius={75} paddingAngle={2}>
            {data.map((d, i) => <Cell key={d.name} fill={PALETTE[i % PALETTE.length]} />)}
          </Pie>
          <Tooltip />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}
