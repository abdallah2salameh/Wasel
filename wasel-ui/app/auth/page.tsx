"use client";

import { FormEvent, startTransition, useState } from "react";
import { useAuth } from "@/components/auth-provider";

export default function AuthPage() {
  const { login, logout, ready, register, session } = useAuth();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("admin@wasel.local");
  const [password, setPassword] = useState("ChangeMe123!");
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setError(null);
    setMessage(null);

    try {
      if (mode === "login") {
        await login(email, password);
        startTransition(() => {
          setMessage("Session established. Moderator and alert flows are now unlocked.");
        });
      } else {
        await register(email, password);
        startTransition(() => {
          setMessage("Account created and signed in.");
        });
      }
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Authentication failed");
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="page">
      <section className="hero-card">
        <div className="eyebrow">Access center</div>
        <div className="double-grid">
          <div>
            <h2 className="hero-title" style={{ maxWidth: "10ch" }}>
              Secure operator access for citizens, moderators, and admins.
            </h2>
            <p className="muted" style={{ maxWidth: "60ch" }}>
              The frontend uses the backend&apos;s JWT login and refresh flow. Citizens can authenticate for alerts and
              report voting, while moderators and admins gain access to protected workflows like reviewing reports.
            </p>
          </div>

          <div className="panel">
            <div className="stack-row" style={{ marginBottom: "1rem" }}>
              <button
                type="button"
                className={mode === "login" ? "button" : "button-ghost"}
                onClick={() => setMode("login")}
              >
                Sign in
              </button>
              <button
                type="button"
                className={mode === "register" ? "button secondary" : "button-ghost"}
                onClick={() => setMode("register")}
              >
                Register
              </button>
            </div>

            <form onSubmit={onSubmit} className="grid-list">
              <div className="field">
                <label htmlFor="email">Email</label>
                <input id="email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
              </div>
              <div className="field">
                <label htmlFor="password">Password</label>
                <input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
              </div>
              <div className="split-actions">
                <button type="submit" className="button" disabled={pending}>
                  {pending ? "Working..." : mode === "login" ? "Sign in" : "Create account"}
                </button>
                {session ? (
                  <button type="button" className="button-ghost" onClick={logout}>
                    Sign out current user
                  </button>
                ) : null}
              </div>
            </form>

            {message ? <div className="banner success-text">{message}</div> : null}
            {error ? <div className="banner error-text">{error}</div> : null}
          </div>
        </div>
      </section>

      <section className="double-grid">
        <article className="panel">
          <h3 className="section-title">Current session</h3>
          {!ready ? (
            <div className="muted" style={{ marginTop: "1rem" }}>
              Checking local session storage...
            </div>
          ) : session ? (
            <div className="grid-list" style={{ marginTop: "1rem" }}>
              <div className="list-item">
                <strong>{session.user.email}</strong>
                <div className="muted">{session.user.role}</div>
              </div>
              <div className="list-item">
                <strong>Access token available</strong>
                <div className="muted">Stored client-side for authenticated requests to the Spring Boot API.</div>
              </div>
            </div>
          ) : (
            <div className="empty-state" style={{ marginTop: "1rem" }}>
              <strong>No active session</strong>
              <p style={{ marginBottom: 0 }}>Sign in with the seeded admin account or register a citizen account.</p>
            </div>
          )}
        </article>

        <article className="panel">
          <h3 className="section-title">Useful test identities</h3>
          <div className="grid-list" style={{ marginTop: "1rem" }}>
            <div className="feed-item">
              <strong>Seeded admin</strong>
              <div className="muted">Email: `admin@wasel.local`</div>
              <div className="muted">Password: `ChangeMe123!`</div>
            </div>
            <div className="feed-item">
              <strong>Citizen account</strong>
              <div className="muted">Use register to create one for alert subscriptions and report voting.</div>
            </div>
          </div>
        </article>
      </section>
    </div>
  );
}
