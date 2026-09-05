import { useEffect, useState, type FormEvent } from 'react'
import { auditApi, authApi, billingApi, packageApi, settingsApi, staffApi, type CurrentUser, type Package, type Staff } from './api'

export function AccountSecurity({ onSignedOut }: { onSignedOut: () => void }) { const [current, setCurrent] = useState(''); const [next, setNext] = useState(''); const [message, setMessage] = useState(''); async function submit(event: FormEvent) { event.preventDefault(); try { await authApi.changePassword(current, next); setMessage('Password changed. Please sign in again.'); setCurrent(''); setNext(''); setTimeout(onSignedOut, 500) } catch (error) { setMessage(error instanceof Error ? error.message : 'Unable to change password.') } } return <form className="compact-form account-security" onSubmit={submit}><h3>Account security</h3><input type="password" placeholder="Current password" value={current} onChange={e => setCurrent(e.target.value)} required /><input type="password" minLength={8} placeholder="New password (8+ characters)" value={next} onChange={e => setNext(e.target.value)} required /><button className="secondary-button compact">Change password</button>{message && <p className="form-message">{message}</p>}</form> }

export function AdminWorkspace({ user }: { user: CurrentUser }) {
  const vendorId = user.vendors?.[0]?.id ?? 0
  const canManageStaff = user.roles.includes('SUPER_ADMIN') || user.roles.includes('VENDOR_ADMIN')
  if (!canManageStaff || !vendorId) return null
  return <section className="tenant-panel admin-panel"><div className="section-heading"><div><span className="eyebrow">Administration</span><h2>Account and business controls</h2></div><span className="scope-badge">{user.roles.includes('SUPER_ADMIN') ? 'Super Admin' : 'Vendor Admin'}</span></div><div className="admin-grid"><StaffPanel vendorId={vendorId} stores={user.stores ?? []} /><VendorSettings vendorId={vendorId} /></div>{user.roles.includes('SUPER_ADMIN') ? <><PackagePanel /><PackageEditor /><AuditLogPanel /></> : <BillingPanel vendorId={vendorId} />}</section>
}

function StaffPanel({ vendorId, stores }: { vendorId: number; stores: { id: number; name: string }[] }) {
  const [staff, setStaff] = useState<Staff[]>([])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')
  const [form, setForm] = useState({ username: '', password: '', displayName: '', role: 'CASHIER' })
  const [selectedStores, setSelectedStores] = useState<number[]>(stores[0] ? [stores[0].id] : [])
  const [editingStaffId, setEditingStaffId] = useState<number | null>(null)
  const [assignmentStores, setAssignmentStores] = useState<number[]>([])

  const reload = () => { setLoading(true); return staffApi.list(vendorId).then(setStaff).catch(error => setMessage(error.message)).finally(() => setLoading(false)) }
  useEffect(() => { void reload() }, [vendorId])
  useEffect(() => { if (stores.length && !selectedStores.length) setSelectedStores([stores[0].id]) }, [stores, selectedStores.length])

  function toggleStore(storeId: number) {
    setSelectedStores(current => current.includes(storeId) ? current.filter(id => id !== storeId) : [...current, storeId])
  }

  function toggleAssignmentStore(storeId: number) {
    setAssignmentStores(current => current.includes(storeId) ? current.filter(id => id !== storeId) : [...current, storeId])
  }

  function editAssignments(member: Staff) {
    setEditingStaffId(member.id)
    setAssignmentStores(member.stores.map(store => store.id))
  }

  async function saveAssignments(member: Staff) {
    try {
      await staffApi.assignStores(vendorId, member.id, assignmentStores)
      setMessage('Store assignments updated.')
      setEditingStaffId(null)
      reload()
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Unable to update store assignments.')
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    try {
      await staffApi.create(vendorId, { username: form.username, password: form.password, displayName: form.displayName, role: form.role, storeIds: form.role === 'VENDOR_ADMIN' ? [] : selectedStores })
      setForm({ username: '', password: '', displayName: '', role: 'CASHIER' })
      setSelectedStores(stores[0] ? [stores[0].id] : [])
      setMessage('Staff account created.')
      reload()
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Unable to create staff.')
    }
  }

  return (
    <div>
      <h3>Staff</h3>
      <form className="compact-form" onSubmit={submit}>
        <input placeholder="Display name" value={form.displayName} onChange={e => setForm({ ...form, displayName: e.target.value })} required />
        <input placeholder="Username" value={form.username} onChange={e => setForm({ ...form, username: e.target.value })} required />
        <input type="password" minLength={8} placeholder="Temporary password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} required />
        <select value={form.role} onChange={e => setForm({ ...form, role: e.target.value })}><option>CASHIER</option><option>INVENTORY_STAFF</option><option>DEBT_STAFF</option><option>DELIVERY_STAFF</option><option>VENDOR_ADMIN</option></select>
        {form.role !== 'VENDOR_ADMIN' && <fieldset className="store-assignment"><legend>Assigned stores</legend>{stores.map(store => <label className="checkbox-label" key={store.id}><input type="checkbox" checked={selectedStores.includes(store.id)} onChange={() => toggleStore(store.id)} />{store.name}</label>)}</fieldset>}
        <button className="primary-button compact" disabled={form.role !== 'VENDOR_ADMIN' && !selectedStores.length}>Create account</button>
      </form>
      {message && <p className="form-message">{message}</p>}
      {loading && <p className="loading-state" role="status">Loading staff…</p>}
      <div className="store-list">{staff.map(member => <div className="store-row" key={member.id}><span><strong>{member.displayName}</strong> · {member.username}<small className="muted"> {member.roles.join(', ')} · {member.stores.map(store => store.name).join(', ') || 'All vendor stores'}</small>{editingStaffId === member.id && <span className="assignment-editor">{stores.map(store => <label className="checkbox-label" key={store.id}><input type="checkbox" checked={assignmentStores.includes(store.id)} onChange={() => toggleAssignmentStore(store.id)} />{store.name}</label>)}<button type="button" className="secondary-button compact" disabled={!assignmentStores.length} onClick={() => saveAssignments(member)}>Save stores</button></span>}</span><span className="button-row"><button type="button" className="secondary-button compact" onClick={() => editAssignments(member)}>Assign stores</button><button type="button" className="secondary-button compact" onClick={() => staffApi.status(vendorId, member.id, member.enabled ? 'INACTIVE' : 'ACTIVE').then(reload).catch(error => setMessage(error.message))}>{member.enabled ? 'Disable' : 'Enable'}</button></span></div>)}</div>
    </div>
  )
}
function VendorSettings({ vendorId }: { vendorId: number }) {
  const [form, setForm] = useState({ businessName: '', businessAddress: '', tin: '', vatRegistered: false, vatRate: 12, nearExpirationDays: 30, nearExpirationDiscountPercent: 0, receiptFooter: '' }); const [message, setMessage] = useState(''); const [loading, setLoading] = useState(true)
  useEffect(() => { setLoading(true); settingsApi.get(vendorId).then(value => setForm({ businessName: value.businessName ?? '', businessAddress: value.businessAddress ?? '', tin: value.tin ?? '', vatRegistered: value.vatRegistered, vatRate: Number(value.vatRate), nearExpirationDays: value.nearExpirationDays, nearExpirationDiscountPercent: Number(value.nearExpirationDiscountPercent ?? 0), receiptFooter: value.receiptFooter ?? '' })).catch(error => setMessage(error.message)).finally(() => setLoading(false)) }, [vendorId])
  if (loading) return <div className="compact-form"><h3>Receipt and tax settings</h3><p className="loading-state" role="status">Loading settings…</p></div>
  async function save(event: FormEvent) { event.preventDefault(); try { await settingsApi.update(vendorId, form); setMessage('Business settings saved.') } catch (error) { setMessage(error instanceof Error ? error.message : 'Unable to save settings.') } }
  return <form className="compact-form" onSubmit={save}><h3>Receipt and tax settings</h3><input placeholder="Business name" value={form.businessName} onChange={e => setForm({ ...form, businessName: e.target.value })} /><input placeholder="Business address" value={form.businessAddress} onChange={e => setForm({ ...form, businessAddress: e.target.value })} /><input placeholder="TIN" value={form.tin} onChange={e => setForm({ ...form, tin: e.target.value })} /><label className="checkbox-label"><input type="checkbox" checked={form.vatRegistered} onChange={e => setForm({ ...form, vatRegistered: e.target.checked })} /> VAT registered</label>{form.vatRegistered && <input type="number" min="0" max="100" step="0.01" placeholder="VAT rate %" value={form.vatRate} onChange={e => setForm({ ...form, vatRate: Number(e.target.value) })} />}<input type="number" min="0" placeholder="Near-expiration warning days" value={form.nearExpirationDays} onChange={e => setForm({ ...form, nearExpirationDays: Number(e.target.value) })} /><input type="number" min="0" max="100" step="0.01" placeholder="Near-expiration discount %" value={form.nearExpirationDiscountPercent} onChange={e => setForm({ ...form, nearExpirationDiscountPercent: Number(e.target.value) })} /><textarea placeholder="Receipt footer" value={form.receiptFooter} onChange={e => setForm({ ...form, receiptFooter: e.target.value })} /><button className="secondary-button compact">Save settings</button>{message && <p className="form-message">{message}</p>}</form>
}

function PackagePanel() {
  const [packages, setPackages] = useState<Package[]>([])
  const [loading, setLoading] = useState(true)
  useEffect(() => { packageApi.list().then(setPackages).catch(() => undefined).finally(() => setLoading(false)) }, [])
  return <div className="package-panel"><h3>Packages</h3>{loading ? <p className="loading-state" role="status">Loading packages…</p> : <Data rows={packages.map(item => [`${item.name} · ₱${Number(item.monthlyPrice).toFixed(2)}/month`, item.active ? 'Active' : 'Inactive', Object.keys(item.features).filter(key => item.features[key]).join(', ') || 'No features'])} />}</div>
}
function PackageEditor() { const [message, setMessage] = useState(''); const [form, setForm] = useState({ name: '', description: '', monthlyPrice: '0', annualPrice: '0', active: true }); async function submit(event: FormEvent) { event.preventDefault(); try { await packageApi.create({ ...form, monthlyPrice: Number(form.monthlyPrice), annualPrice: Number(form.annualPrice), features: { CATALOG: true, INVENTORY: true, POS: true }, limits: { MAX_STORES: 1, MAX_STAFF: 5, MAX_PRODUCTS: 100 } }); setForm({ name: '', description: '', monthlyPrice: '0', annualPrice: '0', active: true }); setMessage('Package created. Refresh to view it.') } catch (error) { setMessage(error instanceof Error ? error.message : 'Unable to create package.') } } return <form className="compact-form package-editor" onSubmit={submit}><h3>Create package</h3><div className="form-grid"><input placeholder="Package name" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required /><input placeholder="Description" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} /><input type="number" min="0" step="0.01" placeholder="Monthly price" value={form.monthlyPrice} onChange={e => setForm({ ...form, monthlyPrice: e.target.value })} required /><input type="number" min="0" step="0.01" placeholder="Annual price" value={form.annualPrice} onChange={e => setForm({ ...form, annualPrice: e.target.value })} required /></div><button className="primary-button compact">Create package</button>{message && <p className="form-message">{message}</p>}</form> }
function BillingPanel({ vendorId }: { vendorId: number }) { const [packages, setPackages] = useState<Package[]>([]); const [selected, setSelected] = useState(''); const [cycle, setCycle] = useState('MONTHLY'); const [message, setMessage] = useState(''); const [loading, setLoading] = useState(true); const [history, setHistory] = useState<{ id: number; checkoutUrl: string; amount: number; currency: string; status: string; createdAt: string }[]>([]); useEffect(() => { setLoading(true); Promise.all([packageApi.available(vendorId).then(setPackages), billingApi.history(vendorId).then(setHistory)]).catch(error => setMessage(error instanceof Error ? error.message : 'Unable to load billing.')).finally(() => setLoading(false)) }, [vendorId]); async function selectPackage() { try { await packageApi.select(vendorId, Number(selected)); setMessage('Package selected. You can now start checkout.') } catch (error) { setMessage(error instanceof Error ? error.message : 'Unable to select package.') } } async function checkout() { try { const result = await billingApi.checkout(vendorId, cycle); window.open(result.checkoutUrl, '_blank', 'noopener,noreferrer'); setMessage('Checkout opened in a new tab.') } catch (error) { setMessage(error instanceof Error ? error.message : 'Unable to create checkout.') } } return <div className="compact-form billing-panel"><h3>Subscription billing</h3>{loading ? <p className="loading-state" role="status">Loading billing...</p> : <><select value={selected} onChange={e => setSelected(e.target.value)}><option value="">Select package</option>{packages.map(item => <option key={item.id} value={item.id}>{item.name} · ₱{Number(item.monthlyPrice).toFixed(2)}/month</option>)}</select><div className="inline-form"><button type="button" className="secondary-button compact" disabled={!selected} onClick={selectPackage}>Select package</button><select value={cycle} onChange={e => setCycle(e.target.value)}><option>MONTHLY</option><option>ANNUAL</option></select><button type="button" className="primary-button compact" onClick={checkout}>Open checkout</button></div><Data rows={history.map(item => [`${item.currency} ${Number(item.amount).toFixed(2)}`, item.status, new Date(item.createdAt).toLocaleString()])} /></>} {message && <p className="form-message">{message}</p>}</div> }
function AuditLogPanel() {
  const [logs, setLogs] = useState<Awaited<ReturnType<typeof auditApi.list>>>([])
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(true)
  useEffect(() => { auditApi.list().then(setLogs).catch(error => setMessage(error instanceof Error ? error.message : 'Unable to load audit logs.')).finally(() => setLoading(false)) }, [])
  return <div className="audit-panel"><h3>Audit log</h3>{loading ? <p className="loading-state" role="status">Loading audit log...</p> : <DataTable rows={logs.map(log => [new Date(log.createdAt).toLocaleString(), log.username ?? 'System', log.action, `${log.entityType ?? ''} ${log.entityId ?? ''}`.trim(), log.details ?? ''])} headers={['Date', 'User', 'Action', 'Entity', 'Details']} empty="No audit events recorded." />}{message && <p className="form-message">{message}</p>}</div>
}

function DataTable({ headers, rows, empty }: { headers: string[]; rows: string[][]; empty: string }) { return rows.length ? <div className="data-table-wrap"><table><thead><tr>{headers.map(header => <th key={header}>{header}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={index}>{row.map((cell, i) => <td key={i}>{cell}</td>)}</tr>)}</tbody></table></div> : <p className="muted">{empty}</p> }
function Data({ rows }: { rows: string[][] }) { return rows.length ? <div className="data-table-wrap"><table><thead><tr><th>Package</th><th>Status</th><th>Features</th></tr></thead><tbody>{rows.map((row, index) => <tr key={index}>{row.map((cell, i) => <td key={i}>{cell}</td>)}</tr>)}</tbody></table></div> : <p className="muted">No packages found.</p> }
