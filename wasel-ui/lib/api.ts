import {
  AlertRecord,
  ApiError,
  AuthResponse,
  Checkpoint,
  GeocodeResult,
  Incident,
  IncidentAnalytics,
  PageResponse,
  Report,
  RouteEstimate,
  Subscription
} from "@/lib/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

type RequestInitWithJson = RequestInit & {
  json?: unknown;
  token?: string | null;
};

export class ApiRequestError extends Error {
  constructor(
    message: string,
    public readonly status: number
  ) {
    super(message);
    this.name = "ApiRequestError";
  }
}

async function request<T>(path: string, init: RequestInitWithJson = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (init.json !== undefined) {
    headers.set("Content-Type", "application/json");
  }
  if (init.token) {
    headers.set("Authorization", `Bearer ${init.token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
    body: init.json !== undefined ? JSON.stringify(init.json) : init.body,
    cache: init.method === "GET" || !init.method ? "no-store" : "no-store"
  });

  if (!response.ok) {
    let payload: ApiError | null = null;
    try {
      payload = (await response.json()) as ApiError;
    } catch {
      payload = null;
    }
    throw new ApiRequestError(payload?.message ?? `Request failed with status ${response.status}`, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const api = {
  register: (payload: { email: string; password: string }) =>
    request<AuthResponse>("/api/v1/auth/register", { method: "POST", json: payload }),
  login: (payload: { email: string; password: string }) =>
    request<AuthResponse>("/api/v1/auth/login", { method: "POST", json: payload }),
  refresh: (refreshToken: string) =>
    request<AuthResponse>("/api/v1/auth/refresh", { method: "POST", json: { refreshToken } }),
  me: (token: string) =>
    request("/api/v1/auth/me", { token }),
  listIncidents: (params: URLSearchParams) =>
    request<PageResponse<Incident>>(`/api/v1/incidents?${params.toString()}`),
  incidentAnalytics: () =>
    request<IncidentAnalytics>("/api/v1/incidents/analytics/summary"),
  listCheckpoints: (params: URLSearchParams) =>
    request<PageResponse<Checkpoint>>(`/api/v1/checkpoints?${params.toString()}`),
  listReports: (params: URLSearchParams, token?: string | null) =>
    request<PageResponse<Report>>(`/api/v1/reports?${params.toString()}`, { token }),
  submitReport: (payload: unknown) =>
    request<Report>("/api/v1/reports", {
      method: "POST",
      json: payload,
      headers: {
        "X-Client-Fingerprint": "wasel-frontend"
      }
    }),
  voteOnReport: (reportId: string, voteType: "CONFIRM" | "DENY", token: string) =>
    request<Report>(`/api/v1/reports/${reportId}/vote`, {
      method: "POST",
      json: { voteType },
      token
    }),
  moderateReport: (reportId: string, action: "APPROVE" | "REJECT" | "DUPLICATE", reason: string, token: string) =>
    request<Report>(`/api/v1/reports/${reportId}/moderate`, {
      method: "POST",
      json: { action, reason },
      token
    }),
  estimateRoute: (payload: unknown) =>
    request<RouteEstimate>("/api/v1/routes/estimate", { method: "POST", json: payload }),
  geocode: (query: string) =>
    request<GeocodeResult[]>(`/api/v1/routes/geocode?q=${encodeURIComponent(query)}`),
  listSubscriptions: (token: string) =>
    request<Subscription[]>("/api/v1/alerts/subscriptions", { token }),
  createSubscription: (payload: unknown, token: string) =>
    request<Subscription>("/api/v1/alerts/subscriptions", { method: "POST", json: payload, token }),
  listAlertRecords: (token: string) =>
    request<AlertRecord[]>("/api/v1/alerts/records", { token }),
  deleteSubscription: (id: string, token: string) =>
    request<void>(`/api/v1/alerts/subscriptions/${id}`, { method: "DELETE", token })
};

export function formatDate(value: string | null) {
  if (!value) {
    return "Not available";
  }
  return new Intl.DateTimeFormat("en-GB", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

export function formatLocation(latitude: number, longitude: number) {
  return `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`;
}
