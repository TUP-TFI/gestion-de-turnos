import { Navigate, Route, Routes } from 'react-router'
import { ProtectedRoute } from './components/ProtectedRoute'
import { ActivateAccountPage } from './pages/ActivateAccountPage'
import { LoginPage } from './pages/LoginPage'
import { BrandingPage } from './pages/company/BrandingPage'
import { BusinessHoursPage } from './pages/company/BusinessHoursPage'
import { CompanyDashboardPage } from './pages/company/CompanyDashboardPage'
import { CompanyLayout } from './pages/company/CompanyLayout'
import { CreateScheduleExceptionPage } from './pages/company/CreateScheduleExceptionPage'
import { CreateServicePage } from './pages/company/CreateServicePage'
import { EditScheduleExceptionPage } from './pages/company/EditScheduleExceptionPage'
import { EditServicePage } from './pages/company/EditServicePage'
import { ScheduleExceptionsPage } from './pages/company/ScheduleExceptionsPage'
import { ServicesPage } from './pages/company/ServicesPage'
import { ClientLoginPage } from './pages/public/ClientLoginPage'
import { ClientRegisterPage } from './pages/public/ClientRegisterPage'
import { CompaniesPage } from './pages/superadmin/CompaniesPage'
import { CreateCompanyPage } from './pages/superadmin/CreateCompanyPage'
import { EditCompanyPage } from './pages/superadmin/EditCompanyPage'
import { SuperadminDashboardPage } from './pages/superadmin/SuperadminDashboardPage'
import { SettingsPage } from './pages/superadmin/SettingsPage'
import { SuperadminLayout } from './pages/superadmin/SuperadminLayout'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/activate" element={<ActivateAccountPage />} />

      {/* Página pública de cada empresa, sin sesión del panel. Las rutas fijas (/login, /activate) tienen prioridad. */}
      <Route path="/:slug/login" element={<ClientLoginPage />} />
      <Route path="/:slug/register" element={<ClientRegisterPage />} />

      <Route element={<ProtectedRoute allowedRole="SUPERADMIN" />}>
        <Route element={<SuperadminLayout />}>
          <Route path="/superadmin/dashboard" element={<SuperadminDashboardPage />} />
          <Route path="/superadmin/companies" element={<CompaniesPage />} />
          <Route path="/superadmin/companies/new" element={<CreateCompanyPage />} />
          <Route path="/superadmin/companies/:id/edit" element={<EditCompanyPage />} />
          <Route path="/superadmin/settings" element={<SettingsPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute allowedRole="COMPANY_ADMIN" />}>
        <Route element={<CompanyLayout />}>
          <Route path="/company/dashboard" element={<CompanyDashboardPage />} />
          <Route path="/company/services" element={<ServicesPage />} />
          <Route path="/company/services/new" element={<CreateServicePage />} />
          <Route path="/company/services/:id/edit" element={<EditServicePage />} />
          <Route path="/company/business-hours" element={<BusinessHoursPage />} />
          <Route path="/company/schedule-exceptions" element={<ScheduleExceptionsPage />} />
          <Route
            path="/company/schedule-exceptions/new"
            element={<CreateScheduleExceptionPage />}
          />
          <Route
            path="/company/schedule-exceptions/:id/edit"
            element={<EditScheduleExceptionPage />}
          />
          <Route path="/company/branding" element={<BrandingPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}

export default App
