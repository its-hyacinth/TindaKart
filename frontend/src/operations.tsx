import { useEffect, useRef, useState, type FormEvent } from 'react'
import { authApi, catalogApi, debtApi, deliveryApi, inventoryApi, packageApi, posApi, receiptApi, reportApi, settingsApi, supplierApi, type BusinessSettings, type CurrentUser, type Delivery, type Product, type Supplier } from './api'
import { AdvancedReports } from './advancedReports'
import { DebtAging } from './debtAging'
import { DeliveryReceiving } from './deliveryReceiving'

type Tab = 'POS' | 'Inventory' | 'Catalog' | 'Debt' | 'Deliveries' | 'Reports'

export function OperationsWorkspace({ user }: { user: CurrentUser }) {
  const [tab, setTab] = useState<Tab>('POS')
  const stores = user.stores ?? []
  const vendors = user.vendors ?? []
  const [vendorId, setVendorId] = useState(vendors[0]?.id ?? 0)
  const [storeId, setStoreId] = useState(stores[0]?.id ?? 0)
  const [features, setFeatures] = useState<Record<string, boolean> | null>(null)

  useEffect(() => { if (!vendorId && vendors[0]) setVendorId(vendors[0].id) }, [vendorId, vendors])
  useEffect(() => { const match = stores.find(store => store.vendorId === vendorId); if (match) setStoreId(match.id) }, [vendorId, stores])
  useEffect(() => { authApi.context().then(context => { if (context.vendorId && vendors.some(vendor => vendor.id === context.vendorId)) setVendorId(context.vendorId); if (context.storeId && stores.some(store => store.id === context.storeId)) setStoreId(context.storeId) }).catch(() => undefined) }, [])
  useEffect(() => { if (user.roles.includes('SUPER_ADMIN') || !vendorId) { setFeatures(null); return } packageApi.entitlements(vendorId).then(value => setFeatures(value.features)).catch(() => setFeatures({})) }, [vendorId, user.roles])
  const allTabs: [Tab, string][] = [['POS', 'POS'], ['Inventory', 'INVENTORY'], ['Catalog', 'CATALOG'], ['Debt', 'DEBT'], ['Deliveries', 'DELIVERY'], ['Reports', 'REPORTS']]
  const tabs = allTabs.filter(([, feature]) => features === null || features[feature] !== false).map(([tab]) => tab)
  useEffect(() => { if (tabs.length && !tabs.includes(tab)) setTab(tabs[0]) }, [tabs.join('|'), tab])
  return <section className="operations-panel">
    <div className="workspace-toolbar"><div><span className="eyebrow">Store operations</span><h2>{tab}</h2></div><div className="context-selects"><select value={vendorId} onChange={event => { const next = Number(event.target.value); setVendorId(next); const store = stores.find(item => item.vendorId === next); if (store) { setStoreId(store.id); void authApi.setContext(next, store.id) } }}>{vendors.map(vendor => <option key={vendor.id} value={vendor.id}>{vendor.name}</option>)}</select><select value={storeId} onChange={event => { const next = Number(event.target.value); setStoreId(next); void authApi.setContext(vendorId, next) }}>{stores.filter(store => store.vendorId === vendorId).map(store => <option key={store.id} value={store.id}>{store.name}</option>)}</select></div></div>
    <div className="workspace-body"><nav className="desktop-nav" aria-label="Desktop operations">{tabs.map(item => <button className={tab === item ? 'tab active' : 'tab'} key={item} onClick={() => setTab(item)}>{item}</button>)}</nav><div className="workspace-content"><nav className="mobile-nav" aria-label="Mobile operations">{tabs.map(item => <button className={tab === item ? 'tab active' : 'tab'} key={item} onClick={() => setTab(item)}>{item}</button>)}</nav>{!vendorId || !storeId ? <p className="empty-state">Create a vendor and store before opening operations.</p> : tab === 'POS' ? <PosPage vendorId={vendorId} storeId={storeId} /> : tab === 'Inventory' ? <InventoryPage vendorId={vendorId} storeId={storeId} /> : tab === 'Catalog' ? <><CatalogCreateForm vendorId={vendorId} /><CatalogPage vendorId={vendorId} /></> : tab === 'Debt' ? <><DebtPage vendorId={vendorId} /><DebtAging vendorId={vendorId} /></> : tab === 'Deliveries' ? <><DeliveryCreateForm vendorId={vendorId} storeId={storeId} /><DeliveryReceiving vendorId={vendorId} storeId={storeId} /><DeliveryPage vendorId={vendorId} storeId={storeId} /></> : <><ReportsPage vendorId={vendorId} storeId={storeId} /><AdvancedReports vendorId={vendorId} storeId={storeId} /></>}</div></div>
  </section>
}

function CatalogCreateForm({ vendorId }: { vendorId: number }) {
  const [categories, setCategories] = useState<Awaited<ReturnType<typeof catalogApi.categories>>>([]); const [message, setMessage] = useState(''); const [form, setForm] = useState({ name: '', sku: '', unitType: 'PIECE', categoryId: '', costPrice: '', retailPrice: '', bulkPrice: '', bulkThreshold: '', reorderLevel: '0', expirationApplicable: false, barcode: '' })
  useEffect(() => { catalogApi.categories(vendorId).then(setCategories).catch(error => setMessage(error.message)) }, [vendorId])
  async function submit(event: FormEvent) { event.preventDefault(); try { await catalogApi.createProduct(vendorId, { name: form.name, sku: form.sku, unitType: form.unitType, categoryId: form.categoryId ? Number(form.categoryId) : null, costPrice: Number(form.costPrice), retailPrice: Number(form.retailPrice), bulkPrice: form.bulkPrice ? Number(form.bulkPrice) : null, bulkThreshold: form.bulkThreshold ? Number(form.bulkThreshold) : null, reorderLevel: Number(form.reorderLevel), expirationApplicable: form.expirationApplicable, barcodes: [form.barcode] }); setMessage('Product created.'); setForm({ ...form, name: '', sku: '', costPrice: '', retailPrice: '', bulkPrice: '', bulkThreshold: '', barcode: '' }) } catch (error) { setMessage(error instanceof Error ? error.message : 'Unable to create product.') } }
  return <form className="compact-form catalog-create-form" onSubmit={submit}><h3>Add product</h3><div className="form-grid"><input placeholder="Product name" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required /><input placeholder="SKU" value={form.sku} onChange={e => setForm({ ...form, sku: e.target.value })} required /><select value={form.categoryId} onChange={e => setForm({ ...form, categoryId: e.target.value })}><option value="">No category</option>{categories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}</select><select value={form.unitType} onChange={e => setForm({ ...form, unitType: e.target.value })}><option>PIECE</option><option>PACK</option><option>BOTTLE</option><option>KILOGRAM</option><option>LITER</option><option>OTHER</option></select><input type="number" min="0" step="0.01" placeholder="Cost price" value={form.costPrice} onChange={e => setForm({ ...form, costPrice: e.target.value })} required /><input type="number" min="0" step="0.01" placeholder="Retail price" value={form.retailPrice} onChange={e => setForm({ ...form, retailPrice: e.target.value })} required /><input placeholder="Barcode" value={form.barcode} onChange={e => setForm({ ...form, barcode: e.target.value })} required /><input type="number" min="0" step="0.01" placeholder="Bulk price (optional)" value={form.bulkPrice} onChange={e => setForm({ ...form, bulkPrice: e.target.value })} /><input type="number" min="1" step="1" placeholder="Bulk threshold" value={form.bulkThreshold} onChange={e => setForm({ ...form, bulkThreshold: e.target.value })} /><input type="number" min="0" step="1" placeholder="Reorder level" value={form.reorderLevel} onChange={e => setForm({ ...form, reorderLevel: e.target.value })} /><label className="checkbox-label"><input type="checkbox" checked={form.expirationApplicable} onChange={e => setForm({ ...form, expirationApplicable: e.target.checked })} /> Expiration applies</label></div><button className="primary-button compact">Create product</button>{message && <p className="form-message">{message}</p>}</form>
}

function PosPage({ vendorId, storeId }: { vendorId: number; storeId: number }) {
  const [products, setProducts] = useState<Product[]>([])
  const [search, setSearch] = useState('')
  const [cart, setCart] = useState<{ product: Product; quantity: number }[]>([])
  const [tendered, setTendered] = useState('')
  const [discount, setDiscount] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('CASH')
  const [message, setMessage] = useState('')
  const [lastSale, setLastSale] = useState<Awaited<ReturnType<typeof posApi.sale>> | null>(null)
  const [businessSettings, setBusinessSettings] = useState<BusinessSettings | null>(null)

  useEffect(() => {
    catalogApi.products(vendorId, search).then(setProducts).catch(error => setMessage(error.message))
  }, [vendorId, search])
  useEffect(() => { settingsApi.get(vendorId).then(setBusinessSettings).catch(() => undefined) }, [vendorId])

  const price = (product: Product, quantity: number) =>
    product.bulkPrice != null && product.bulkThreshold != null && quantity >= product.bulkThreshold
      ? Number(product.bulkPrice)
      : Number(product.retailPrice)
  const subtotal = cart.reduce((sum, line) => sum + price(line.product, line.quantity) * line.quantity, 0)
  const total = Math.max(0, subtotal - Number(discount || 0))

  function add(product: Product) {
    setCart(lines => {
      const existing = lines.find(line => line.product.id === product.id)
      return existing
        ? lines.map(line => line.product.id === product.id ? { ...line, quantity: line.quantity + 1 } : line)
        : [...lines, { product, quantity: 1 }]
    })
  }

  function remove(productId: number) {
    setCart(lines => lines.filter(line => line.product.id !== productId))
  }

  function clearSale() {
    setCart([])
    setDiscount('')
    setTendered('')
  }

  async function scan(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key !== 'Enter' || !search.trim()) return
    event.preventDefault()
    try {
      add(await catalogApi.barcode(vendorId, search.trim()))
      setSearch('')
    } catch {
      setMessage('No product found for that barcode.')
    }
  }

  async function checkout() {
    try {
      const result = await posApi.sale(
        vendorId,
        storeId,
        cart.map(line => ({ productId: line.product.id, quantity: line.quantity })),
        Number(tendered || total),
        Number(discount || 0),
        paymentMethod,
      )
      setLastSale(result)
      setMessage(`Sale ${result.receiptNumber} completed · Change ₱${result.changeAmount.toFixed(2)}`)
      clearSale()
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Unable to complete sale')
    }
  }

  async function voidLastSale() {
    if (!lastSale || !window.confirm('Void this completed sale and restore its stock?')) return
    try {
      await posApi.voidSale(vendorId, storeId, lastSale.id)
      setMessage('Sale voided and stock restored.')
      setLastSale(null)
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Unable to void sale.')
    }
  }

  async function printReceipt() {
    if (!lastSale) return
    try {
      await receiptApi.markPrinted(vendorId, storeId, lastSale.id)
      window.print()
      setMessage('Receipt sent to browser print preview.')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Unable to record receipt printing.')
    }
  }

  return (
    <div>
      <div className="pos-layout">
        <div>
          <div className="scan-row">
            <input className="search-input" placeholder="Search or scan barcode, then press Enter" value={search} onChange={event => setSearch(event.target.value)} onKeyDown={scan} autoFocus />
            <CameraScanButton onScan={code => { setSearch(''); catalogApi.barcode(vendorId, code).then(add).catch(() => setMessage('No product found for that barcode.')) }} />
          </div>
          <div className="product-grid">
            {[...products].sort((a, b) => a.name.localeCompare(b.name)).map(product => (
              <button className="product-card" key={product.id} onClick={() => add(product)}>
                <strong>{product.name}</strong>
                <span>{product.sku} · {product.unitType}</span>
                <b>₱{Number(product.retailPrice).toFixed(2)}{product.bulkPrice != null && <small> Bulk ₱{Number(product.bulkPrice).toFixed(2)}</small>}</b>
              </button>
            ))}
          </div>
        </div>
        <aside className="cart-card">
          <h3>Current sale</h3>
          {cart.map(line => (
            <div className="cart-line" key={line.product.id}>
              <span>{line.product.name}<small> @ ₱{price(line.product, line.quantity).toFixed(2)}</small></span>
              <span className="quantity-controls">
                <button type="button" aria-label={`Remove ${line.product.name}`} onClick={() => line.quantity > 1 ? setCart(lines => lines.map(item => item.product.id === line.product.id ? { ...item, quantity: item.quantity - 1 } : item)) : remove(line.product.id)}>−</button>
                <b>{line.quantity}</b>
                <button type="button" aria-label={`Add ${line.product.name}`} onClick={() => add(line.product)}>+</button>
                <strong>₱{(price(line.product, line.quantity) * line.quantity).toFixed(2)}</strong>
              </span>
            </div>
          ))}
          <div className="summary-line"><span>Subtotal</span><b>₱{subtotal.toFixed(2)}</b></div>
          <div className="summary-line"><span>Discount</span><input className="small-input" type="number" min="0" max={subtotal} step="0.01" value={discount} onChange={event => setDiscount(event.target.value)} /></div>
          <div className="total-line"><span>Total</span><strong>₱{total.toFixed(2)}</strong></div>
          <select value={paymentMethod} onChange={event => setPaymentMethod(event.target.value)}><option value="CASH">Cash</option><option value="CARD">Card</option><option value="EWALLET">E-wallet</option></select>
          <input type="number" min={total} step="0.01" placeholder="Amount tendered" value={tendered} onChange={event => setTendered(event.target.value)} />
          <div className="button-row">
            <button className="primary-button" disabled={!cart.length} onClick={checkout}>Complete sale</button>
            <button type="button" className="secondary-button compact" disabled={!cart.length} onClick={clearSale}>Clear sale</button>
          </div>
        </aside>
      </div>
      {message && <p className="form-message">{message}</p>}
      {lastSale && (
        <div className="receipt-preview">
          <span className="eyebrow">Receipt preview</span>
          {businessSettings?.businessName && <h2>{businessSettings.businessName}</h2>}
          {businessSettings?.businessAddress && <p>{businessSettings.businessAddress}</p>}
          {businessSettings?.tin && <p>TIN: {businessSettings.tin}</p>}
          <h3>{lastSale.receiptNumber}</h3>
          <p>Subtotal ₱{lastSale.subtotal.toFixed(2)} · Discount ₱{lastSale.discountAmount.toFixed(2)} · {businessSettings?.vatRegistered ? `VAT (${businessSettings.vatRate}%)` : 'VAT'} ₱{lastSale.vatAmount.toFixed(2)}</p>
          <p>Payment: {lastSale.paymentMethod} · Change ₱{lastSale.changeAmount.toFixed(2)}</p>
          <strong>Total ₱{lastSale.totalAmount.toFixed(2)}</strong>
          {businessSettings?.receiptFooter && <p>{businessSettings.receiptFooter}</p>}
          <div className="button-row">
            <button className="secondary-button print-button" onClick={printReceipt}>Print receipt</button>
            <button className="secondary-button compact" onClick={voidLastSale}>Void sale</button>
          </div>
        </div>
      )}
    </div>
  )
}
function CameraScanButton({ onScan }: { onScan: (code: string) => void }) {
  const [open, setOpen] = useState(false)
  const [error, setError] = useState('')
  const videoRef = useRef<HTMLVideoElement>(null)
  useEffect(() => {
    if (!open) return
    const Detector = (window as unknown as { BarcodeDetector?: new (options?: { formats?: string[] }) => { detect: (source: HTMLVideoElement) => Promise<{ rawValue: string }[]> } }).BarcodeDetector
    if (!Detector) { setError('Camera scanning is not supported in this browser. Use a barcode scanner or search instead.'); return }
    let stream: MediaStream | undefined
    let timer: number | undefined
    const detector = new Detector({ formats: ['ean_13', 'ean_8', 'code_128', 'upc_a', 'upc_e'] })
    navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } } }).then(mediaStream => {
      stream = mediaStream
      if (videoRef.current) { videoRef.current.srcObject = mediaStream; void videoRef.current.play() }
      timer = window.setInterval(() => { if (videoRef.current?.readyState === HTMLMediaElement.HAVE_ENOUGH_DATA) detector.detect(videoRef.current).then(codes => { if (codes[0]?.rawValue) { onScan(codes[0].rawValue); setOpen(false) } }).catch(() => undefined) }, 350)
    }).catch(() => setError('Camera permission was denied or no camera is available.'))
    return () => { if (timer) window.clearInterval(timer); stream?.getTracks().forEach(track => track.stop()) }
  }, [open, onScan])
  return <>{<button type="button" className="camera-button" onClick={() => { setError(''); setOpen(true) }}>Camera</button>}{open && <div className="scanner-modal"><div className="scanner-card"><div className="scanner-heading"><strong>Scan barcode</strong><button type="button" onClick={() => setOpen(false)}>Close</button></div><video ref={videoRef} playsInline muted />{error && <p className="error-message">{error}</p>}<p className="muted">Point the rear camera at a product barcode.</p></div></div>}</>
}

function InventoryPage({ vendorId, storeId }: { vendorId: number; storeId: number }) {
  const [items, setItems] = useState<Awaited<ReturnType<typeof inventoryApi.list>>>([]); const [products, setProducts] = useState<Product[]>([]); const [loading, setLoading] = useState(true); const [productId, setProductId] = useState(0); const [barcode, setBarcode] = useState(''); const [quantity, setQuantity] = useState(''); const [cost, setCost] = useState(''); const [expiry, setExpiry] = useState(''); const [batchId, setBatchId] = useState(0); const [delta, setDelta] = useState(''); const [reason, setReason] = useState(''); const [message, setMessage] = useState('')
  const reload = () => { setLoading(true); return inventoryApi.list(vendorId, storeId).then(setItems).catch(error => setMessage(error.message)).finally(() => setLoading(false)) }
  useEffect(() => { reload(); catalogApi.products(vendorId).then(setProducts).catch(() => undefined) }, [vendorId, storeId])
  async function selectBarcode(code: string) { const normalized = code.trim(); if (!normalized) return; try { const product = await catalogApi.barcode(vendorId, normalized); setProductId(product.id); setBarcode(normalized); setMessage(`Product identified: ${product.name}.`); } catch (error) { setMessage(error instanceof Error ? error.message : 'Product not found for this barcode.') } }
  async function receive(event: FormEvent) { event.preventDefault(); try { await inventoryApi.receive(vendorId, storeId, productId, Number(quantity), Number(cost), expiry || undefined); setMessage('Stock received.'); setQuantity(''); setCost(''); setExpiry(''); reload() } catch (error) { setMessage(error instanceof Error ? error.message : 'Unable to receive stock.') } }
  async function adjust(event: FormEvent) { event.preventDefault(); try { await inventoryApi.adjust(vendorId, storeId, batchId, Number(delta), reason); setMessage('Stock adjusted.'); setDelta(''); setReason(''); reload() } catch (error) { setMessage(error instanceof Error ? error.message : 'Unable to adjust stock.') } }
  return <div><div className="inventory-forms"><form className="compact-form" onSubmit={receive}><h3>Receive stock</h3><div className="inline-form"><input placeholder="Scan barcode or enter code" value={barcode} onChange={event => setBarcode(event.target.value)} onKeyDown={event => { if (event.key === 'Enter') { event.preventDefault(); void selectBarcode(barcode) } }} /><CameraScanButton onScan={code => { void selectBarcode(code) }} /></div><select value={productId} onChange={event => setProductId(Number(event.target.value))} required><option value={0}>Select product</option>{products.map(product => <option key={product.id} value={product.id}>{product.name}</option>)}</select><input type="number" min="0.001" step="0.001" placeholder="Quantity" value={quantity} onChange={event => setQuantity(event.target.value)} required /><input type="number" min="0" step="0.01" placeholder="Cost price" value={cost} onChange={event => setCost(event.target.value)} required /><input type="date" value={expiry} onChange={event => setExpiry(event.target.value)} /><button className="primary-button compact">Receive</button></form><form className="compact-form" onSubmit={adjust}><h3>Adjust stock</h3><select value={batchId} onChange={event => setBatchId(Number(event.target.value))} required><option value={0}>Select batch</option>{items.map(item => <option key={item.id} value={item.id}>{item.productName} - {item.quantityOnHand}</option>)}</select><input type="number" step="0.001" placeholder="+/- quantity" value={delta} onChange={event => setDelta(event.target.value)} required /><input placeholder="Reason" value={reason} onChange={event => setReason(event.target.value)} required /><button className="secondary-button compact">Adjust</button></form></div>{message && <p className="form-message">{message}</p>}<DataTable headers={['Product', 'Barcode', 'Category', 'Unit', 'Stock', 'Price', 'Bulk price', 'Expiry', 'Status']} rows={items.map(item => [item.productName, item.barcodes ?? '-', item.categoryName ?? 'Uncategorized', item.unitType, String(item.quantityOnHand), `PHP ${Number(item.retailPrice).toFixed(2)}`, item.bulkPrice == null ? '-' : `PHP ${Number(item.bulkPrice).toFixed(2)}`, item.expirationDate ?? '-', item.status])} empty="No inventory batches found." loading={loading} /><InventoryMovementHistory vendorId={vendorId} storeId={storeId} /></div>
}

function InventoryMovementHistory({ vendorId, storeId }: { vendorId: number; storeId: number }) {
  const [movements, setMovements] = useState<Awaited<ReturnType<typeof inventoryApi.movements>>>([])
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true); inventoryApi.movements(vendorId, storeId).then(setMovements).catch(error => setMessage(error instanceof Error ? error.message : 'Unable to load movement history.')).finally(() => setLoading(false))
  }, [vendorId, storeId])

  return (
    <section className="movement-history">
      <div className="section-heading"><div><span className="eyebrow">Traceability</span><h3>Inventory movement history</h3></div></div>
      {message && <p className="form-message">{message}</p>}
      <DataTable headers={['Date', 'Product', 'Movement', 'Quantity', 'Reason', 'User']} rows={movements.map(item => [
        new Date(item.createdAt).toLocaleString(),
        item.productName,
        item.movementType,
        String(item.quantityDelta > 0 ? '+' : '') + item.quantityDelta,
        item.reason ?? item.referenceType ?? '—',
        item.createdBy ?? 'System',
      ])} empty="No inventory movements recorded." loading={loading} />
    </section>
  )
}
function CatalogPage({ vendorId }: { vendorId: number }) { const [products, setProducts] = useState<Product[]>([]); const [loading, setLoading] = useState(true); const [categories, setCategories] = useState<Awaited<ReturnType<typeof catalogApi.categories>>>([]); const [search, setSearch] = useState(''); const [categoryId, setCategoryId] = useState<number | undefined>(); useEffect(() => { catalogApi.categories(vendorId).then(setCategories).catch(() => undefined) }, [vendorId]); useEffect(() => { setLoading(true); catalogApi.products(vendorId, search, categoryId).then(setProducts).catch(() => undefined).finally(() => setLoading(false)) }, [vendorId, search, categoryId]); return <div><div className="catalog-filters"><input className="search-input" placeholder="Search catalog" value={search} onChange={event => setSearch(event.target.value)} /><select value={categoryId ?? ''} onChange={event => setCategoryId(event.target.value ? Number(event.target.value) : undefined)}><option value="">All categories</option>{categories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}</select></div><DataTable headers={['Product', 'SKU', 'Category', 'Unit', 'Retail']} rows={products.map(product => [product.name, product.sku, product.categoryName ?? 'Uncategorized', product.unitType, `₱${Number(product.retailPrice).toFixed(2)}`])} empty="No products found." loading={loading} /></div> }
function DebtPage({ vendorId }: { vendorId: number }) { const [accounts, setAccounts] = useState<Awaited<ReturnType<typeof debtApi.accounts>>>([]); const [loading, setLoading] = useState(true); const [search, setSearch] = useState(''); const [accountId, setAccountId] = useState(0); const [amount, setAmount] = useState(''); const [notes, setNotes] = useState(''); const [message, setMessage] = useState(''); const [paid, setPaid] = useState(false); const reload = () => { setLoading(true); return debtApi.accounts(vendorId).then(setAccounts).catch(() => undefined).finally(() => setLoading(false)) }; useEffect(() => { reload() }, [vendorId]); const filtered = accounts.filter(account => account.customerName.toLowerCase().includes(search.toLowerCase()) || (account.phone ?? '').includes(search)); async function pay(event: FormEvent) { event.preventDefault(); try { await debtApi.pay(vendorId, accountId, Number(amount), notes); setMessage('Payment recorded.'); setPaid(true); setAmount(''); setNotes(''); reload() } catch (error) { setMessage(error instanceof Error ? error.message : 'Unable to record payment.') } } return <div><input className="search-input" placeholder="Search customer" value={search} onChange={event => setSearch(event.target.value)} /><form className="debt-payment-form" onSubmit={pay}><select value={accountId} onChange={event => setAccountId(Number(event.target.value))} required><option value={0}>Select account</option>{filtered.filter(account => account.balance > 0).map(account => <option key={account.id} value={account.id}>{account.customerName} · ₱{Number(account.balance).toFixed(2)} balance</option>)}</select><input type="number" min="0.01" step="0.01" placeholder="Payment amount" value={amount} onChange={event => setAmount(event.target.value)} required /><input placeholder="Notes" value={notes} onChange={event => setNotes(event.target.value)} /><button className="primary-button compact">Record payment</button></form>{message && <p className="form-message">{message} {paid && <button className="secondary-button compact" onClick={() => window.print()}>Print payment receipt</button>}</p>}<DataTable headers={['Customer', 'Total credit', 'Paid', 'Balance', 'Status']} rows={filtered.map(account => [account.customerName, `₱${Number(account.totalCredit).toFixed(2)}`, `₱${Number(account.totalPaid).toFixed(2)}`, `₱${Number(account.balance).toFixed(2)}`, account.status])} empty="No debt accounts found." loading={loading} /></div> }
function DeliveryCreateForm({ vendorId, storeId }: { vendorId: number; storeId: number }) {
  const [suppliers, setSuppliers] = useState<Supplier[]>([]); const [products, setProducts] = useState<Product[]>([]); const [message, setMessage] = useState(''); const [form, setForm] = useState({ supplierId: '', productId: '', expectedDate: '', quantity: '', costPrice: '', expirationDate: '', notes: '' })
  useEffect(() => { supplierApi.list(vendorId).then(setSuppliers).catch(error => setMessage(error.message)); catalogApi.products(vendorId).then(setProducts).catch(error => setMessage(error.message)) }, [vendorId])
  async function submit(event: FormEvent) { event.preventDefault(); try { await deliveryApi.create(vendorId, storeId, { supplierId: Number(form.supplierId), expectedDate: form.expectedDate, notes: form.notes, items: [{ productId: Number(form.productId), quantity: Number(form.quantity), costPrice: Number(form.costPrice), expirationDate: form.expirationDate || null }] }); setMessage('Delivery created.'); setForm({ ...form, supplierId: '', productId: '', expectedDate: '', quantity: '', costPrice: '', expirationDate: '', notes: '' }) } catch (error) { setMessage(error instanceof Error ? error.message : 'Unable to create delivery.') } }
  async function addSupplier() { const name = window.prompt('Supplier name'); if (!name) return; try { const supplier = await supplierApi.create(vendorId, name); setSuppliers(items => [...items, supplier]); setForm({ ...form, supplierId: String(supplier.id) }) } catch (error) { setMessage(error instanceof Error ? error.message : 'Unable to create supplier.') } }
  return <form className="compact-form delivery-create-form" onSubmit={submit}><h3>New delivery</h3><div className="form-grid"><select value={form.supplierId} onChange={e => setForm({ ...form, supplierId: e.target.value })} required><option value="">Select supplier</option>{suppliers.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select><button type="button" className="secondary-button compact" onClick={addSupplier}>New supplier</button><select value={form.productId} onChange={e => setForm({ ...form, productId: e.target.value })} required><option value="">Select product</option>{products.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select><input type="date" value={form.expectedDate} onChange={e => setForm({ ...form, expectedDate: e.target.value })} required /><input type="number" min="0.001" step="0.001" placeholder="Quantity" value={form.quantity} onChange={e => setForm({ ...form, quantity: e.target.value })} required /><input type="number" min="0" step="0.01" placeholder="Cost price" value={form.costPrice} onChange={e => setForm({ ...form, costPrice: e.target.value })} required /><input type="date" value={form.expirationDate} onChange={e => setForm({ ...form, expirationDate: e.target.value })} /><input placeholder="Notes" value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })} /></div><button className="primary-button compact">Create delivery</button>{message && <p className="form-message">{message}</p>}</form>
}
function DeliveryPage({ vendorId, storeId }: { vendorId: number; storeId: number }) { const [deliveries, setDeliveries] = useState<Delivery[]>([]); const [loading, setLoading] = useState(true); const [message, setMessage] = useState(''); const reload = () => { setLoading(true); return deliveryApi.list(vendorId, storeId).then(setDeliveries).catch(error => setMessage(error.message)).finally(() => setLoading(false)) }; useEffect(() => { void reload() }, [vendorId, storeId]); async function update(delivery: Delivery) { try { if (delivery.status === 'UPCOMING') await deliveryApi.status(vendorId, storeId, delivery.id, 'IN_TRANSIT'); else if (delivery.status === 'IN_TRANSIT') await deliveryApi.receive(vendorId, storeId, delivery.id); else if (delivery.status === 'RECEIVED') await deliveryApi.status(vendorId, storeId, delivery.id, 'COMPLETED'); setMessage('Delivery updated.'); reload() } catch (error) { setMessage(error instanceof Error ? error.message : 'Unable to update delivery.') } } return <div>{message && <p className="form-message">{message}</p>}<DataTable headers={['Supplier', 'Expected', 'Items', 'Status']} rows={deliveries.map(delivery => [delivery.supplierName, delivery.expectedDate, String(delivery.items.length), delivery.status])} empty="No deliveries found." loading={loading} />{deliveries.filter(delivery => delivery.status !== 'COMPLETED').map(delivery => <div className="store-row" key={delivery.id}><span>#{delivery.id} · {delivery.supplierName} · {delivery.status}</span><button className="secondary-button compact" onClick={() => update(delivery)}>{delivery.status === 'IN_TRANSIT' ? 'Receive stock' : delivery.status === 'RECEIVED' ? 'Complete' : 'Mark in transit'}</button></div>)}</div> }
function ReportsPage({ vendorId, storeId }: { vendorId: number; storeId: number }) {
  const [report, setReport] = useState<Awaited<ReturnType<typeof reportApi.sales>> | null>(null)
  const [low, setLow] = useState<Awaited<ReturnType<typeof reportApi.lowStock>>>([])
  const [expiry, setExpiry] = useState<Awaited<ReturnType<typeof reportApi.expiration>>>([])
  const [vat, setVat] = useState<Awaited<ReturnType<typeof reportApi.vatSummary>> | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    Promise.all([reportApi.sales(vendorId, storeId), reportApi.lowStock(vendorId, storeId), reportApi.expiration(vendorId, storeId), reportApi.vatSummary(vendorId, storeId)])
      .then(([sales, lowStock, expiring, vatSummary]) => { setReport(sales); setLow(lowStock); setExpiry(expiring); setVat(vatSummary) })
      .catch(() => undefined)
      .finally(() => setLoading(false))
  }, [vendorId, storeId])

  return (
    <div>
      <div className="report-grid">
        <div className="metric-card"><span>Sales today</span><strong>₱{Number(report?.total ?? 0).toFixed(2)}</strong><small>{report?.saleCount ?? 0} transactions</small></div>
        <div className="metric-card"><span>Low stock</span><strong>{low.length}</strong><small>products need attention</small></div>
        <div className="metric-card"><span>Near expiration</span><strong>{expiry.length}</strong><small>batches within 30 days</small></div>
        <div className="metric-card"><span>{vat?.vatRegistered ? `VAT (${vat.vatRate}%)` : 'VAT'}</span><strong>₱{Number(vat?.vatAmount ?? 0).toFixed(2)}</strong><small>{vat?.vatRegistered ? 'for the selected period' : 'not enabled'}</small></div>
      </div>
      <div className="alert-panels">
        <section className="alert-panel">
          <h3>Low-stock alerts</h3>
          <DataTable headers={['Product', 'SKU', 'Stock', 'Reorder level']} rows={low.map(item => [item.productName, item.sku, String(item.quantity), String(item.reorderLevel)])} empty="No low-stock products." loading={loading} />
        </section>
        <section className="alert-panel">
          <h3>Expiration alerts</h3>
          <DataTable headers={['Product', 'SKU', 'Quantity', 'Expiration']} rows={expiry.map(item => [item.productName, item.sku, String(item.quantity), item.expirationDate])} empty="No near-expiration or expired batches." loading={loading} />
        </section>
      </div>
    </div>
  )
}
function DataTable({ headers, rows, empty, loading = false }: { headers: string[]; rows: string[][]; empty: string; loading?: boolean }) { return loading ? <p className="loading-state" role="status">Loading…</p> : rows.length ? <div className="data-table-wrap"><table><thead><tr>{headers.map(header => <th key={header}>{header}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={index}>{row.map((cell, cellIndex) => <td key={cellIndex}>{cell}</td>)}</tr>)}</tbody></table></div> : <p className="empty-state">{empty}</p> }
