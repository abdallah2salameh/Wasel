export type Role = "CITIZEN" | "MODERATOR" | "ADMIN";

export type UserProfile = {
  id: string;
  email: string;
  role: Role;
  createdAt: string;
};

export type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  user: UserProfile;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type Incident = {
  id: string;
  title: string;
  description: string;
  category: string;
  severity: string;
  status: string;
  sourceType: string;
  latitude: number;
  longitude: number;
  verified: boolean;
  reportedAt: string;
  verifiedAt: string | null;
  closedAt: string | null;
  checkpointId: string | null;
  createdBy: string | null;
};

export type Checkpoint = {
  id: string;
  name: string;
  governorate: string;
  latitude: number;
  longitude: number;
  currentStatus: string;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
};

export type Report = {
  id: string;
  latitude: number;
  longitude: number;
  category: string;
  description: string;
  status: string;
  confidenceScore: number;
  abuseFlagCount: number;
  createdAt: string;
  reviewedAt: string | null;
  duplicateOfReportId: string | null;
  submittedBy: string;
};

export type RouteEstimate = {
  estimatedDistanceKm: number;
  estimatedDurationMinutes: number;
  factors: string[];
  provider: string;
};

export type GeocodeResult = {
  displayName: string;
  latitude: number;
  longitude: number;
};

export type Subscription = {
  id: string;
  areaName: string;
  minLatitude: number;
  maxLatitude: number;
  minLongitude: number;
  maxLongitude: number;
  incidentCategory: string | null;
  active: boolean;
  createdAt: string;
};

export type AlertRecord = {
  id: string;
  incidentId: string;
  incidentTitle: string;
  deliveryStatus: string;
  createdAt: string;
};

export type IncidentSummaryRow = {
  category: string;
  severity: string;
  total: number;
};

export type IncidentAnalytics = {
  rows: IncidentSummaryRow[];
};

export type ApiError = {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
};
