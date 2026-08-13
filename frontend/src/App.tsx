import { Route, Routes } from 'react-router-dom'
import { RoleSelectPage } from '@/pages/RoleSelectPage'
import { ShipperPage } from '@/pages/ShipperPage'
import { DriverPage } from '@/pages/DriverPage'

export function App() {
  return (
    <Routes>
      <Route path="/" element={<RoleSelectPage />} />
      <Route path="/shipper" element={<ShipperPage />} />
      <Route path="/driver" element={<DriverPage />} />
    </Routes>
  )
}
