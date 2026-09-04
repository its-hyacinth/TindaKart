import { StrictMode, useEffect, useState, type FormEvent } from 'react'
import { createRoot } from 'react-dom/client'
import { authApi, tenantApi, type CurrentUser, type Store, type Vendor } from './api'
import { OperationsWorkspace } from './operations'
import { AdminWorkspace } from './admin'
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
    <TenantDashboard user={user} />
    <AdminWorkspace user={user} />
    <OperationsWorkspace user={user} />
    <section className="next-grid">
      <article><span>01</span><h2>Secure foundation</h2><p>Passwords are stored as BCrypt hashes, never as plain text.</p></article>
      <article><span>02</span><h2>Role-aware</h2><p>Owner, cashier, and debt staff roles are ready for protected modules.</p></article>
      <article><span>03</span><h2>Next module</h2><p>Product categories and the catalog will use this authenticated API.</p></article>
    </section>
  </main>
}

function TenantDashboard({ user }: { user: CurrentUser }) {
  const isSuperAdmin = user.roles.includes('SUPER_ADMIN')
  const [vendors, setVendors] = useState<Vendor[]>(user.vendors ?? [])
  const [stores, setStores] = useState<Store[]>(user.stores ?? [])
  const [vendorName, setVendorName] = useState('')
  const [storeName, setStoreName] = useState('')
  const [storeCode, setStoreCode] = useState('')
  const [selectedVendor, setSelectedVendor] = useState<number>(vendors[0]?.id ?? 0)
  const [message, setMessage] = useState('')

  useEffect(() => {
    if (!isSuperAdmin) return
    tenantApi.vendors().then(items => { setVendors(items); setSelectedVendor(items[0]?.id ?? 0) }).catch(() => undefined)
  }, [isSuperAdmin])

  useEffect(() => {
    if (!selectedVendor) return
    tenantApi.stores(selectedVendor).then(setStores).catch(() => undefined)
  }, [selectedVendor])

  async function addVendor(event: FormEvent) {
    event.preventDefault(); setMessage('')
    try { const vendor = await tenantApi.createVendor(vendorName); setVendors(items => [...items, vendor]); setVendorName(''); setSelectedVendor(vendor.id); setMessage('Vendor created.') }
    catch (error) { setMessage(error instanceof Error ? error.message : 'Unable to create vendor.') }
  }

  async function addStore(event: FormEvent) {
    event.preventDefault(); setMessage('')
    try { const store = await tenantApi.createStore(selectedVendor, storeName, storeCode, ''); setStores(items => [...items, store]); setStoreName(''); setStoreCode(''); setMessage('Store created.') }
    catch (error) { setMessage(error instanceof Error ? error.message : 'Unable to create store.') }
  }

  return <section className="tenant-panel">
    <div className="section-heading"><div><span className="eyebrow">Module 5A</span><h2>Business workspace</h2></div><span className="scope-badge">{isSuperAdmin ? 'Platform scope' : 'Assigned stores'}</span></div>
    {isSuperAdmin && <form className="inline-form" onSubmit={addVendor}><input placeholder="New vendor name" value={vendorName} onChange={event => setVendorName(event.target.value)} required /><button className="primary-button compact">Add vendor</button></form>}
    <label className="select-label">Vendor<select value={selectedVendor} onChange={event => setSelectedVendor(Number(event.target.value))} disabled={!vendors.length}><option value={0}>Select vendor</option>{vendors.map(vendor => <option key={vendor.id} value={vendor.id}>{vendor.name} · {vendor.status}</option>)}</select></label>
    {selectedVendor > 0 && <form className="inline-form" onSubmit={addStore}><input placeholder="Branch name" value={storeName} onChange={event => setStoreName(event.target.value)} required /><input placeholder="Code" value={storeCode} onChange={event => setStoreCode(event.target.value)} required /><button className="primary-button compact">Add store</button></form>}
    {message && <p className="form-message">{message}</p>}
    <div className="store-list">{stores.filter(store => !selectedVendor || store.vendorId === selectedVendor).map(store => <div className="store-row" key={store.id}><strong>{store.name}</strong><span>{store.code} · {store.status}</span></div>)}{!stores.length && <p className="muted">No stores found for this vendor yet.</p>}</div>
  </section>
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
