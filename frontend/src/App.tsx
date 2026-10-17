import { Navigate, Route, Routes } from 'react-router'
import { ProtectedRoute } from './components/ProtectedRoute'
import { ActivateAccountPage } from './pages/ActivateAccountPage'
import { LoginPage } from './pages/LoginPage'
import { CompanyDashboardPage } from './pages/company/CompanyDashboardPage'
import { CompaniesPage } from './pages/superadmin/CompaniesPage'
import { CreateCompanyPage } from './pages/superadmin/CreateCompanyPage'
import { EditCompanyPage } from './pages/superadmin/EditCompanyPage'
import { SuperadminDashboardPage } from './pages/superadmin/SuperadminDashboardPage'
import { SuperadminLayout } from './pages/superadmin/SuperadminLayout'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/activate" element={<ActivateAccountPage />} />

      <Route element={<ProtectedRoute allowedRole="SUPERADMIN" />}>
        <Route element={<SuperadminLayout />}>
          <Route path="/superadmin/dashboard" element={<SuperadminDashboardPage />} />
          <Route path="/superadmin/companies" element={<CompaniesPage />} />
          <Route path="/superadmin/companies/new" element={<CreateCompanyPage />} />
          <Route path="/superadmin/companies/:id/edit" element={<EditCompanyPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute allowedRole="COMPANY_ADMIN" />}>
        <Route path="/company/dashboard" element={<CompanyDashboardPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}

export default App
