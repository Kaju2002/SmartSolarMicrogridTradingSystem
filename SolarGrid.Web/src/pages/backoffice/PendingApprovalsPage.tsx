import { useEffect, useState } from 'react'
import axios from 'axios'
import PageHeader from '../../components/PageHeader'
import {
  getPendingUsers,
  updateUserStatus,
  type PendingUser,
} from '../../api/users'

function formatDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

function errorMessage(err: unknown, fallback: string) {
  if (!axios.isAxiosError(err)) return fallback
  if (err.response?.status === 401) {
    return 'Unauthorized (401). Sign out and sign in again so a valid token is sent.'
  }
  if (err.response?.status === 403) {
    return 'Forbidden (403). Your account cannot access pending users.'
  }
  if (err.code === 'ERR_NETWORK') {
    return 'Network error. Is the API running on http://localhost:5204?'
  }
  const apiMsg = (err.response?.data as { message?: string } | undefined)?.message
  return apiMsg || `${fallback} (HTTP ${err.response?.status ?? '—'})`
}

export default function PendingApprovalsPage() {
  const [users, setUsers] = useState<PendingUser[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionId, setActionId] = useState<string | null>(null)
  const [toast, setToast] = useState('')

  async function loadUsers() {
    setLoading(true)
    setError('')
    try {
      const data = await getPendingUsers()
      setUsers(data)
    } catch (err) {
      setError(
        errorMessage(
          err,
          'Could not load pending users. Check that the API is running.',
        ),
      )
      setUsers([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadUsers()
  }, [])

  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(() => setToast(''), 2800)
    return () => window.clearTimeout(timer)
  }, [toast])

  async function handleStatus(
    user: PendingUser,
    newStatus: 'Active' | 'Deactivated',
  ) {
    setActionId(user.id)
    setError('')
    try {
      const result = await updateUserStatus({
        userId: user.id,
        newStatus,
      })

      if (!result.success) {
        setError(result.message || 'Status update failed.')
        return
      }

      setUsers((prev) => prev.filter((u) => u.id !== user.id))
      setToast(
        newStatus === 'Active'
          ? `${user.fullName} approved`
          : `${user.fullName} rejected`,
      )
    } catch (err) {
      setError(errorMessage(err, 'Could not update user status. Try again.'))
    } finally {
      setActionId(null)
    }
  }

  return (
    <div>
      <PageHeader
        title="Pending Approvals"
        subtitle="Review Prosumer registrations waiting for Backoffice approval."
        action={
          <button
            type="button"
            onClick={() => void loadUsers()}
            disabled={loading}
            className="rounded-xl border border-line bg-panel px-3.5 py-2 text-sm font-medium text-ink transition hover:bg-surface disabled:opacity-60"
          >
            Refresh
          </button>
        }
      />

      {toast ? (
        <div className="mb-4 rounded-xl border border-success-500/20 bg-success-500/10 px-4 py-2.5 text-sm text-success-500">
          {toast}
        </div>
      ) : null}

      {error ? (
        <div className="mb-4 rounded-xl border border-error-500/20 bg-error-500/10 px-4 py-2.5 text-sm text-error-500">
          {error}
        </div>
      ) : null}

      <div className="overflow-hidden rounded-2xl border border-line bg-panel shadow-sm">
        {loading ? (
          <div className="px-6 py-16 text-center text-sm text-muted">
            Loading pending users…
          </div>
        ) : users.length === 0 ? (
          <div className="px-6 py-16 text-center">
            <p className="mb-1 text-sm font-medium text-ink">
              No pending approvals
            </p>
            <p className="text-sm text-muted">
              New Prosumer registrations will appear here for review.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="border-b border-line bg-surface/70 text-xs tracking-wide text-muted uppercase">
                <tr>
                  <th className="px-5 py-3.5 font-medium">Name</th>
                  <th className="px-5 py-3.5 font-medium">NIC</th>
                  <th className="px-5 py-3.5 font-medium">Contact</th>
                  <th className="px-5 py-3.5 font-medium">Registered</th>
                  <th className="px-5 py-3.5 text-right font-medium">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => {
                  const busy = actionId === user.id
                  return (
                    <tr
                      key={user.id}
                      className="border-b border-line last:border-b-0"
                    >
                      <td className="px-5 py-4">
                        <p className="font-medium text-ink">{user.fullName}</p>
                        <p className="text-xs text-muted">{user.userType}</p>
                      </td>
                      <td className="px-5 py-4 text-ink">
                        {user.nic || '—'}
                      </td>
                      <td className="px-5 py-4">
                        <p className="text-ink">{user.email}</p>
                        <p className="text-xs text-muted">
                          {user.phoneNumber || '—'}
                        </p>
                      </td>
                      <td className="px-5 py-4 text-muted">
                        {formatDate(user.createdAt)}
                      </td>
                      <td className="px-5 py-4">
                        <div className="flex flex-wrap justify-end gap-2">
                          <button
                            type="button"
                            disabled={busy}
                            onClick={() => void handleStatus(user, 'Active')}
                            className="rounded-lg bg-success-500 px-3 py-1.5 text-xs font-medium text-white transition hover:opacity-90 disabled:opacity-60"
                          >
                            {busy ? '…' : 'Approve'}
                          </button>
                          <button
                            type="button"
                            disabled={busy}
                            onClick={() =>
                              void handleStatus(user, 'Deactivated')
                            }
                            className="rounded-lg bg-error-500 px-3 py-1.5 text-xs font-medium text-white transition hover:opacity-90 disabled:opacity-60"
                          >
                            {busy ? '…' : 'Reject'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
