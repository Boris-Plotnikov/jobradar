import { useEffect, useState } from "react";
import { api } from "../api";
import type { Insights } from "../types";
import { ErrorBox, PageHeader, Spinner } from "../ui";
import InsightsCharts from "./InsightsCharts";

export default function InsightsPage() {
  const [days, setDays] = useState(30);
  const [data, setData] = useState<Insights | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setData(null);
    api<Insights>(`/api/insights?days=${days}`).then(setData).catch((e) => setError(e.message));
  }, [days]);

  return (
    <div>
      <PageHeader
        title="Market insights"
        subtitle="What the European junior market asks for, calculated from every collected vacancy."
        actions={
          <select className="input w-40" value={days} onChange={(e) => setDays(Number(e.target.value))}>
            <option value={7}>Last 7 days</option>
            <option value={30}>Last 30 days</option>
            <option value={90}>Last 90 days</option>
          </select>
        }
      />
      {error && <ErrorBox message={error} />}
      {!data && !error && <Spinner />}
      {data && <InsightsCharts data={data} />}
    </div>
  );
}
