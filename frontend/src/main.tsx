import { StrictMode, useEffect, useState, type FormEvent } from 'react'
import { createRoot } from 'react-dom/client'
import { authApi, type CurrentUser } from './api'
import './styles.css'

function App() {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [checkingSession, setCheckingSession] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    authApi.me().then(setUser).catch(() => undefined).finally(() => setCheckingSession(false))
  }, [])

  if (checkingSession) return <main className="centered"><p>Checking session…</p></main>
  if (!user) return <Login onLogin={setUser} onError={setError} error={error} />

  return <main className="shell">
    <section className="hero">
      <span className="eyebrow">Module 1 · Authentication</span>
      <h1>TindaKart</h1>
      <p className="subtitle">A responsive store management system for desktop, tablet, and mobile.</p>
    </section>
    <section className="status-card" aria-live="polite">
      <div><span className="status-dot" /><strong>Signed in as {user.username}</strong></div>
      <p>Role: {user.roles.join(', ') || 'No role assigned'}</p>
      <button className="secondary-button" onClick={() => authApi.logout().then(() => setUser(null))}>Sign out</button>
    </section>
    <section className="next-grid">
      <article><span>01</span><h2>Secure foundation</h2><p>Passwords are stored as BCrypt hashes, never as plain text.</p></article>
      <article><span>02</span><h2>Role-aware</h2><p>Owner, cashier, and debt staff roles are ready for protected modules.</p></article>
      <article><span>03</span><h2>Next module</h2><p>Product categories and the catalog will use this authenticated API.</p></article>
    </section>
  </main>
}

function Login({ onLogin, onError, error }: { onLogin: (user: CurrentUser) => void; onError: (message: string) => void; error: string }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault(); setSubmitting(true); onError('')
    try { onLogin(await authApi.login(username, password)) }
    catch (err) { onError(err instanceof Error ? err.message : 'Unable to sign in') }
    finally { setSubmitting(false) }
  }

  return <main className="auth-shell"><form className="login-card" onSubmit={submit}>
    <span className="eyebrow">TindaKart PWA</span><h1>Welcome back</h1>
    <p className="subtitle">Sign in to manage your store.</p>
    <label>Username<input value={username} onChange={event => setUsername(event.target.value)} autoComplete="username" required /></label>
    <label>Password<input type="password" value={password} onChange={event => setPassword(event.target.value)} autoComplete="current-password" required /></label>
    {error && <p className="error-message">{error}</p>}
    <button className="primary-button" disabled={submitting}>{submitting ? 'Signing in…' : 'Sign in'}</button>
  </form></main>
}

createRoot(document.getElementById('root')!).render(<StrictMode><App /></StrictMode>)
