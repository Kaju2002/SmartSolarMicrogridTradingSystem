import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Unauthorized() {
  const navigate = useNavigate()
  const { logout, user } = useAuth()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-brand-950 px-6 text-center text-white">
      <h1 className="mb-2 text-2xl font-semibold">Access not allowed</h1>
      <p className="mb-8 max-w-sm text-sm text-gray-400">
        {user?.userType === 'Prosumer'
          ? 'Prosumer accounts use the mobile app. This web portal is for Backoffice and Grid Operator only.'
          : 'Your account cannot access this area of the web portal.'}
      </p>
      <button
        type="button"
        onClick={handleLogout}
        className="rounded-lg bg-brand-500 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-brand-600"
      >
        Sign Out
      </button>
    </div>
  )
}
