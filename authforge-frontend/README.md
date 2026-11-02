# AuthForge Frontend

React 19 + Vite + Tailwind CSS frontend for the AuthForge Spring Boot API.

## Included
- Login and registration
- Google OAuth initiation
- Protected user dashboard
- Admin-only audit log viewer with pagination and filters
- Axios API client with bearer-token injection and one-time refresh retry
- Responsive dark identity-console UI

## Requirements
- Node.js 20.19+ or 22.12+
- AuthForge backend running at `http://localhost:8080` (or configure another URL)

## Run in IntelliJ / terminal

1. Extract this ZIP.
2. Open the extracted `authforge-frontend` folder in IntelliJ IDEA (or VS Code).
3. Open the terminal in that folder.
4. Copy `.env.example` to `.env`:
   ```powershell
   Copy-Item .env.example .env
   ```
5. Install dependencies:
   ```powershell
   npm install
   ```
6. Start the frontend:
   ```powershell
   npm run dev
   ```
7. Open `http://localhost:5173`.

For a production build:
```powershell
npm run build
npm run preview
```

## Backend endpoints expected

| Method | Endpoint | Expected purpose |
|---|---|---|
| POST | `/api/v1/auth/register` | Register `{ fullName, email, password }` |
| POST | `/api/v1/auth/login` | Login `{ email, password }`; returns `accessToken`, `refreshToken`, `tokenType`, `expiresIn` |
| POST | `/api/v1/auth/refresh` | Rotate `{ refreshToken }`; returns new token pair |
| POST | `/api/v1/auth/logout` | Revoke `{ refreshToken }` |
| GET | `/api/v1/users/me` | Return current user profile |
| GET | `/api/v1/admin/audit-logs` | Spring Page of audit log records; optional `userId`, `eventType`, `page`, `size` |

The profile page expects fields such as `id`, `fullName`, `email`, and optionally `roles`. The JWT is expected to contain `sub`, `userId`, and `roles`.

## Configure API URL

`.env`:
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_USE_COOKIES=false
```
Restart `npm run dev` after changing environment variables.

## CORS requirement

The backend must allow the Vite origin `http://localhost:5173` for the API routes and allow the `Authorization` and `Content-Type` headers. If you later move refresh tokens to HttpOnly cookies, enable credentials on both sides and configure an explicit origin (not `*`).

## Token handling note

The backend currently returns the refresh token in JSON. This starter keeps the access token in memory and the refresh token in `sessionStorage` so a tab refresh can restore the session. `sessionStorage` is still readable by JavaScript and is not the preferred production design. For production browser authentication, change the backend to issue the refresh token in a `Secure`, `HttpOnly`, appropriately `SameSite` cookie and implement a deliberate CSRF strategy. Then set `VITE_USE_COOKIES=true` and update the backend refresh/logout endpoints to read the cookie instead of JSON.

## Google OAuth integration note

The Google button starts the backend OAuth flow at `/oauth2/authorization/google`. The current backend success handler described in the project returns a JSON token response directly from the OAuth callback. A top-level browser navigation will display that JSON rather than automatically navigate into this React app. To complete the browser UX, update the backend success handler to establish a secure cookie-based session or perform a one-time authorization-code handoff and redirect to `http://localhost:5173/oauth/callback`. Do not put access or refresh JWTs in URL query parameters.

## Important backend checks

- Configure Spring Security CORS for `http://localhost:5173`.
- Ensure `/api/v1/users/me` exists and returns the expected profile fields.
- Ensure the admin account has `ROLE_ADMIN`; the audit page will correctly be forbidden for regular users.
- Confirm the backend handles invalid/missing JWTs with an `AuthenticationEntryPoint` and forbidden roles with an `AccessDeniedHandler`.
- Spring Security OAuth login may need its temporary authorization-request session even though API bearer-token requests are stateless.

## Troubleshooting

- **Network error:** confirm backend is running and `VITE_API_BASE_URL` is correct.
- **CORS error:** add `http://localhost:5173` to backend allowed origins.
- **401 after login:** inspect the login response and confirm it contains `accessToken` and `refreshToken`.
- **Dashboard profile error:** confirm `GET /api/v1/users/me` exists. The dashboard still renders token-derived identity if the endpoint is not implemented.
- **Admin audit page denied:** log in with a user whose JWT includes `ROLE_ADMIN`; log in again after changing database roles to get a fresh token.
