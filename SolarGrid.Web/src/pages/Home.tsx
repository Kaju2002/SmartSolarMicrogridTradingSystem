import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Home() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-brand-950 px-6 text-white">
      <h1 className="mb-2 text-3xl font-semibold">Welcome</h1>
      <p className="mb-1 text-sm text-gray-300">
        {user?.fullName || 'User'}
        {user?.userType ? ` · ${user.userType}` : ''}
      </p>
      <p className="mb-8 text-xs text-gray-500">
        Protected home — role dashboards come next.
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
