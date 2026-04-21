"use client";

import { useDeferredValue, useEffect, useMemo, useState } from "react";
import { api, formatDate, formatLocation } from "@/lib/api";
import { Incident } from "@/lib/types";
import { EmptyState, StatusPill } from "@/components/ui";

export default function IncidentsPage() {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [category, setCategory] = useState("");
  const [severity, setSeverity] = useState("");
  const [status, setStatus] = useState("");
  const [verified, setVerified] = useState("");
  const [search, setSearch] = useState("");
  const deferredSearch = useDeferredValue(search);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    async function load() {
      const params = new URLSearchParams({
        page: String(page),
        size: "12"
      });
      if (category) params.set("category", category);
      if (severity) params.set("severity", severity);
      if (status) params.set("status", status);
      if (verified) params.set("verified", verified);

      try {
        const response = await api.listIncidents(params);
        if (!active) {
          return;
        }
        setIncidents(response.content);
        setTotalPages(Math.max(1, response.totalPages));
      } catch (loadError) {
        if (active) {
          setError(loadError instanceof Error ? loadError.message : "Failed to load incidents");
        }
      }
    }

    load();
    return () => {
      active = false;
    };
  }, [category, page, severity, status, verified]);

  const filteredIncidents = useMemo(() => {
    const term = deferredSearch.trim().toLowerCase();
    if (!term) {
      return incidents;
    }
    return incidents.filter((incident) =>
      `${incident.title} ${incident.description} ${incident.category}`.toLowerCase().includes(term)
    );
  }, [deferredSearch, incidents]);

  return (
    <div className="page">
      <section className="hero-card">
        <div className="eyebrow">Incident registry</div>
        <div className="row-between">
          <div>
            <h2 className="hero-title" style={{ maxWidth: "12ch" }}>
              Filter, inspect, and sort verified and pending road incidents.
            </h2>
            <p className="muted" style={{ maxWidth: "60ch" }}>
              This view consumes the public incident listing API and layers client-side search on top of backend
              filtering and pagination.
            </p>
          </div>
          <StatusPill tone="warning">/api/v1/incidents</StatusPill>
        </div>
      </section>

      <section className="panel">
        <div className="form-grid">
          <div className="field">
            <label htmlFor="search">Search title or description</label>
            <input id="search" value={search} onChange={(event) => setSearch(event.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="category">Category</label>
            <select id="category" value={category} onChange={(event) => setCategory(event.target.value)}>
              <option value="">All</option>
              <option value="CHECKPOINT">Checkpoint</option>
              <option value="CLOSURE">Closure</option>
              <option value="DELAY">Delay</option>
              <option value="ACCIDENT">Accident</option>
              <option value="WEATHER_HAZARD">Weather hazard</option>
              <option value="SECURITY">Security</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="severity">Severity</label>
            <select id="severity" value={severity} onChange={(event) => setSeverity(event.target.value)}>
              <option value="">All</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="status">Status</label>
            <select id="status" value={status} onChange={(event) => setStatus(event.target.value)}>
              <option value="">All</option>
              <option value="OPEN">Open</option>
              <option value="VERIFIED">Verified</option>
              <option value="CLOSED">Closed</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="verified">Verified flag</label>
            <select id="verified" value={verified} onChange={(event) => setVerified(event.target.value)}>
              <option value="">Any</option>
              <option value="true">Verified</option>
              <option value="false">Not verified</option>
            </select>
          </div>
        </div>
      </section>

      {error ? <div className="banner error-text">{error}</div> : null}

      <section className="panel">
        <div className="section-row">
          <h3 className="section-title">Incident table</h3>
          <div className="split-actions">
            <button className="button-ghost" type="button" onClick={() => setPage((current) => Math.max(0, current - 1))}>
              Previous
            </button>
            <button
              className="button-ghost"
              type="button"
              onClick={() => setPage((current) => Math.min(totalPages - 1, current + 1))}
            >
              Next
            </button>
          </div>
        </div>

        {filteredIncidents.length === 0 ? (
          <div style={{ marginTop: "1rem" }}>
            <EmptyState
              title="No incidents match the current filter set"
              description="Clear a few filters or add sample data to the backend."
            />
          </div>
        ) : (
          <div style={{ overflowX: "auto", marginTop: "1rem" }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Incident</th>
                  <th>Category</th>
                  <th>Severity</th>
                  <th>Status</th>
                  <th>Location</th>
                  <th>Reported</th>
                </tr>
              </thead>
              <tbody>
                {filteredIncidents.map((incident) => (
                  <tr key={incident.id}>
                    <td>
                      <strong>{incident.title}</strong>
                      <div className="muted">{incident.description}</div>
                    </td>
                    <td>{incident.category}</td>
                    <td>
                      <StatusPill tone={incident.severity === "CRITICAL" ? "alert" : "warning"}>
                        {incident.severity}
                      </StatusPill>
                    </td>
                    <td>
                      <StatusPill tone={incident.verified ? "warning" : "neutral"}>{incident.status}</StatusPill>
                    </td>
                    <td>{formatLocation(incident.latitude, incident.longitude)}</td>
                    <td>{formatDate(incident.reportedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
