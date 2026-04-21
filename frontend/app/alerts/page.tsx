"use client";

import { ChangeEvent, FormEvent, startTransition, useEffect, useState } from "react";
import { useAuth } from "@/components/auth-provider";
import { api, formatDate } from "@/lib/api";
import { AlertRecord, Subscription } from "@/lib/types";
import { EmptyState, StatusPill } from "@/components/ui";

type SubscriptionForm = {
  areaName: string;
  minLatitude: string;
  maxLatitude: string;
  minLongitude: string;
  maxLongitude: string;
  incidentCategory: string;
};

const initialForm: SubscriptionForm = {
  areaName: "Central corridor",
  minLatitude: "31.70",
  maxLatitude: "31.95",
  minLongitude: "35.10",
  maxLongitude: "35.30",
  incidentCategory: ""
};

type SubscriptionFormChangeEvent = ChangeEvent<HTMLInputElement | HTMLSelectElement>;

export default function AlertsPage() {
  const { session, withAuth } = useAuth();
  const [form, setForm] = useState<SubscriptionForm>(initialForm);
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [records, setRecords] = useState<AlertRecord[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  function updateField(field: keyof SubscriptionForm) {
    return (event: SubscriptionFormChangeEvent) => {
      const { value } = event.target;
      setForm((current: SubscriptionForm) => ({ ...current, [field]: value }));
    };
  }

  useEffect(() => {
    let active = true;
    async function load() {
      if (!session) {
        setSubscriptions([]);
        setRecords([]);
        return;
      }
      try {
        const [subscriptionResponse, recordResponse] = await Promise.all([
          withAuth((token: string) => api.listSubscriptions(token)),
          withAuth((token: string) => api.listAlertRecords(token))
        ]);
        if (!active) {
          return;
        }
        setSubscriptions(subscriptionResponse);
        setRecords(recordResponse);
      } catch (loadError) {
        if (active) {
          setError(loadError instanceof Error ? loadError.message : "Failed to load alerts");
        }
      }
    }

    load();
    return () => {
      active = false;
    };
  }, [session, withAuth]);

  async function createSubscription(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setMessage(null);

    try {
      const created = await withAuth((token: string) =>
        api.createSubscription(
          {
            areaName: form.areaName,
            minLatitude: Number(form.minLatitude),
            maxLatitude: Number(form.maxLatitude),
            minLongitude: Number(form.minLongitude),
            maxLongitude: Number(form.maxLongitude),
            incidentCategory: form.incidentCategory || null
          },
          token
        )
      );

      startTransition(() => {
        setSubscriptions((current: Subscription[]) => [created, ...current]);
        setMessage("Subscription created.");
      });
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Failed to create subscription");
    }
  }

  async function removeSubscription(id: string) {
    setError(null);
    try {
      await withAuth((token: string) => api.deleteSubscription(id, token));
      setSubscriptions((current: Subscription[]) =>
        current.filter((subscription: Subscription) => subscription.id !== id)
      );
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Failed to delete subscription");
    }
  }

  if (!session) {
    return (
      <div className="page">
        <section className="hero-card">
          <div className="eyebrow">Regional alerts</div>
          <h2 className="hero-title" style={{ maxWidth: "12ch" }}>
            Subscribe to incident signals by geography and category.
          </h2>
          <p className="muted" style={{ maxWidth: "58ch" }}>
            Alert management is a personal authenticated workflow. Sign in first, then come back here to create
            subscription bounds and view alert records generated from verified incidents.
          </p>
        </section>
        <EmptyState
          title="Authentication required"
          description="The backend exposes subscriptions and alert records only to authenticated users."
        />
      </div>
    );
  }

  return (
    <div className="page">
      <section className="hero-card">
        <div className="eyebrow">Regional alerts</div>
        <h2 className="hero-title" style={{ maxWidth: "12ch" }}>
          Personalize verified incident notifications by region and category.
        </h2>
        <p className="muted" style={{ maxWidth: "60ch" }}>
          Subscriptions define a rectangular geographic area and optional incident type. When a moderator verifies an
          incident inside that region, the backend generates alert records for delivery.
        </p>
      </section>

      {error ? <div className="banner error-text">{error}</div> : null}
      {message ? <div className="banner success-text">{message}</div> : null}

      <section className="content-grid">
        <article className="panel">
          <h3 className="section-title">Create subscription</h3>
          <form className="grid-list" style={{ marginTop: "1rem" }} onSubmit={createSubscription}>
            <div className="form-grid">
              <div className="field">
                <label htmlFor="area-name">Area name</label>
                <input
                  id="area-name"
                  value={form.areaName}
                  onChange={updateField("areaName")}
                />
              </div>
              <div className="field">
                <label htmlFor="incident-category">Incident category</label>
                <select
                  id="incident-category"
                  value={form.incidentCategory}
                  onChange={updateField("incidentCategory")}
                >
                  <option value="">All categories</option>
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
                <label htmlFor="min-lat">Min latitude</label>
                <input
                  id="min-lat"
                  value={form.minLatitude}
                  onChange={updateField("minLatitude")}
                />
              </div>
              <div className="field">
                <label htmlFor="max-lat">Max latitude</label>
                <input
                  id="max-lat"
                  value={form.maxLatitude}
                  onChange={updateField("maxLatitude")}
                />
              </div>
              <div className="field">
                <label htmlFor="min-lon">Min longitude</label>
                <input
                  id="min-lon"
                  value={form.minLongitude}
                  onChange={updateField("minLongitude")}
                />
              </div>
              <div className="field">
                <label htmlFor="max-lon">Max longitude</label>
                <input
                  id="max-lon"
                  value={form.maxLongitude}
                  onChange={updateField("maxLongitude")}
                />
              </div>
            </div>
            <div className="split-actions">
              <button className="button" type="submit">
                Save subscription
              </button>
              <button className="button-ghost" type="button" onClick={() => setForm(initialForm)}>
                Reset
              </button>
            </div>
          </form>
        </article>

        <article className="panel">
          <h3 className="section-title">Alert records</h3>
          <div className="grid-list" style={{ marginTop: "1rem" }}>
            {records.length === 0 ? (
              <EmptyState
                title="No alert records yet"
                description="Records will appear after a verified incident matches one of your saved subscriptions."
              />
            ) : (
              records.map((record: AlertRecord) => (
                <div key={record.id} className="feed-item">
                  <div className="row-between">
                    <strong>{record.incidentTitle}</strong>
                    <StatusPill tone={record.deliveryStatus === "DISPATCHED" ? "neutral" : "warning"}>
                      {record.deliveryStatus}
                    </StatusPill>
                  </div>
                  <div className="muted">{formatDate(record.createdAt)}</div>
                </div>
              ))
            )}
          </div>
        </article>
      </section>

      <section className="panel">
        <h3 className="section-title">Saved subscriptions</h3>
        <div className="grid-list" style={{ marginTop: "1rem" }}>
          {subscriptions.length === 0 ? (
            <EmptyState
              title="No subscriptions saved"
              description="Create your first subscription above to activate regional notifications."
            />
          ) : (
            subscriptions.map((subscription: Subscription) => (
              <div key={subscription.id} className="list-item">
                <div className="row-between">
                  <div>
                    <strong>{subscription.areaName}</strong>
                    <div className="muted">
                      {subscription.minLatitude} to {subscription.maxLatitude} / {subscription.minLongitude} to{" "}
                      {subscription.maxLongitude}
                    </div>
                  </div>
                  <StatusPill>{subscription.incidentCategory || "All categories"}</StatusPill>
                </div>
                <div className="split-actions" style={{ marginTop: "0.8rem" }}>
                  <button className="button-ghost" type="button" onClick={() => removeSubscription(subscription.id)}>
                    Remove
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </section>
    </div>
  );
}
