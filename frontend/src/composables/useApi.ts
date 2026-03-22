const API_BASE = (import.meta.env.PUBLIC_API_URL as string | undefined) ?? 'http://localhost:8080';

function getAuthToken(): string | null {
  // Try to get the access token from Keycloak session storage
  // Keycloak JS adapter stores tokens in sessionStorage/localStorage
  for (const key of Object.keys(window.sessionStorage)) {
    if (key.includes('kc-') || key.includes('keycloak')) {
      try {
        const val = JSON.parse(window.sessionStorage.getItem(key) || '{}');
        if (val.access_token) return val.access_token;
      } catch { /* ignore */ }
    }
  }
  // Fallback: check localStorage for token stored during OAuth redirect
  const token = window.localStorage.getItem('gandzi_access_token');
  return token;
}

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T | null> {
  const token = getAuthToken();
  if (!token) return null;

  try {
    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...(options.headers || {}),
      },
    });

    if (!response.ok) {
      console.warn(`API ${response.status} on ${path}`);
      return null;
    }

    if (response.status === 204) return null;
    return await response.json() as T;
  } catch (err) {
    console.warn(`API unreachable for ${path}:`, err);
    return null;
  }
}

export function apiGet<T>(path: string): Promise<T | null> {
  return apiFetch<T>(path);
}

export function apiPut(path: string, body: unknown): Promise<null> {
  return apiFetch(path, { method: 'PUT', body: JSON.stringify(body) });
}

export function apiPost(path: string, body: unknown): Promise<null> {
  return apiFetch(path, { method: 'POST', body: JSON.stringify(body) });
}

export function apiPatch(path: string, body: unknown): Promise<null> {
  return apiFetch(path, { method: 'PATCH', body: JSON.stringify(body) });
}

export function apiDelete(path: string): Promise<null> {
  return apiFetch(path, { method: 'DELETE' });
}
