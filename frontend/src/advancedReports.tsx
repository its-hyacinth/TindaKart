import { useEffect, useState } from 'react'
import { reportApi } from './api'

export function AdvancedReports({ vendorId, storeId }: { vendorId: number; storeId: number }) {
  const [best, setBest] = useState<Awaited<ReturnType<typeof reportApi.bestSelling>>>([])
  const [payments, setPayments] = useState<Awaited<ReturnType<typeof reportApi.payments>>>([])
  const [profit, setProfit] = useState<Awaited<ReturnType<typeof reportApi.profit>> | null>(null)
  const [deliveries, setDeliveries] = useState<Awaited<ReturnType<typeof reportApi.deliveryHistory>>>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    Promise.all([
      reportApi.bestSelling(vendorId, storeId),
      reportApi.payments(vendorId, storeId),
      reportApi.profit(vendorId, storeId),
      reportApi.deliveryHistory(vendorId, storeId),
    ]).then(([top, methods, estimate, history]) => {
      setBest(top)
      setPayments(methods)
      setProfit(estimate)
      setDeliveries(history)
    }).catch(() => undefined).finally(() => setLoading(false))
  }, [vendorId, storeId])

  function exportCsv() {
    const rows = [
      ['Best-selling products'],
      ['Product', 'Quantity', 'Revenue'],
      ...best.map(item => [item.productName, String(item.quantity), String(item.revenue)]),
      [],
      ['Payments collected'],
      ['Method', 'Count', 'Amount'],
      ...payments.map(item => [item.paymentMethod, String(item.paymentCount), String(item.amount)]),
      [],
      ['Delivery history'],
      ['Supplier', 'Expected date', 'Status', 'Items', 'Ordered', 'Received'],
      ...deliveries.map(item => [item.supplierName, item.expectedDate, item.status, String(item.itemCount), String(item.quantityOrdered), String(item.quantityReceived)]),
    ]
    const csv = rows.map(row => row.map(cell => `"${cell.replaceAll('"', '""')}"`).join(',')).join('\n')
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
    const link = document.createElement('a')
    link.href = url
    link.download = `tindakart-report-${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(url)
  }

  return <div className="report-print-area">
    {loading && <p className="loading-state" role="status">Loading reports…</p>}
    <div className="button-row export-actions"><button className="secondary-button compact export-button" onClick={exportCsv}>Export CSV</button><button className="secondary-button compact export-button" onClick={() => window.print()}>Print / Save PDF</button></div>
    <div className="advanced-report-grid">
      <div><h3>Best-selling products</h3><table><thead><tr><th>Product</th><th>Qty</th><th>Revenue</th></tr></thead><tbody>{best.map(item => <tr key={item.productId}><td>{item.productName}</td><td>{item.quantity}</td><td>₱{Number(item.revenue).toFixed(2)}</td></tr>)}</tbody></table>{!best.length && <p className="empty-state">No sales recorded today.</p>}</div>
      <div><h3>Payments collected</h3><table><thead><tr><th>Method</th><th>Count</th><th>Amount</th></tr></thead><tbody>{payments.map(item => <tr key={item.paymentMethod}><td>{item.paymentMethod}</td><td>{item.paymentCount}</td><td>₱{Number(item.amount).toFixed(2)}</td></tr>)}</tbody></table>{!payments.length && <p className="empty-state">No payments recorded today.</p>}<div className="metric-card"><span>Estimated profit</span><strong>₱{Number((profit?.revenue ?? 0) - (profit?.cost ?? 0)).toFixed(2)}</strong></div></div>
    </div>
    <section className="delivery-report"><h3>Delivery history</h3><table><thead><tr><th>Supplier</th><th>Expected</th><th>Status</th><th>Items</th><th>Ordered</th><th>Received</th></tr></thead><tbody>{deliveries.map(item => <tr key={item.id}><td>{item.supplierName}</td><td>{item.expectedDate}</td><td>{item.status}</td><td>{item.itemCount}</td><td>{item.quantityOrdered}</td><td>{item.quantityReceived}</td></tr>)}</tbody></table>{!deliveries.length && <p className="empty-state">No delivery history found.</p>}</section>
  </div>
}
