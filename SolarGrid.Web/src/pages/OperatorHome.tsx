import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function OperatorHome() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-brand-950 px-6 text-white">
      <p className="mb-2 text-xs uppercase tracking-widest text-brand-500">
        Grid Operator
      </p>
      <h1 className="mb-2 text-3xl font-semibold">Welcome</h1>
      <p className="mb-8 text-sm text-gray-300">
        {user?.fullName || 'User'}
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
