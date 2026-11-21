import { FormEvent, useState } from "react";
import { api } from "../api";
import { Button, ErrorBox } from "../ui";

export default function LoginPage({ onLogin }: { onLogin: (username: string) => void }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const result = await api<{ username: string }>("/api/auth/login", { method: "POST", body: { username, password } });
      onLogin(result.username);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-brand/10 via-surface to-accent/10 p-4">
      <div className="w-full max-w-sm">
        <div className="mb-6 flex items-center justify-center gap-3">
          <img src="./favicon.svg" alt="" className="h-10 w-10" />
          <div className="text-2xl font-bold">JobRadar</div>
        </div>
        <form onSubmit={submit} className="card space-y-4 p-6">
          <div>
            <label className="label" htmlFor="u">Username</label>
            <input id="u" className="input" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" />
          </div>
          <div>
            <label className="label" htmlFor="p">Password</label>
            <input id="p" type="password" className="input" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
          </div>
          {error && <ErrorBox message={error} />}
          <Button type="submit" disabled={busy || !username || !password} className="w-full">
            {busy ? "Signing in…" : "Sign in"}
          </Button>
        </form>
        <a href="#/showcase" className="mt-6 block rounded-xl border border-dashed border-brand/40 bg-white/60 p-4 text-center text-sm text-brand-dark hover:bg-white">
          <b>Visitor?</b> Open the public showcase — live matches and market insights, no login needed →
        </a>
      </div>
    </div>
  );
}
