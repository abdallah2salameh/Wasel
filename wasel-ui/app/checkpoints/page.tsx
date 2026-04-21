"use client";

import { useEffect, useState } from "react";
import { api, formatDate, formatLocation } from "@/lib/api";
import { Checkpoint } from "@/lib/types";
import { EmptyState, StatusPill } from "@/components/ui";

export default function CheckpointsPage() {
  const [checkpoints, setCheckpoints] = useState<Checkpoint[]>([]);
  const [governorate, setGovernorate] = useState("");
  const [status, setStatus] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    async function load() {
      const params = new URLSearchParams({ page: "0", size: "20" });
      if (governorate) params.set("governorate", governorate);
      if (status) params.set("status", status);

      try {
        const response = await api.listCheckpoints(params);
        if (active) {
          setCheckpoints(response.content);
        }
      } catch (loadError) {
        if (active) {
          setError(loadError instanceof Error ? loadError.message : "Failed to load checkpoints");
        }
      }
    }

    load();
    return () => {
      active = false;
    };
  }, [governorate, status]);


  return (
    <div className="page">
      <section className="hero-card">
        <div className="eyebrow">Checkpoint registry</div>
        <h2 className="hero-title" style={{ maxWidth: "12ch" }}>
          Watch status shifts across key crossing points and road-control nodes.
        </h2>
        <p className="muted" style={{ maxWidth: "60ch" }}>
          The backend preserves status history; this frontend surfaces the current operational picture so dispatchers
          can isolate delayed or closed movement corridors quickly.
        </p>
      </section>

      <section className="panel">
        <div className="form-grid">
          <div className="field">
            <label htmlFor="governorate">Governorate</label>
            <input
              id="governorate"
              value={governorate}
              onChange={(event) => setGovernorate(event.target.value)}
              placeholder="Jerusalem, Ramallah, Hebron..."
            />
          </div>
          <div className="field">
            <label htmlFor="checkpoint-status">Status</label>
            <select id="checkpoint-status" value={status} onChange={(event) => setStatus(event.target.value)}>
              <option value="">All</option>
              <option value="OPEN">Open</option>
              <option value="DELAYED">Delayed</option>
              <option value="CLOSED">Closed</option>
              <option value="UNKNOWN">Unknown</option>
            </select>
          </div>
        </div>
      </section>

      {error ? <div className="banner error-text">{error}</div> : null}

      <section className="double-grid">
        {checkpoints.length === 0 ? (
          <EmptyState
            title="No checkpoints returned"
            description="Once the backend has checkpoint records, they will appear here with their latest status."
          />
        ) : (
          checkpoints.map((checkpoint) => (
            <article className="panel" key={checkpoint.id}>
              <div className="row-between">
                <div>
                  <h3 className="panel-title">{checkpoint.name}</h3>
                  <div className="muted">{checkpoint.governorate}</div>
                </div>
                <StatusPill tone={checkpoint.currentStatus === "OPEN" ? "neutral" : "warning"}>
                  {checkpoint.currentStatus}
                </StatusPill>
              </div>
              <div className="grid-list" style={{ marginTop: "1rem" }}>
                <div className="feed-item">
                  <strong>Location</strong>
                  <div className="muted">{formatLocation(checkpoint.latitude, checkpoint.longitude)}</div>
                </div>
                <div className="feed-item">
                  <strong>Operator notes</strong>
                  <div className="muted">{checkpoint.notes || "No notes attached to the current state."}</div>
                </div>
                <div className="feed-item">
                  <strong>Last updated</strong>
                  <div className="muted">{formatDate(checkpoint.updatedAt)}</div>
                </div>
              </div>
            </article>
          ))
        )}
      </section>
    </div>
  );
}
