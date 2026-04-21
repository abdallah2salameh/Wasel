"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api, formatDate, formatLocation } from "@/lib/api";
import { Checkpoint, Incident, IncidentAnalytics } from "@/lib/types";
import { EmptyState, StatusPill } from "@/components/ui";

export default function HomePage() {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [checkpoints, setCheckpoints] = useState<Checkpoint[]>([]);
  const [analytics, setAnalytics] = useState<IncidentAnalytics | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const [incidentPage, checkpointPage, analyticsResponse] = await Promise.all([
          api.listIncidents(new URLSearchParams({ page: "0", size: "6" })),
          api.listCheckpoints(new URLSearchParams({ page: "0", size: "6" })),
          api.incidentAnalytics()
        ]);
        if (!active) {
          return;
        }
        setIncidents(incidentPage.content);
        setCheckpoints(checkpointPage.content);
        setAnalytics(analyticsResponse);
      } catch (loadError) {
        if (!active) {
          return;
        }
        setError(loadError instanceof Error ? loadError.message : "Failed to load dashboard");
      }
    }

    load();
    return () => {
      active = false;
    };
  }, []);

  const verifiedCount = incidents.filter((incident) => incident.verified).length;
  const checkpointRestrictions = checkpoints.filter((checkpoint) => checkpoint.currentStatus !== "OPEN").length;

  return (
    <div className="page">
      <section className="hero-card">
        <div className="eyebrow">Operational view</div>
        <div className="content-grid" style={{ alignItems: "start" }}>
          <div>
            <h2 className="hero-title">Frontline mobility intelligence for roads, checkpoints, and incident response.</h2>
            <p className="muted" style={{ maxWidth: "62ch", fontSize: "1.04rem" }}>
              This console turns the Spring Boot API into a working field dashboard. Analysts can monitor verified
              incidents, watch checkpoint friction, estimate constrained routes, and move from citizen reports to
              moderation decisions without leaving the web interface.
            </p>
            <div className="split-actions" style={{ marginTop: "1.25rem" }}>
              <Link className="button" href="/incidents">
                Review live incidents
              </Link>
              <Link className="button-ghost" href="/routes">
                Open route planner
              </Link>
            </div>
          </div>

          <div className="feed-card">
            <div className="row-between">
              <h3 className="panel-title">Live pulse</h3>
              <StatusPill tone="warning">API driven</StatusPill>
            </div>
            <div className="feed-list" style={{ marginTop: "1rem" }}>
              <div className="feed-item">
                <strong>{incidents.length} recent incidents</strong>
                <div className="muted">Pulled from `/api/v1/incidents` with pagination.</div>
              </div>
              <div className="feed-item">
                <strong>{verifiedCount} verified right now</strong>
                <div className="muted">Verification status is surfaced directly from the backend state.</div>
              </div>
              <div className="feed-item">
                <strong>{checkpointRestrictions} checkpoint constraints</strong>
                <div className="muted">Anything not `OPEN` is treated as a planning or field-risk signal.</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {error ? <div className="banner error-text">{error}</div> : null}

      <section className="metrics-grid">
        <article className="metric-card">
          <div className="metric-label">Visible incidents</div>
          <div className="metric-value">{incidents.length}</div>
        </article>
        <article className="metric-card">
          <div className="metric-label">Verified incidents</div>
          <div className="metric-value">{verifiedCount}</div>
        </article>
        <article className="metric-card">
          <div className="metric-label">Checkpoints loaded</div>
          <div className="metric-value">{checkpoints.length}</div>
        </article>
        <article className="metric-card">
          <div className="metric-label">Restricted checkpoints</div>
          <div className="metric-value">{checkpointRestrictions}</div>
        </article>
      </section>

      <section className="content-grid">
        <article className="panel">
          <div className="section-row">
            <h3 className="section-title">Recent incidents</h3>
            <Link href="/incidents" className="button-ghost">
              Full incident board
            </Link>
          </div>
          <div className="grid-list" style={{ marginTop: "1rem" }}>
            {incidents.length === 0 ? (
              <EmptyState
                title="No incidents in the current sample"
                description="Once records exist in the backend, they will appear here automatically."
              />
            ) : (
              incidents.map((incident) => (
                <div key={incident.id} className="list-item">
                  <div className="stack-row" style={{ marginBottom: "0.6rem" }}>
                    <StatusPill tone={incident.verified ? "warning" : "neutral"}>{incident.status}</StatusPill>
                    <StatusPill tone={incident.severity === "CRITICAL" ? "alert" : "neutral"}>
                      {incident.severity}
                    </StatusPill>
                    <span className="table-meta">{incident.category}</span>
                  </div>
                  <strong>{incident.title}</strong>
                  <p className="muted" style={{ marginTop: "0.35rem" }}>
                    {incident.description}
                  </p>
                  <div className="table-meta">
                    {formatLocation(incident.latitude, incident.longitude)} | {formatDate(incident.reportedAt)}
                  </div>
                </div>
              ))
            )}
          </div>
        </article>

        <article className="panel">
          <h3 className="section-title">Category pressure</h3>
          <div className="grid-list" style={{ marginTop: "1rem" }}>
            {analytics?.rows.length ? (
              analytics.rows.slice(0, 6).map((row) => (
                <div key={`${row.category}-${row.severity}`} className="list-item">
                  <div className="row-between">
                    <strong>
                      {row.category} / {row.severity}
                    </strong>
                    <StatusPill tone={row.total > 3 ? "alert" : "neutral"}>{row.total}</StatusPill>
                  </div>
                </div>
              ))
            ) : (
              <EmptyState
                title="No analytics rows yet"
                description="The raw SQL summary endpoint will populate this once incidents are stored."
              />
            )}
          </div>
        </article>
      </section>

      <section className="double-grid">
        <article className="panel">
          <div className="section-row">
            <h3 className="section-title">Checkpoint snapshot</h3>
            <Link href="/checkpoints" className="button-ghost">
              View checkpoints
            </Link>
          </div>
          <div className="grid-list" style={{ marginTop: "1rem" }}>
            {checkpoints.length === 0 ? (
              <EmptyState
                title="No checkpoints returned"
                description="Create checkpoints from the backend or admin flow to see their status history here."
              />
            ) : (
              checkpoints.map((checkpoint) => (
                <div key={checkpoint.id} className="list-item">
                  <div className="row-between">
                    <strong>{checkpoint.name}</strong>
                    <StatusPill tone={checkpoint.currentStatus === "OPEN" ? "neutral" : "warning"}>
                      {checkpoint.currentStatus}
                    </StatusPill>
                  </div>
                  <div className="muted">
                    {checkpoint.governorate} | {formatLocation(checkpoint.latitude, checkpoint.longitude)}
                  </div>
                </div>
              ))
            )}
          </div>
        </article>

        <article className="panel">
          <h3 className="section-title">Suggested operator flow</h3>
          <div className="grid-list" style={{ marginTop: "1rem" }}>
            <div className="feed-item">
              <strong>1. Sign in as moderator or admin</strong>
              <div className="muted">Use the access page to obtain a JWT-backed session stored in the frontend.</div>
            </div>
            <div className="feed-item">
              <strong>2. Moderate citizen submissions</strong>
              <div className="muted">Approve or reject reports from the reports board when the role allows it.</div>
            </div>
            <div className="feed-item">
              <strong>3. Create subscriptions for regional alerting</strong>
              <div className="muted">Authenticated users can manage their own alert filters in the alerts page.</div>
            </div>
          </div>
        </article>
      </section>
    </div>
  );
}
