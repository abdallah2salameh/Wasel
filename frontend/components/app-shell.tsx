"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/components/auth-provider";

const links = [
  { href: "/", label: "Overview" },
  { href: "/incidents", label: "Incidents" },
  { href: "/checkpoints", label: "Checkpoints" },
  { href: "/routes", label: "Routing" },
  { href: "/reports", label: "Reports" },
  { href: "/alerts", label: "Alerts" },
  { href: "/auth", label: "Access" }
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { session, ready, logout } = useAuth();

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="eyebrow" style={{ color: "rgba(248,245,236,0.72)", marginBottom: 0 }}>
            Wasel Palestine
          </span>
          <h1>Mobility control room</h1>
          <p style={{ margin: 0, color: "rgba(248,245,236,0.72)" }}>
            A frontend for incident intelligence, public reporting, and route awareness.
          </p>
        </div>

        <nav className="nav-list">
          {links.map((link) => {
            const active = pathname === link.href;
            return (
              <Link key={link.href} href={link.href} className={`nav-link${active ? " active" : ""}`}>
                {link.label}
              </Link>
            );
          })}
        </nav>

        <div className="sidebar-footer">
          <div>
            <div className="eyebrow" style={{ color: "rgba(248,245,236,0.58)" }}>
              Session
            </div>
            {!ready ? (
              <div className="muted" style={{ color: "rgba(248,245,236,0.72)" }}>
                Loading account state...
              </div>
            ) : session ? (
              <div className="grid-list">
                <div>
                  <strong>{session.user.email}</strong>
                  <div style={{ color: "rgba(248,245,236,0.72)" }}>{session.user.role}</div>
                </div>
                <button type="button" className="button-ghost" onClick={logout}>
                  Sign out
                </button>
              </div>
            ) : (
              <div className="grid-list">
                <div style={{ color: "rgba(248,245,236,0.72)" }}>
                  Sign in to manage moderation and personal alerts.
                </div>
                <Link href="/auth" className="button-ghost">
                  Open access center
                </Link>
              </div>
            )}
          </div>

          <div style={{ color: "rgba(248,245,236,0.62)", fontSize: "0.92rem" }}>
            Backend target: <code>http://localhost:8080</code>
          </div>
        </div>
      </aside>

      <main className="main">{children}</main>
    </div>
  );
}
