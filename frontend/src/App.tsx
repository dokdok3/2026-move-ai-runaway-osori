import { Route, Routes } from 'react-router-dom'
import { RoleSelectPage } from '@/pages/RoleSelectPage'
import { ShipperPage } from '@/pages/ShipperPage'
import { DriverPage } from '@/pages/DriverPage'
import { DriverProfilePage } from '@/pages/DriverProfilePage'

export function App() {
  return (
    <Routes>
      <Route path="/" element={<RoleSelectPage />} />
      <Route path="/shipper" element={<ShipperPage />} />
      <Route path="/driver" element={<DriverPage />} />
      <Route path="/driver/profile" element={<DriverProfilePage />} />
    </Routes>
  )
}
