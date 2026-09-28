import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import RoleRoute from './components/RoleRoute'
import Login from './pages/Login'
import RoleHomeRedirect from './pages/RoleHomeRedirect'
import Unauthorized from './pages/Unauthorized'
import BackofficeLayout from './layouts/BackofficeLayout'
import OperatorLayout from './layouts/OperatorLayout'
import DashboardPage from './pages/backoffice/DashboardPage'
import PendingApprovalsPage from './pages/backoffice/PendingApprovalsPage'
import UsersPage from './pages/backoffice/UsersPage'
import StationsPage from './pages/backoffice/StationsPage'
import CreateStationPage from './pages/backoffice/CreateStationPage'
import EditStationPage from './pages/backoffice/EditStationPage'
import ReservationsPage from './pages/backoffice/ReservationsPage'
import CreateOperatorPage from './pages/backoffice/CreateOperatorPage'
import ProfilePage from './pages/backoffice/ProfilePage'
import OperatorDashboardPage from './pages/operator/DashboardPage'
import OperatorStationsPage from './pages/operator/StationsPage'
import OperatorReservationsPage from './pages/operator/ReservationsPage'
import OperatorVerifyQrPage from './pages/operator/VerifyQrPage'
import OperatorProfilePage from './pages/operator/ProfilePage'
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
                <Route path="users" element={<UsersPage />} />
                <Route path="stations/new" element={<CreateStationPage />} />
                <Route path="stations/:id/edit" element={<EditStationPage />} />
                <Route path="stations" element={<StationsPage />} />
                <Route path="reservations" element={<ReservationsPage />} />
                <Route path="operators" element={<CreateOperatorPage />} />
                <Route path="profile" element={<ProfilePage />} />
              </Route>
            </Route>

            <Route element={<RoleRoute allowed={[ROLES.GridOperator]} />}>
              <Route path="/operator" element={<OperatorLayout />}>
                <Route index element={<OperatorDashboardPage />} />
                <Route path="stations" element={<OperatorStationsPage />} />
                <Route path="reservations" element={<OperatorReservationsPage />} />
                <Route path="verify" element={<OperatorVerifyQrPage />} />
                <Route path="profile" element={<OperatorProfilePage />} />
              </Route>
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
