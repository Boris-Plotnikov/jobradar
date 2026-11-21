import { Fragment, useEffect, useState } from "react";
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api } from "../api";
import type { Stats } from "../types";
import { PageHeader, Spinner, Stat, Tag, timeAgo } from "../ui";

export default function StatsPage() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [openLog, setOpenLog] = useState<number | null>(null);

  useEffect(() => {
    api<Stats>("/api/stats").then(setStats);
  }, []);

  if (!stats) return <Spinner />;
  const funnel = stats.funnel;
  const applied = funnel.APPLIED + funnel.INTERVIEW + funnel.OFFER + funnel.REJECTED;

  return (
    <div className="space-y-6">
      <PageHeader title="Stats" subtitle="Your search in numbers and the health of the pipeline." />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Saved" value={funnel.SAVED} />
        <Stat label="Applied (total)" value={applied} />
        <Stat label="Interviews" value={funnel.INTERVIEW + funnel.OFFER} hint={applied ? `${Math.round((100 * (funnel.INTERVIEW + funnel.OFFER)) / applied)}% of applications` : undefined} />
        <Stat
          label="AI this month"
          value={`$${Number(stats.aiSpentThisMonth).toFixed(2)}`}
          hint={stats.aiOffReason ?? `${stats.aiCallsThisMonth} calls · budget $${stats.aiMonthlyBudget}`}
        />
      </div>

      <div className="card p-5">
        <h3 className="mb-3 font-semibold">Per week</h3>
        <ResponsiveContainer width="100%" height={280}>
          <BarChart data={stats.weekly}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} />
            <XAxis dataKey="week" fontSize={12} />
            <YAxis fontSize={12} />
            <Tooltip />
            <Legend />
            <Bar dataKey="found" name="Relevant found" fill="#0f766e" radius={[4, 4, 0, 0]} />
            <Bar dataKey="applied" name="Applied" fill="#0ea5e9" radius={[4, 4, 0, 0]} />
            <Bar dataKey="interviews" name="Interviews" fill="#a855f7" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="card overflow-x-auto p-5">
        <h3 className="mb-3 font-semibold">Pipeline runs</h3>
        <table className="w-full min-w-[640px] text-sm">
          <thead>
            <tr className="text-left text-xs uppercase text-muted">
              <th className="py-1">Started</th><th>Trigger</th><th>Status</th><th>Fetched</th><th>New</th><th>Duplicates</th><th>Relevant</th><th>Scored</th><th />
            </tr>
          </thead>
          <tbody>
            {stats.runs.map((r, i) => (
              <Fragment key={r.startedAt}>
                <tr className="border-t border-slate-100">
                  <td className="py-2">{timeAgo(r.startedAt)}</td>
                  <td>{r.trigger.toLowerCase()}</td>
                  <td><Tag tone={r.status === "OK" ? "green" : r.status === "FAILED" ? "red" : "amber"}>{r.status}</Tag></td>
                  <td>{r.fetched}</td><td>{r.new}</td><td>{r.duplicates}</td><td>{r.passed}</td><td>{r.scored}</td>
                  <td><button className="text-xs text-brand" onClick={() => setOpenLog(openLog === i ? null : i)}>log</button></td>
                </tr>
                {openLog === i && (
                  <tr><td colSpan={9}><pre className="whitespace-pre-wrap rounded bg-slate-50 p-3 text-xs">{r.log}</pre></td></tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
