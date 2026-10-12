import { Navigate, Route, Routes } from 'react-router'
import { ProtectedRoute } from './components/ProtectedRoute'
import { LoginPage } from './pages/LoginPage'
import { CompanyDashboardPage } from './pages/company/CompanyDashboardPage'
import { SuperadminDashboardPage } from './pages/superadmin/SuperadminDashboardPage'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<ProtectedRoute allowedRole="SUPERADMIN" />}>
        <Route path="/superadmin/dashboard" element={<SuperadminDashboardPage />} />
      </Route>

      <Route element={<ProtectedRoute allowedRole="COMPANY_ADMIN" />}>
        <Route path="/company/dashboard" element={<CompanyDashboardPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}

export default App
