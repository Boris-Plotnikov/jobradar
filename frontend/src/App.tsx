import { useEffect, useState } from "react";
import { NavLink, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { api } from "./api";
import { Spinner } from "./ui";
import LoginPage from "./pages/LoginPage";
import FeedPage from "./pages/FeedPage";
import VacancyPage from "./pages/VacancyPage";
import ApplicationsPage from "./pages/ApplicationsPage";
import InsightsPage from "./pages/InsightsPage";
import StatsPage from "./pages/StatsPage";
import SettingsPage from "./pages/SettingsPage";
import ShowcasePage from "./pages/ShowcasePage";
import UnderTheHood from "./codetour/UnderTheHood";

interface Me {
  authenticated: boolean;
  username?: string;
}

const NAV = [
  { to: "/feed", label: "Feed", icon: "📡" },
  { to: "/applications", label: "Applications", icon: "🗂️" },
  { to: "/insights", label: "Market insights", icon: "📊" },
  { to: "/stats", label: "Stats", icon: "📈" },
  { to: "/settings", label: "Settings", icon: "⚙️" },
];

export default function App() {
  const [me, setMe] = useState<Me | null>(null);
  const location = useLocation();

  useEffect(() => {
    api<Me>("/api/auth/me").then(setMe).catch(() => setMe({ authenticated: false }));
  }, []);

  if (location.pathname.startsWith("/showcase")) {
    return (
      <>
        <Routes>
          <Route path="/showcase" element={<ShowcasePage />} />
        </Routes>
        <UnderTheHood screen="/showcase" />
      </>
    );
  }

  if (!me) return <Spinner />;
  if (!me.authenticated) {
    return (
      <>
        <LoginPage onLogin={(username) => setMe({ authenticated: true, username })} />
        <UnderTheHood screen="/login" />
      </>
    );
  }

  const logout = async () => {
    await api("/api/auth/logout", { method: "POST" });
    setMe({ authenticated: false });
  };

  return (
    <div className="min-h-screen md:flex">
      <aside className="border-b border-slate-200 bg-white md:sticky md:top-0 md:h-screen md:w-60 md:shrink-0 md:border-b-0 md:border-r">
        <div className="flex items-center gap-2 px-5 py-4">
          <img src="./favicon.svg" alt="" className="h-8 w-8" />
          <div>
            <div className="font-bold leading-tight">JobRadar</div>
            <div className="text-xs text-muted">Junior jobs in Europe</div>
          </div>
        </div>
        <nav className="flex gap-1 overflow-x-auto px-3 pb-3 md:flex-col md:pb-0">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex shrink-0 items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition ${
                  isActive ? "bg-brand/10 text-brand-dark" : "text-slate-600 hover:bg-slate-100"
                }`
              }
            >
              <span>{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="hidden px-5 py-6 text-xs text-muted md:block">
          <a href="#/showcase" className="block py-1 hover:text-brand">Public showcase ↗</a>
          <a href="./swagger-ui.html" className="block py-1 hover:text-brand">API docs ↗</a>
          <button onClick={logout} className="mt-3 py-1 hover:text-brand">Sign out ({me.username})</button>
        </div>
      </aside>
      <main className="mx-auto w-full max-w-6xl flex-1 p-4 md:p-8">
        <Routes>
          <Route path="/" element={<Navigate to="/feed" replace />} />
          <Route path="/feed" element={<FeedPage />} />
          <Route path="/vacancy/:id" element={<VacancyPage />} />
          <Route path="/applications" element={<ApplicationsPage />} />
          <Route path="/insights" element={<InsightsPage />} />
          <Route path="/stats" element={<StatsPage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="*" element={<Navigate to="/feed" replace />} />
        </Routes>
      </main>
      <UnderTheHood screen={location.pathname} />
    </div>
  );
}
