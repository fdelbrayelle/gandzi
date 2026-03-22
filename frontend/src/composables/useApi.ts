import { ensureValidToken } from './useAuth';

const API_BASE = (import.meta.env.PUBLIC_API_URL as string | undefined) ?? 'http://localhost:8080';

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T | null> {
  const token = await ensureValidToken();
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

    if (response.status === 401) {
      // Token expired mid-request, don't retry — next call will refresh
      console.warn(`API 401 on ${path} — token may have expired`);
      return null;
    }

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
