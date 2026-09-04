export type CurrentUser = { username: string; roles: string[] }

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options?.headers ?? {}) },
    ...options,
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({})) as { error?: string }
    throw new Error(body.error ?? `Request failed with status ${response.status}`)
  }
  return response.status === 204 ? (undefined as T) : response.json() as Promise<T>
}

export const authApi = {
  login: (username: string, password: string) => request<CurrentUser>('/api/auth/login', {
    method: 'POST', body: JSON.stringify({ username, password }),
  }),
  me: () => request<CurrentUser>('/api/auth/me'),
  logout: () => request<void>('/api/auth/logout', { method: 'POST' }),
}
