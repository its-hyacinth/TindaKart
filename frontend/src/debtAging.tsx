import { useEffect, useState } from 'react'
import { debtApi } from './api'

export function DebtAging({ vendorId }: { vendorId: number }) {
  const [rows, setRows] = useState<Awaited<ReturnType<typeof debtApi.aging>>>([])
  useEffect(() => { debtApi.aging(vendorId).then(setRows).catch(() => undefined) }, [vendorId])
  return <div className="aging-panel"><h3>Debt aging</h3>{rows.length ? <table><thead><tr><th>Bucket</th><th>Accounts</th><th>Balance</th></tr></thead><tbody>{rows.map(row => <tr key={row.bucket}><td>{row.bucket.replaceAll('_', ' ')}</td><td>{row.accountCount}</td><td>₱{Number(row.balance).toFixed(2)}</td></tr>)}</tbody></table> : <p className="empty-state">No outstanding debt.</p>}</div>
}
