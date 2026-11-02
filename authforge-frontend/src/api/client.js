import axios from "axios";

const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const useCookies = import.meta.env.VITE_USE_COOKIES === "true";

export const api = axios.create({
  baseURL,
  headers: { "Content-Type": "application/json" },
  withCredentials: useCookies
});

let accessToken = null;
let refreshToken = sessionStorage.getItem("authforge_refresh_token");

export function setAccessToken(token) {
  accessToken = token || null;
}
export function setRefreshToken(token) {
  refreshToken = token || null;
  if (refreshToken) sessionStorage.setItem("authforge_refresh_token", refreshToken);
  else sessionStorage.removeItem("authforge_refresh_token");
}
export function clearTokens() {
  accessToken = null;
  refreshToken = null;
  sessionStorage.removeItem("authforge_refresh_token");
}
export function getRefreshToken() {
  return refreshToken;
}

api.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
  return config;
});

let refreshPromise = null;
api.interceptors.response.use(
  response => response,
  async error => {
    const original = error.config;
    const isAuthEndpoint = original?.url?.includes("/api/v1/auth/");
    if (error.response?.status === 401 && !original?._retry && !isAuthEndpoint && refreshToken) {
      original._retry = true;
      try {
        refreshPromise ||= api.post("/api/v1/auth/refresh", { refreshToken })
          .then(({ data }) => {
            setAccessToken(data.accessToken);
            if (data.refreshToken) setRefreshToken(data.refreshToken);
            return data.accessToken;
          })
          .finally(() => { refreshPromise = null; });
        const token = await refreshPromise;
        original.headers = original.headers || {};
        original.headers.Authorization = `Bearer ${token}`;
        return api(original);
      } catch (refreshError) {
        clearTokens();
        window.dispatchEvent(new Event("authforge:session-expired"));
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  register: payload => api.post("/api/v1/auth/register", payload),
  login: payload => api.post("/api/v1/auth/login", payload),
  refresh: token => api.post("/api/v1/auth/refresh", { refreshToken: token }),
  logout: token => api.post("/api/v1/auth/logout", { refreshToken: token }),
  me: () => api.get("/api/v1/users/me"),
  googleLoginUrl: `${baseURL}/oauth2/authorization/google`
};