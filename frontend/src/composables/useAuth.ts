const KEYCLOAK_BASE = (import.meta.env.PUBLIC_KEYCLOAK_URL as string | undefined) ?? 'http://localhost:8081';
const REALM = 'gandzi';
const CLIENT_ID = 'gandzi-frontend';
const REDIRECT_URI = (import.meta.env.PUBLIC_REDIRECT_URI as string | undefined) ?? 'http://localhost:4321/';
const TOKEN_URL = `${KEYCLOAK_BASE}/realms/${REALM}/protocol/openid-connect/token`;
const LOGOUT_URL = `${KEYCLOAK_BASE}/realms/${REALM}/protocol/openid-connect/logout`;

const STORAGE_KEY_TOKEN = 'gandzi_access_token';
const STORAGE_KEY_REFRESH = 'gandzi_refresh_token';
const STORAGE_KEY_EXPIRY = 'gandzi_token_expiry';

type TokenResponse = {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  token_type: string;
};

export function getAccessToken(): string | null {
  const token = window.localStorage.getItem(STORAGE_KEY_TOKEN);
  const expiry = Number(window.localStorage.getItem(STORAGE_KEY_EXPIRY) || '0');
  if (token && Date.now() < expiry) return token;
  return null;
}

function storeTokens(data: TokenResponse): void {
  window.localStorage.setItem(STORAGE_KEY_TOKEN, data.access_token);
  window.localStorage.setItem(STORAGE_KEY_REFRESH, data.refresh_token);
  // Expire 30 seconds early to avoid edge cases
  window.localStorage.setItem(STORAGE_KEY_EXPIRY, String(Date.now() + (data.expires_in - 30) * 1000));
}

export function clearTokens(): void {
  window.localStorage.removeItem(STORAGE_KEY_TOKEN);
  window.localStorage.removeItem(STORAGE_KEY_REFRESH);
  window.localStorage.removeItem(STORAGE_KEY_EXPIRY);
}

export async function exchangeCode(code: string): Promise<boolean> {
  try {
    const response = await fetch(TOKEN_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'authorization_code',
        client_id: CLIENT_ID,
        code,
        redirect_uri: REDIRECT_URI,
      }),
    });

    if (!response.ok) {
      console.error('Token exchange failed:', response.status, await response.text());
      return false;
    }

    const data = await response.json() as TokenResponse;
    storeTokens(data);
    return true;
  } catch (err) {
    console.error('Token exchange error:', err);
    return false;
  }
}

export async function refreshAccessToken(): Promise<boolean> {
  const refreshToken = window.localStorage.getItem(STORAGE_KEY_REFRESH);
  if (!refreshToken) return false;

  try {
    const response = await fetch(TOKEN_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'refresh_token',
        client_id: CLIENT_ID,
        refresh_token: refreshToken,
      }),
    });

    if (!response.ok) {
      clearTokens();
      return false;
    }

    const data = await response.json() as TokenResponse;
    storeTokens(data);
    return true;
  } catch {
    clearTokens();
    return false;
  }
}

export function isAuthenticated(): boolean {
  return getAccessToken() !== null;
}

export async function ensureValidToken(): Promise<string | null> {
  let token = getAccessToken();
  if (token) return token;

  // Try refreshing
  const refreshed = await refreshAccessToken();
  if (refreshed) return getAccessToken();

  return null;
}

export function getLogoutUrl(): string {
  return `${LOGOUT_URL}?client_id=${CLIENT_ID}&post_logout_redirect_uri=${encodeURIComponent(REDIRECT_URI)}`;
}

// Start a background timer to refresh the token before it expires
let refreshTimer: ReturnType<typeof setTimeout> | null = null;

export function startTokenRefreshLoop(): void {
  stopTokenRefreshLoop();
  const expiry = Number(window.localStorage.getItem(STORAGE_KEY_EXPIRY) || '0');
  const now = Date.now();
  // Refresh 60 seconds before expiry
  const delay = Math.max(0, expiry - now - 60000);
  refreshTimer = setTimeout(async () => {
    await refreshAccessToken();
    startTokenRefreshLoop(); // reschedule
  }, delay);
}

export function stopTokenRefreshLoop(): void {
  if (refreshTimer) {
    clearTimeout(refreshTimer);
    refreshTimer = null;
  }
}
