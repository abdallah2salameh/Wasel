"use client";

import { FormEvent, startTransition, useEffect, useState } from "react";
import { useAuth } from "@/components/auth-provider";
import { EmptyState, StatusPill } from "@/components/ui";
import { api, formatDate, formatLocation } from "@/lib/api";
import { Report } from "@/lib/types";

type ReportForm = {
  latitude: string;
  longitude: string;
  category: string;
  description: string;
};

const initialForm: ReportForm = {
  latitude: "31.7683",
  longitude: "35.2137",
  category: "DELAY",
  description: "Traffic backup observed near the main road junction."
};

export default function ReportsPage() {
  const { session, withAuth } = useAuth();
  const [form, setForm] = useState<ReportForm>(initialForm);
  const [submissionMessage, setSubmissionMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reports, setReports] = useState<Report[]>([]);
  const [moderationReason, setModerationReason] = useState("Confirmed by moderator review.");

  useEffect(() => {
    let active = true;
    async function loadModerationQueue() {
      if (!session || (session.user.role !== "ADMIN" && session.user.role !== "MODERATOR")) {
        setReports([]);
        return;
      }

      try {
        const response = await withAuth((token) =>
          api.listReports(new URLSearchParams({ page: "0", size: "10", status: "PENDING" }), token)
        );
        if (active) {
          setReports(response.content);
        }
      } catch (loadError) {
        if (active) {
          setError(loadError instanceof Error ? loadError.message : "Failed to load moderation queue");
        }
      }
    }

    loadModerationQueue();
    return () => {
      active = false;
    };
  }, [session, withAuth]);

  async function submitReport(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSubmissionMessage(null);

    try {
      const response = await api.submitReport({
        latitude: Number(form.latitude),
        longitude: Number(form.longitude),
        category: form.category,
        description: form.description
      });
      startTransition(() => {
        setSubmissionMessage(`Report submitted with status ${response.status}.`);
      });
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Failed to submit report");
    }
  }

  async function moderate(reportId: string, action: "APPROVE" | "REJECT" | "DUPLICATE") {
    setError(null);
    try {
      await withAuth((token) => api.moderateReport(reportId, action, moderationReason, token));
      setReports((current) => current.filter((report) => report.id !== reportId));
    } catch (moderationError) {
      setError(moderationError instanceof Error ? moderationError.message : "Moderation action failed");
    }
  }

  return (
    <div className="page">
      <section className="hero-card">
        <div className="eyebrow">Crowdsourced reporting</div>
        <h2 className="hero-title" style={{ maxWidth: "13ch" }}>
          Turn field observations into structured reports, then push them through moderation.
        </h2>
        <p className="muted" style={{ maxWidth: "62ch" }}>
          The backend already performs validation, duplicate detection, and audit logging. This interface exposes a
          public reporting form plus a protected moderation queue for higher-privilege users.
        </p>
      </section>

      {error ? <div className="banner error-text">{error}</div> : null}
      {submissionMessage ? <div className="banner success-text">{submissionMessage}</div> : null}

      <section className="content-grid">
        <article className="panel">
          <h3 className="section-title">Submit a report</h3>
          <form className="grid-list" style={{ marginTop: "1rem" }} onSubmit={submitReport}>
            <div className="form-grid">
              <div className="field">
                <label htmlFor="report-lat">Latitude</label>
                <input
                  id="report-lat"
                  value={form.latitude}
                  onChange={(event) => setForm((current) => ({ ...current, latitude: event.target.value }))}
                />
              </div>
              <div className="field">
                <label htmlFor="report-lon">Longitude</label>
                <input
                  id="report-lon"
                  value={form.longitude}
                  onChange={(event) => setForm((current) => ({ ...current, longitude: event.target.value }))}
                />
              </div>
              <div className="field">
                <label htmlFor="report-category">Category</label>
                <select
                  id="report-category"
                  value={form.category}
                  onChange={(event) => setForm((current) => ({ ...current, category: event.target.value }))}
                >
                  <option value="CHECKPOINT">Checkpoint</option>
                  <option value="CLOSURE">Closure</option>
                  <option value="DELAY">Delay</option>
                  <option value="ACCIDENT">Accident</option>
                  <option value="WEATHER_HAZARD">Weather hazard</option>
                  <option value="SECURITY">Security</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
            </div>
            <div className="field">
              <label htmlFor="report-description">Description</label>
              <textarea
                id="report-description"
                value={form.description}
                onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
              />
            </div>
            <div className="split-actions">
              <button className="button" type="submit">
                Submit report
              </button>
              <button className="button-ghost" type="button" onClick={() => setForm(initialForm)}>
                Reset
              </button>
            </div>
          </form>
        </article>

        <article className="panel">
          <h3 className="section-title">How moderation works</h3>
          <div className="grid-list" style={{ marginTop: "1rem" }}>
            <div className="feed-item">
              <strong>Duplicate detection</strong>
              <div className="muted">Nearby recent reports of the same category may be auto-marked as duplicates.</div>
            </div>
            <div className="feed-item">
              <strong>Credibility score</strong>
              <div className="muted">Citizen confirmations and denials adjust the confidence score in the backend.</div>
            </div>
            <div className="feed-item">
              <strong>Auditable actions</strong>
              <div className="muted">Every moderation decision is written to `moderation_audit`.</div>
            </div>
          </div>
        </article>
      </section>

      <section className="panel">
        <div className="section-row">
          <h3 className="section-title">Moderator queue</h3>
          <StatusPill tone="warning">Protected</StatusPill>
        </div>
        <div className="field" style={{ marginTop: "1rem", maxWidth: "420px" }}>
          <label htmlFor="moderation-reason">Moderation reason</label>
          <input
            id="moderation-reason"
            value={moderationReason}
            onChange={(event) => setModerationReason(event.target.value)}
          />
        </div>

        {!session || (session.user.role !== "ADMIN" && session.user.role !== "MODERATOR") ? (
          <div style={{ marginTop: "1rem" }}>
            <EmptyState
              title="Moderator session required"
              description="Sign in as the seeded admin or another moderator to load pending reports."
            />
          </div>
        ) : reports.length === 0 ? (
          <div style={{ marginTop: "1rem" }}>
            <EmptyState
              title="No pending reports"
              description="The moderation queue is empty or the backend does not currently have pending submissions."
            />
          </div>
        ) : (
          <div className="grid-list" style={{ marginTop: "1rem" }}>
            {reports.map((report) => (
              <div key={report.id} className="list-item">
                <div className="row-between">
                  <div>
                    <strong>{report.category}</strong>
                    <div className="muted">
                      {formatLocation(report.latitude, report.longitude)} | {formatDate(report.createdAt)}
                    </div>
                  </div>
                  <StatusPill tone="warning">{report.status}</StatusPill>
                </div>
                <p className="muted">{report.description}</p>
                <div className="muted">Confidence: {report.confidenceScore.toFixed(2)}</div>
                <div className="split-actions" style={{ marginTop: "0.75rem" }}>
                  <button className="button" type="button" onClick={() => moderate(report.id, "APPROVE")}>
                    Approve
                  </button>
                  <button className="button secondary" type="button" onClick={() => moderate(report.id, "REJECT")}>
                    Reject
                  </button>
                  <button className="button-ghost" type="button" onClick={() => moderate(report.id, "DUPLICATE")}>
                    Mark duplicate
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
