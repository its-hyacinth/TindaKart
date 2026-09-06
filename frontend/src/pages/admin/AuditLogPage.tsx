import { useEffect, useState } from 'react'
import { Activity, RefreshCw } from 'lucide-react'
import { auditApi } from '../../api'
import { AdminPageHeader } from '../../components/admin/AdminShell'
import { Button } from '../../components/ui/button'
import { Card, CardContent } from '../../components/ui/card'

export function AuditLogPage() {
  const [entries, setEntries] = useState<{ id: number; action: string; entityType?: string; entityId?: string; details?: string; createdAt: string; username?: string }[]>([])
  const [loading, setLoading] = useState(true)
  function load() { setLoading(true); auditApi.list().then(setEntries).catch(() => setEntries([])).finally(() => setLoading(false)) }
  useEffect(load, [])
  return <><AdminPageHeader eyebrow="Governance" title="Audit log" description="Trace platform-level changes across vendor accounts, pricing, access, and administrative actions." action={<Button variant="secondary" onClick={load}><RefreshCw size={16} /> Refresh</Button>} /><Card><CardContent className="p-0">{loading ? <p className="p-8 text-sm text-slate-500">Loading audit events…</p> : entries.length === 0 ? <div className="grid place-items-center p-16 text-center"><Activity className="mb-3 text-slate-300" size={32} /><p className="text-sm text-slate-500">No audit events found.</p></div> : <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead className="border-b border-slate-100 bg-slate-50 text-xs uppercase tracking-wider text-slate-500"><tr><th className="px-5 py-4">Action</th><th className="px-5 py-4">Entity</th><th className="px-5 py-4">Actor</th><th className="px-5 py-4">Details</th><th className="px-5 py-4">Time</th></tr></thead><tbody>{entries.map(entry => <tr key={entry.id} className="border-b border-slate-100 last:border-0"><td className="px-5 py-4 font-bold">{entry.action}</td><td className="px-5 py-4 text-slate-500">{entry.entityType ?? '—'} {entry.entityId ? `#${entry.entityId}` : ''}</td><td className="px-5 py-4">{entry.username ?? 'System'}</td><td className="max-w-xs truncate px-5 py-4 text-slate-500">{entry.details ?? '—'}</td><td className="whitespace-nowrap px-5 py-4 text-slate-500">{new Date(entry.createdAt).toLocaleString()}</td></tr>)}</tbody></table></div>}</CardContent></Card></>
}
