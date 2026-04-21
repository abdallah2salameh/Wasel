"use client";

import { FormEvent, startTransition, useState } from "react";
import { api } from "@/lib/api";
import { GeocodeResult, RouteEstimate } from "@/lib/types";
import { EmptyState, StatusPill } from "@/components/ui";

type FormState = {
  originLatitude: string;
  originLongitude: string;
  destinationLatitude: string;
  destinationLongitude: string;
  avoidCheckpoints: boolean;
};

const initialForm: FormState = {
  originLatitude: "31.7683",
  originLongitude: "35.2137",
  destinationLatitude: "31.9038",
  destinationLongitude: "35.2034",
  avoidCheckpoints: true
};

export default function RoutesPage() {
  const [form, setForm] = useState<FormState>(initialForm);
  const [route, setRoute] = useState<RouteEstimate | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [geocodeQuery, setGeocodeQuery] = useState("Ramallah");
  const [geocodeResults, setGeocodeResults] = useState<GeocodeResult[]>([]);

  async function submitEstimate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setError(null);

    try {
      const result = await api.estimateRoute({
        originLatitude: Number(form.originLatitude),
        originLongitude: Number(form.originLongitude),
        destinationLatitude: Number(form.destinationLatitude),
        destinationLongitude: Number(form.destinationLongitude),
        avoidCheckpoints: form.avoidCheckpoints,
        avoidedAreas: []
      });
      startTransition(() => {
        setRoute(result);
      });
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Failed to estimate route");
    } finally {
      setPending(false);
    }
  }

  async function searchGeocode() {
    setError(null);
    try {
      const results = await api.geocode(geocodeQuery);
      startTransition(() => {
        setGeocodeResults(results);
      });
    } catch (searchError) {
      setError(searchError instanceof Error ? searchError.message : "Failed to geocode location");
    }
  }

  return (
    <div className="page">
      <section className="hero-card">
        <div className="eyebrow">Route planner</div>
        <h2 className="hero-title" style={{ maxWidth: "13ch" }}>
          Estimate routes with checkpoint friction, incident load, and weather-aware heuristics.
        </h2>
        <p className="muted" style={{ maxWidth: "62ch" }}>
          This page consumes the backend&apos;s route estimation endpoint and geocoder. It is designed for planners who
          want explainable route factors rather than opaque black-box navigation output.
        </p>
      </section>

      <section className="content-grid">
        <article className="panel">
          <div className="section-row">
            <h3 className="section-title">Estimate route</h3>
            <StatusPill tone="warning">/api/v1/routes/estimate</StatusPill>
          </div>
          <form className="grid-list" style={{ marginTop: "1rem" }} onSubmit={submitEstimate}>
            <div className="form-grid">
              <div className="field">
                <label htmlFor="origin-lat">Origin latitude</label>
                <input
                  id="origin-lat"
                  value={form.originLatitude}
                  onChange={(event) => setForm((current) => ({ ...current, originLatitude: event.target.value }))}
                />
              </div>
              <div className="field">
                <label htmlFor="origin-lon">Origin longitude</label>
                <input
                  id="origin-lon"
                  value={form.originLongitude}
                  onChange={(event) => setForm((current) => ({ ...current, originLongitude: event.target.value }))}
                />
              </div>
              <div className="field">
                <label htmlFor="destination-lat">Destination latitude</label>
                <input
                  id="destination-lat"
                  value={form.destinationLatitude}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, destinationLatitude: event.target.value }))
                  }
                />
              </div>
              <div className="field">
                <label htmlFor="destination-lon">Destination longitude</label>
                <input
                  id="destination-lon"
                  value={form.destinationLongitude}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, destinationLongitude: event.target.value }))
                  }
                />
              </div>
            </div>
            <label className="stack-row">
              <input
                type="checkbox"
                checked={form.avoidCheckpoints}
                onChange={(event) => setForm((current) => ({ ...current, avoidCheckpoints: event.target.checked }))}
              />
              <span>Avoid checkpoint-heavy paths where possible</span>
            </label>
            <div className="split-actions">
              <button type="submit" className="button" disabled={pending}>
                {pending ? "Estimating..." : "Run estimate"}
              </button>
              <button type="button" className="button-ghost" onClick={() => setForm(initialForm)}>
                Reset form
              </button>
            </div>
          </form>
          {error ? <div className="banner error-text">{error}</div> : null}
        </article>

        <article className="panel">
          <h3 className="section-title">Result</h3>
          {!route ? (
            <div style={{ marginTop: "1rem" }}>
              <EmptyState
                title="No route estimate yet"
                description="Submit an origin and destination to see distance, duration, and explanatory route factors."
              />
            </div>
          ) : (
            <div className="grid-list" style={{ marginTop: "1rem" }}>
              <div className="feed-item">
                <strong>{route.estimatedDistanceKm} km</strong>
                <div className="muted">Estimated distance</div>
              </div>
              <div className="feed-item">
                <strong>{route.estimatedDurationMinutes} minutes</strong>
                <div className="muted">Estimated duration</div>
              </div>
              <div className="feed-item">
                <strong>{route.provider}</strong>
                <div className="muted">Provider or fallback mode</div>
              </div>
              <div className="feed-item">
                <strong>Factors</strong>
                <ul style={{ marginBottom: 0 }}>
                  {route.factors.map((factor) => (
                    <li key={factor}>{factor}</li>
                  ))}
                </ul>
              </div>
            </div>
          )}
        </article>
      </section>

      <section className="panel">
        <div className="section-row">
          <h3 className="section-title">Geocode helper</h3>
          <StatusPill>/api/v1/routes/geocode</StatusPill>
        </div>
        <div className="form-grid" style={{ marginTop: "1rem" }}>
          <div className="field">
            <label htmlFor="geocode-query">Place search</label>
            <input
              id="geocode-query"
              value={geocodeQuery}
              onChange={(event) => setGeocodeQuery(event.target.value)}
              placeholder="Ramallah, Hebron, Jerusalem..."
            />
          </div>
        </div>
        <div className="split-actions" style={{ marginTop: "1rem" }}>
          <button type="button" className="button-ghost" onClick={searchGeocode}>
            Search geocoder
          </button>
        </div>

        <div className="grid-list" style={{ marginTop: "1rem" }}>
          {geocodeResults.length === 0 ? (
            <EmptyState
              title="No geocode search yet"
              description="Run a search to get candidate coordinates you can paste into the route form."
            />
          ) : (
            geocodeResults.map((result) => (
              <div key={`${result.displayName}-${result.latitude}`} className="list-item">
                <strong>{result.displayName}</strong>
                <div className="muted">
                  {result.latitude}, {result.longitude}
                </div>
              </div>
            ))
          )}
        </div>
      </section>
    </div>
  );
}
