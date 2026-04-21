"use client";

import { api, ApiRequestError } from "@/lib/api";
import { AuthResponse, UserProfile } from "@/lib/types";
import {
  createContext,
  startTransition,
  useContext,
  useEffect,
  useMemo,
  useState
} from "react";

type SessionState = {
  accessToken: string;
  refreshToken: string;
  user: UserProfile;
};

type AuthContextValue = {
  session: SessionState | null;
  ready: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => void;
  withAuth: <T>(operation: (token: string) => Promise<T>) => Promise<T>;
};

const STORAGE_KEY = "wasel.frontend.session";
const AuthContext = createContext<AuthContextValue | null>(null);

function toSession(response: AuthResponse): SessionState {
  return {
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    user: response.user
  };
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<SessionState | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        setSession(JSON.parse(stored) as SessionState);
      } catch {
        window.localStorage.removeItem(STORAGE_KEY);
      }
    }
    setReady(true);
  }, []);

  const persist = (nextSession: SessionState | null) => {
    startTransition(() => {
      setSession(nextSession);
    });
    if (nextSession) {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(nextSession));
    } else {
      window.localStorage.removeItem(STORAGE_KEY);
    }
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      ready,
      async login(email, password) {
        const response = await api.login({ email, password });
        persist(toSession(response));
      },
      async register(email, password) {
        const response = await api.register({ email, password });
        persist(toSession(response));
      },
      logout() {
        persist(null);
      },
      async withAuth<T>(operation: (token: string) => Promise<T>) {
        if (!session) {
          throw new Error("You need to sign in first.");
        }

        try {
          return await operation(session.accessToken);
        } catch (error) {
          if (!(error instanceof ApiRequestError) || error.status !== 401) {
            throw error;
          }
        }

        const refreshed = await api.refresh(session.refreshToken);
        const nextSession = toSession(refreshed);
        persist(nextSession);
        return operation(nextSession.accessToken);
      }
    }),
    [ready, session]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
