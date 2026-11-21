import { FormEvent, useEffect, useState } from "react";
import { api } from "../api";
import type { PromptTemplate, Settings } from "../types";
import { Button, ErrorBox, PageHeader, Spinner, useToast } from "../ui";

export default function SettingsPage() {
  const [settings, setSettings] = useState<Settings | null>(null);
  const [templates, setTemplates] = useState<PromptTemplate[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [toast, showToast] = useToast();

  useEffect(() => {
    api<Settings>("/api/settings").then(setSettings);
    api<PromptTemplate[]>("/api/templates").then(setTemplates);
  }, []);

  if (!settings) return <Spinner />;

  const set = <K extends keyof Settings>(key: K, value: Settings[K]) => setSettings({ ...settings, [key]: value });

  const save = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      setSettings(await api<Settings>("/api/settings", { method: "PUT", body: settings }));
      showToast("Settings saved");
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const refilter = async () => {
    try {
      await api("/api/pipeline/refilter", { method: "POST" });
      showToast("Re-checking the last 30 days with the new rules…");
    } catch (err) {
      showToast((err as Error).message);
    }
  };

  const area = (key: keyof Settings, label: string, hint: string, rows = 6) => (
    <div>
      <label className="label">{label}</label>
      <textarea className="input font-mono text-xs" rows={rows} value={String(settings[key])} onChange={(e) => set(key, e.target.value as never)} />
      <p className="mt-1 text-xs text-muted">{hint}</p>
    </div>
  );

  return (
    <div className="space-y-6">
      {toast}
      <PageHeader title="Settings" subtitle="What to look for. After changing the rules, re-check already collected vacancies." />
      <form onSubmit={save} className="card space-y-5 p-6">
        {area("resumeText", "CV (plain text)", "The AI compares every vacancy with this text and uses it for cover letters.", 12)}
        <div className="grid gap-5 md:grid-cols-2">
          {area("directions", "Directions", "One per line. Each relevant vacancy is assigned to the closest direction.")}
          {area("countries", "Countries", "ISO codes, one per line. EU = remote jobs open to all of Europe.")}
          {area("keywords", "Keywords", "A vacancy must contain at least one (title, skills or start of the text).")}
          {area("stopWords", "Stop words", "A title with any of these words is dropped (e.g. senior, lead).")}
        </div>
        <div className="grid gap-5 sm:grid-cols-3">
          <div>
            <label className="label">Digest from score</label>
            <input type="number" min={0} max={100} className="input" value={settings.minScore} onChange={(e) => set("minScore", Number(e.target.value))} />
          </div>
          <div>
            <label className="label">Digest hour</label>
            <input type="number" min={0} max={23} className="input" value={settings.digestHour} onChange={(e) => set("digestHour", Number(e.target.value))} />
          </div>
          <label className="flex items-center gap-2 pt-6 text-sm">
            <input type="checkbox" className="accent-brand" checked={settings.remoteOnly} onChange={(e) => set("remoteOnly", e.target.checked)} />
            Remote jobs only
          </label>
        </div>
        {error && <ErrorBox message={error} />}
        <div className="flex flex-wrap gap-2">
          <Button type="submit">Save settings</Button>
          <Button variant="secondary" onClick={refilter}>Re-check last 30 days</Button>
        </div>
      </form>

      <div className="card space-y-4 p-6">
        <div>
          <h2 className="font-semibold">Letter templates</h2>
          <p className="text-sm text-muted">Instructions the AI follows when writing cover letters. Edit them without touching the code.</p>
        </div>
        {templates.map((t) => <TemplateEditor key={t.id} template={t} onSaved={() => showToast("Template saved")} />)}
      </div>
    </div>
  );
}

function TemplateEditor({ template, onSaved }: { template: PromptTemplate; onSaved: () => void }) {
  const [body, setBody] = useState(template.body);
  const save = async () => {
    await api(`/api/templates/${template.id}`, { method: "PUT", body: { body } });
    onSaved();
  };
  return (
    <div>
      <label className="label">{template.title}</label>
      <textarea className="input font-mono text-xs" rows={9} value={body} onChange={(e) => setBody(e.target.value)} />
      <Button small variant="secondary" className="mt-2" disabled={body === template.body} onClick={save}>Save template</Button>
    </div>
  );
}
