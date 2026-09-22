import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { homePathForRole } from '../utils/roles'

/** Sends authenticated users to their role home. */
export default function RoleHomeRedirect() {
  const { user } = useAuth()
  return <Navigate to={homePathForRole(user?.userType)} replace />
}
