import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { authApi, clearTokens, getRefreshToken, setAccessToken, setRefreshToken } from "../api/client";

const AuthContext = createContext(null);

function decodeJwtPayload(token) {
  try {
    const part = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(part));
  } catch {
    return {};
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const applyLogin = (data) => {
    setAccessToken(data.accessToken);
    if (data.refreshToken) setRefreshToken(data.refreshToken);
    const claims = decodeJwtPayload(data.accessToken);
    setUser({
      id: claims.userId,
      email: claims.sub,
      roles: Array.isArray(claims.roles) ? claims.roles : String(claims.roles || "").split(/[ ,]+/).filter(Boolean)
    });
  };

  useEffect(() => {
    let active = true;
    const restore = async () => {
      const token = getRefreshToken();
      if (!token) {
        if (active) setLoading(false);
        return;
      }
      try {
        const { data } = await authApi.refresh(token);
        if (active) applyLogin(data);
      } catch {
        clearTokens();
        if (active) setUser(null);
      } finally {
        if (active) setLoading(false);
      }
    };
    restore();
    const expired = () => setUser(null);
    window.addEventListener("authforge:session-expired", expired);
    return () => {
      active = false;
      window.removeEventListener("authforge:session-expired", expired);
    };
  }, []);

  const login = async (email, password) => {
    const { data } = await authApi.login({ email, password });
    applyLogin(data);
  };

  const register = async (fullName, email, password) => {
    await authApi.register({ fullName, email, password });
    await login(email, password);
  };

  const logout = async () => {
    const token = getRefreshToken();
    try {
      if (token) await authApi.logout(token);
    } finally {
      clearTokens();
      setUser(null);
    }
  };

  const value = useMemo(() => ({
    user, loading, login, register, logout,
    isAdmin: Boolean(user?.roles?.includes("ROLE_ADMIN"))
  }), [user, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}