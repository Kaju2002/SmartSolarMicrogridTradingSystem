import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import RoleRoute from './components/RoleRoute'
import Login from './pages/Login'
import RoleHomeRedirect from './pages/RoleHomeRedirect'
import OperatorHome from './pages/OperatorHome'
import Unauthorized from './pages/Unauthorized'
import BackofficeLayout from './layouts/BackofficeLayout'
import DashboardPage from './pages/backoffice/DashboardPage'
import PendingApprovalsPage from './pages/backoffice/PendingApprovalsPage'
import StationsPage from './pages/backoffice/StationsPage'
import ReservationsPage from './pages/backoffice/ReservationsPage'
import CreateOperatorPage from './pages/backoffice/CreateOperatorPage'
import ProfilePage from './pages/backoffice/ProfilePage'
import { ROLES } from './utils/roles'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<RoleHomeRedirect />} />
            <Route path="/unauthorized" element={<Unauthorized />} />

            <Route element={<RoleRoute allowed={[ROLES.Backoffice]} />}>
              <Route path="/backoffice" element={<BackofficeLayout />}>
                <Route index element={<DashboardPage />} />
                <Route path="pending" element={<PendingApprovalsPage />} />
                <Route path="stations" element={<StationsPage />} />
                <Route path="reservations" element={<ReservationsPage />} />
                <Route path="operators" element={<CreateOperatorPage />} />
                <Route path="profile" element={<ProfilePage />} />
              </Route>
            </Route>

            <Route element={<RoleRoute allowed={[ROLES.GridOperator]} />}>
              <Route path="/operator" element={<OperatorHome />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
