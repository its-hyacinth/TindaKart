export type Vendor = { id: number; name: string; status: string }
export type Store = { id: number; vendorId: number; name: string; code: string; address?: string; status: string }
export type CurrentUser = { username: string; roles: string[]; vendors: Vendor[]; stores: Store[] }
export type Category = { id: number; vendorId: number; name: string; active: boolean }
export type Product = { id: number; vendorId: number; categoryId?: number; categoryName?: string; name: string; sku: string; unitType: string; retailPrice: number; bulkPrice?: number; bulkThreshold?: number; barcodes: string[] }
export type InventoryBatch = { id: number; productId: number; productName: string; sku: string; categoryName?: string; unitType: string; barcodes?: string; batchReference?: string; quantityOnHand: number; retailPrice: number; bulkPrice?: number; expirationDate?: string; status: string }
export type Debt = { id: number; customerId: number; customerName: string; phone?: string; status: string; totalCredit: number; totalPaid: number; balance: number }
export type Delivery = { id: number; supplierId: number; supplierName: string; expectedDate: string; status: string; notes?: string; items: { id: number; productId: number; productName: string; quantityOrdered: number; quantityReceived: number; costPrice: number; expirationDate?: string }[] }
export type Staff = { id: number; username: string; displayName: string; enabled: boolean; roles: string[]; stores: { id: number; name: string; code: string }[] }
export type Package = { id: number; name: string; description?: string; monthlyPrice: number; annualPrice: number; active: boolean; features: Record<string, boolean>; limits: Record<string, number> }
export type BusinessSettings = { businessName?: string; businessAddress?: string; tin?: string; vatRegistered: boolean; vatRate: number; nearExpirationDays: number; receiptFooter?: string }
export type Supplier = { id: number; name: string; phone?: string; address?: string }

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
  login: async (username: string, password: string) => {
    const csrf = await request<{ token: string; headerName: string }>('/api/auth/csrf')
    return request<CurrentUser>('/api/auth/login', {
      method: 'POST', headers: { [csrf.headerName]: csrf.token }, body: JSON.stringify({ username, password }),
    })
  },
  me: () => request<CurrentUser>('/api/auth/me'),
  logout: async () => {
    const csrf = await request<{ token: string; headerName: string }>('/api/auth/csrf')
    return request<void>('/api/auth/logout', { method: 'POST', headers: { [csrf.headerName]: csrf.token } })
  },
}

export const tenantApi = {
  vendors: () => request<Vendor[]>('/api/super-admin/vendors'),
  createVendor: (name: string) => request<Vendor>('/api/super-admin/vendors', {
    method: 'POST', body: JSON.stringify({ name }),
  }),
  stores: (vendorId: number) => request<Store[]>(`/api/vendors/${vendorId}/stores`),
  createStore: (vendorId: number, name: string, code: string, address: string) => request<Store>(`/api/vendors/${vendorId}/stores`, {
    method: 'POST', body: JSON.stringify({ name, code, address }),
  }),
}

export const staffApi = {
  list: (vendorId: number) => request<Staff[]>(`/api/vendors/${vendorId}/staff`),
  create: (vendorId: number, payload: { username: string; password: string; displayName: string; role: string; storeIds: number[] }) => request<Staff>(`/api/vendors/${vendorId}/staff`, { method: 'POST', body: JSON.stringify(payload) }),
  status: (vendorId: number, userId: number, status: string) => request<Staff>(`/api/vendors/${vendorId}/staff/${userId}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
}

export const packageApi = {
  list: () => request<Package[]>('/api/super-admin/packages'),
  create: (payload: Record<string, unknown>) => request<Package>('/api/super-admin/packages', { method: 'POST', body: JSON.stringify(payload) }),
  update: (id: number, payload: Record<string, unknown>) => request<Package>(`/api/super-admin/packages/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  available: (vendorId: number) => request<Package[]>(`/api/vendors/${vendorId}/packages`),
  subscription: (vendorId: number) => request<{ packageName: string; status: string }>(`/api/vendors/${vendorId}/subscription`),
  select: (vendorId: number, packageId: number) => request<unknown>(`/api/vendors/${vendorId}/subscription`, { method: 'PUT', body: JSON.stringify({ packageId }) }),
  entitlements: (vendorId: number) => request<{ features: Record<string, boolean>; limits: Record<string, number> }>(`/api/vendors/${vendorId}/entitlements`),
}

export const billingApi = {
  history: (vendorId: number) => request<{ id: number; checkoutUrl: string; amount: number; currency: string; status: string; createdAt: string }[]>(`/api/vendors/${vendorId}/billing/checkout`),
  checkout: (vendorId: number, billingCycle: string) => request<{ checkoutUrl: string }>(`/api/vendors/${vendorId}/billing/checkout`, { method: 'POST', body: JSON.stringify({ billingCycle }) }),
}

export const settingsApi = {
  get: (vendorId: number) => request<BusinessSettings>(`/api/vendors/${vendorId}/settings`),
  update: (vendorId: number, payload: BusinessSettings) => request<BusinessSettings>(`/api/vendors/${vendorId}/settings`, { method: 'PUT', body: JSON.stringify(payload) }),
}

export const catalogApi = {
  categories: (vendorId: number) => request<Category[]>(`/api/vendors/${vendorId}/categories`),
  createCategory: (vendorId: number, name: string) => request<Category>(`/api/vendors/${vendorId}/categories`, { method: 'POST', body: JSON.stringify({ name }) }),
  products: (vendorId: number, search = '', categoryId?: number) => request<Product[]>(`/api/vendors/${vendorId}/products?search=${encodeURIComponent(search)}${categoryId ? `&categoryId=${categoryId}` : ''}`),
  barcode: (vendorId: number, barcode: string) => request<Product>(`/api/vendors/${vendorId}/products/barcode/${encodeURIComponent(barcode)}`),
  createProduct: (vendorId: number, product: Record<string, unknown>) => request<Product>(`/api/vendors/${vendorId}/products`, { method: 'POST', body: JSON.stringify(product) }),
}

export const inventoryApi = {
  list: (vendorId: number, storeId: number, status?: string) => request<InventoryBatch[]>(`/api/vendors/${vendorId}/stores/${storeId}/inventory${status ? `?status=${status}` : ''}`),
  receive: (vendorId: number, storeId: number, productId: number, quantity: number, costPrice: number, expirationDate?: string) => request<InventoryBatch>(`/api/vendors/${vendorId}/stores/${storeId}/inventory/receive`, { method: 'POST', body: JSON.stringify({ productId, quantity, costPrice, expirationDate }) }),
  adjust: (vendorId: number, storeId: number, batchId: number, quantityDelta: number, reason: string) => request<InventoryBatch>(`/api/vendors/${vendorId}/stores/${storeId}/inventory/adjust`, { method: 'POST', body: JSON.stringify({ batchId, quantityDelta, reason }) }),
}

export const posApi = {
  sale: (vendorId: number, storeId: number, items: { productId: number; quantity: number }[], amountTendered: number, discountAmount = 0, paymentMethod = 'CASH') => request<{ id: number; receiptNumber: string; subtotal: number; discountAmount: number; vatAmount: number; totalAmount: number; changeAmount: number; paymentMethod: string }>(`/api/vendors/${vendorId}/stores/${storeId}/sales`, {
    method: 'POST', body: JSON.stringify({ items, paymentMethod, amountTendered, discountAmount }),
  }),
}

export const debtApi = {
  accounts: (vendorId: number) => request<Debt[]>(`/api/vendors/${vendorId}/debt/accounts`),
  pay: (vendorId: number, accountId: number, amount: number, notes: string) => request<{ id: number; amount: number; paymentMethod: string; paidAt: string }>(`/api/vendors/${vendorId}/debt/accounts/${accountId}/payments`, { method: 'POST', body: JSON.stringify({ amount, paymentMethod: 'CASH', notes }) }),
}

export const deliveryApi = {
  list: (vendorId: number, storeId: number) => request<Delivery[]>(`/api/vendors/${vendorId}/stores/${storeId}/deliveries`),
  create: (vendorId: number, storeId: number, payload: Record<string, unknown>) => request<Delivery>(`/api/vendors/${vendorId}/stores/${storeId}/deliveries`, { method: 'POST', body: JSON.stringify(payload) }),
  status: (vendorId: number, storeId: number, deliveryId: number, status: string) => request<Delivery>(`/api/vendors/${vendorId}/stores/${storeId}/deliveries/${deliveryId}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  receive: (vendorId: number, storeId: number, deliveryId: number) => request<Delivery>(`/api/vendors/${vendorId}/stores/${storeId}/deliveries/${deliveryId}/receive`, { method: 'PUT' }),
}

export const supplierApi = {
  list: (vendorId: number) => request<Supplier[]>(`/api/vendors/${vendorId}/suppliers`),
  create: (vendorId: number, name: string, phone = '', address = '') => request<Supplier>(`/api/vendors/${vendorId}/suppliers`, { method: 'POST', body: JSON.stringify({ name, phone, address }) }),
}

export const reportApi = {
  sales: (vendorId: number, storeId: number) => request<{ saleCount: number; subtotal: number; discount: number; vat: number; total: number }>(`/api/vendors/${vendorId}/stores/${storeId}/reports/sales`),
  lowStock: (vendorId: number, storeId: number) => request<{ productId: number; productName: string; quantity: number; reorderLevel: number }[]>(`/api/vendors/${vendorId}/stores/${storeId}/reports/low-stock`),
  expiration: (vendorId: number, storeId: number) => request<{ batchId: number; productName: string; quantity: number; expirationDate: string }[]>(`/api/vendors/${vendorId}/stores/${storeId}/reports/expiration`),
}
