import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { matchesRole } from '../utils/roles'

type RoleRouteProps = {
  allowed: string[]
}

export default function RoleRoute({ allowed }: RoleRouteProps) {
  const { user } = useAuth()

  if (!matchesRole(user?.userType, allowed)) {
    return <Navigate to="/unauthorized" replace />
  }

  return <Outlet />
}
