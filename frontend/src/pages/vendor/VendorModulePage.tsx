import type { Vendor, Store } from '../../api'
import type { VendorView } from '../../components/vendor/VendorShell'
import { PosPage } from './PosPage'
import { InventoryPage } from './InventoryPage'
import { CatalogPage } from './CatalogPage'
import { DebtPage } from './DebtPage'
import { DeliveryPage } from './DeliveryPage'
import { ReportsPage } from './ReportsPage'
import { StaffPage } from './StaffPage'
import { SettingsPage } from './SettingsPage'

export function VendorModulePage({ view, vendor, storeId }: { view: Exclude<VendorView, 'overview'>; vendor?: Vendor; storeId?: number; stores?: Store[] }) {
  if (view === 'pos') return <PosPage vendor={vendor} storeId={storeId} />
  if (view === 'inventory') return <InventoryPage vendor={vendor} storeId={storeId} />
  if (view === 'catalog') return <CatalogPage vendor={vendor} />
  if (view === 'debt') return <DebtPage vendor={vendor} />
  if (view === 'deliveries') return <DeliveryPage vendor={vendor} storeId={storeId} />
  if (view === 'reports') return <ReportsPage vendor={vendor} storeId={storeId} />
  if (view === 'staff') return <StaffPage vendor={vendor} />
  return <SettingsPage vendor={vendor} />
}
