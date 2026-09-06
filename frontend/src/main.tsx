import { StrictMode, useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { authApi, tenantApi, type CurrentUser } from './api'
import { AdminShell, type AdminView } from './components/admin/AdminShell'
import { AdminOverviewPage } from './pages/admin/AdminOverviewPage'
import { AuditLogPage } from './pages/admin/AuditLogPage'
import { AdminSettingsPage } from './pages/admin/AdminSettingsPage'
import { BillingActivityPage } from './pages/admin/BillingActivityPage'
import { PlansPricingPage } from './pages/admin/PlansPricingPage'
import { VendorManagementPage } from './pages/admin/VendorManagementPage'
import { VendorModulePage } from './pages/vendor/VendorModulePage'
import { VendorOverviewPage } from './pages/vendor/VendorOverviewPage'
import { VendorShell, type VendorView } from './components/vendor/VendorShell'
import { LandingPage } from './pages/LandingPage'
import { LoginPage, RegistrationPage } from './pages/LoginPage'
import './index.css'

type Screen = 'landing' | 'login' | 'register'
type InstallPromptEvent = Event & { prompt: () => Promise<void>; userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }> }

function isSuperAdmin(user: CurrentUser) {
  return user.roles.some(role => role === 'SUPER_ADMIN' || role === 'ROLE_SUPER_ADMIN')
}

function isAdminHost() {
  return window.location.hostname === 'admin.localhost' || window.location.hostname.startsWith('admin.')
}

function adminOrigin() {
  const hostname = window.location.hostname
  const baseHost = hostname === 'localhost' || hostname === '127.0.0.1' || hostname === 'admin.localhost' ? 'admin.localhost' : `admin.${hostname.replace(/^admin\./, '').replace(/^www\./, '')}`
  return `${window.location.protocol}//${baseHost}${window.location.port ? `:${window.location.port}` : ''}`
}

function storeOrigin() {
  const hostname = window.location.hostname
  const baseHost = hostname === 'admin.localhost' ? 'localhost' : hostname.replace(/^admin\./, '')
  return `${window.location.protocol}//${baseHost}${window.location.port ? `:${window.location.port}` : ''}`
}

function App() {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [screen, setScreen] = useState<Screen>('landing')
  const [checking, setChecking] = useState(true)
  const [installPrompt, setInstallPrompt] = useState<InstallPromptEvent | null>(null)
  const [installed, setInstalled] = useState(false)
  const [installMessage, setInstallMessage] = useState('')
  useEffect(() => {
    authApi.me().then(async currentUser => {
      if (!isAdminHost() && isSuperAdmin(currentUser)) {
        window.localStorage.clear()
        window.sessionStorage.clear()
        await authApi.logout().catch(() => undefined)
        setUser(null)
        return
      }
      setUser(currentUser)
    }).catch(() => undefined).finally(() => setChecking(false))
  }, [])
  useEffect(() => {
    const standalone = window.matchMedia('(display-mode: standalone)').matches || ('standalone' in navigator && Boolean((navigator as Navigator & { standalone?: boolean }).standalone))
    setInstalled(standalone)
    const available = (event: Event) => { event.preventDefault(); setInstallPrompt(event as InstallPromptEvent) }
    const completed = () => { setInstalled(true); setInstallPrompt(null); setInstallMessage('TindaKart was added to your device.') }
    window.addEventListener('beforeinstallprompt', available); window.addEventListener('appinstalled', completed)
    return () => { window.removeEventListener('beforeinstallprompt', available); window.removeEventListener('appinstalled', completed) }
  }, [])
  async function install() {
    setInstallMessage('')
    if (installed) return setInstallMessage('TindaKart is already installed on this device.')
    if (!installPrompt) return setInstallMessage('Use your browser menu and choose “Install app” or “Add to Home Screen”.')
    await installPrompt.prompt(); const result = await installPrompt.userChoice; setInstallPrompt(null); if (result.outcome === 'accepted') setInstallMessage('TindaKart is ready to install.')
  }
  if (checking) return <main className="grid min-h-screen place-items-center bg-slate-950 text-sm font-semibold text-white">Loading TindaKart…</main>
  if (user) {
    if (isSuperAdmin(user) && isAdminHost()) return <SuperAdminPortal user={user} onSignOut={() => authApi.logout().finally(() => setUser(null))} />
    if (isSuperAdmin(user)) return <AdminSubdomainNotice />
    if (isAdminHost()) return <AdminAccessDenied onSignOut={() => authApi.logout().finally(() => setUser(null))} />
    return <VendorPortal user={user} onSignOut={() => authApi.logout().finally(() => setUser(null))} />
  }
  if (isAdminHost() && screen === 'landing') return <LoginPage onLogin={setUser} onBack={() => { window.location.href = storeOrigin() }} onRegister={() => { window.location.href = storeOrigin() }} />
  if (screen === 'login') return <LoginPage onLogin={setUser} onBack={() => setScreen('landing')} onRegister={() => setScreen('register')} />
  if (screen === 'register') return <RegistrationPage onBack={() => setScreen('landing')} onRegistered={() => setScreen('login')} />
  return <LandingPage onLogin={() => setScreen('login')} onRegister={() => setScreen('register')} onInstall={install} installed={installed} installMessage={installMessage} />
}

function AdminSubdomainNotice() {
  return <main className="grid min-h-screen place-items-center bg-slate-950 px-5"><section className="w-full max-w-md rounded-2xl bg-white p-7 shadow-2xl"><span className="grid size-10 place-items-center rounded-xl bg-slate-950 text-xs font-black text-white">TK</span><p className="mt-7 text-[11px] font-bold uppercase tracking-[.18em] text-indigo-600">Super Admin portal</p><h1 className="mt-2 text-2xl font-black tracking-tight">Use the admin portal</h1><p className="mt-3 text-sm leading-6 text-slate-500">Super Admin access is isolated from store operations. Continue to the dedicated admin subdomain.</p><button className="mt-6 w-full rounded-xl bg-slate-950 px-4 py-3 text-sm font-bold text-white hover:bg-indigo-700" onClick={() => { window.location.href = adminOrigin() }}>Open admin portal</button></section></main>
}

function AdminAccessDenied({ onSignOut }: { onSignOut: () => void }) {
  return <main className="grid min-h-screen place-items-center bg-slate-950 px-5"><section className="w-full max-w-md rounded-2xl bg-white p-7 shadow-2xl"><span className="grid size-10 place-items-center rounded-xl bg-slate-950 text-xs font-black text-white">TK</span><p className="mt-7 text-[11px] font-bold uppercase tracking-[.18em] text-rose-600">Admin access required</p><h1 className="mt-2 text-2xl font-black tracking-tight">This portal is restricted.</h1><p className="mt-3 text-sm leading-6 text-slate-500">Your account is not assigned the Super Admin role. Sign out and use the store operations application instead.</p><button className="mt-6 w-full rounded-xl bg-slate-950 px-4 py-3 text-sm font-bold text-white hover:bg-indigo-700" onClick={onSignOut}>Sign out</button></section></main>
}

function SuperAdminPortal({ user, onSignOut }: { user: CurrentUser; onSignOut: () => void }) {
  const [view, setView] = useState<AdminView>('overview')
  const [vendors, setVendors] = useState(user.vendors)
  useEffect(() => { tenantApi.vendors().then(setVendors).catch(() => setVendors(user.vendors)) }, [user.vendors])
  return <AdminShell view={view} onViewChange={setView} onSignOut={onSignOut} username={user.username}>{view === 'overview' && <AdminOverviewPage user={user} vendors={vendors} onViewChange={setView} />}{view === 'vendors' && <VendorManagementPage initialVendors={vendors} />}{view === 'plans' && <PlansPricingPage />}{view === 'billing' && <BillingActivityPage vendors={vendors} />}{view === 'audit' && <AuditLogPage />}{view === 'settings' && <AdminSettingsPage />}</AdminShell>
}

function VendorPortal({ user, onSignOut }: { user: CurrentUser; onSignOut: () => void }) {
  const [view, setView] = useState<VendorView>('overview')
  const [vendorId, setVendorId] = useState<number | undefined>(user.vendors[0]?.id)
  const [storeId, setStoreId] = useState<number | undefined>(user.stores[0]?.id)
  const isAdmin = user.roles.some(role => role === 'VENDOR_ADMIN' || role === 'ROLE_VENDOR_ADMIN')
  useEffect(() => { authApi.context().then(context => { setVendorId(context.vendorId ?? user.vendors[0]?.id); setStoreId(context.storeId ?? user.stores[0]?.id) }).catch(() => undefined) }, [user.vendors, user.stores])
  function changeVendor(nextVendorId: number) { setVendorId(nextVendorId); const nextStore = user.stores.find(store => store.vendorId === nextVendorId); setStoreId(nextStore?.id); if (nextStore) authApi.setContext(nextVendorId, nextStore.id).catch(() => undefined) }
  function changeStore(nextStoreId: number) { setStoreId(nextStoreId || undefined); if (nextStoreId && vendorId) authApi.setContext(vendorId, nextStoreId).catch(() => undefined) }
  const vendor = user.vendors.find(item => item.id === vendorId)
  return <VendorShell view={view} onViewChange={setView} onSignOut={onSignOut} username={user.username} vendors={user.vendors} stores={user.stores} vendorId={vendorId} storeId={storeId} onVendorChange={changeVendor} onStoreChange={changeStore} isAdmin={isAdmin}>{view === 'overview' ? <VendorOverviewPage vendor={vendor} storeId={storeId} onViewChange={setView} /> : <VendorModulePage view={view} vendor={vendor} storeId={storeId} stores={user.stores} />}</VendorShell>
}

function Workspace({ user, onSignOut }: { user: CurrentUser; onSignOut: () => void }) {
  return <main className="min-h-screen bg-slate-100"><header className="border-b border-slate-200 bg-white"><div className="mx-auto flex max-w-7xl items-center justify-between px-5 py-4 lg:px-8"><div className="flex items-center gap-3"><span className="grid size-9 place-items-center rounded-xl bg-slate-950 text-xs font-black text-white">TK</span><div><strong className="block text-sm">TindaKart</strong><span className="block text-xs text-slate-500">New workspace foundation</span></div></div><button className="rounded-xl px-3 py-2 text-sm font-semibold text-slate-500 hover:bg-slate-100 hover:text-slate-950" onClick={onSignOut}>Sign out</button></div></header><section className="mx-auto max-w-7xl px-5 py-16 lg:px-8"><p className="text-xs font-bold uppercase tracking-[.16em] text-indigo-600">{user.roles.join(' · ')}</p><h1 className="mt-3 text-4xl font-black tracking-[-.06em] text-slate-950">Welcome, {user.username}.</h1><p className="mt-3 max-w-xl text-slate-600">The frontend has been restarted on a new component foundation. POS, inventory, and administration modules will be rebuilt into this workspace one by one.</p><div className="mt-8 grid gap-4 sm:grid-cols-3"><div className="rounded-2xl border border-slate-200 bg-white p-5"><strong className="block text-sm">Vendor access</strong><span className="mt-2 block text-2xl font-black">{user.vendors?.length ?? 0}</span></div><div className="rounded-2xl border border-slate-200 bg-white p-5"><strong className="block text-sm">Stores</strong><span className="mt-2 block text-2xl font-black">{user.stores?.length ?? 0}</span></div><div className="rounded-2xl border border-slate-200 bg-white p-5"><strong className="block text-sm">Next</strong><span className="mt-2 block text-sm font-semibold text-indigo-600">POS module rebuild</span></div></div></section></main>
}

createRoot(document.getElementById('root')!).render(<StrictMode><App /></StrictMode>)
