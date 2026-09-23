import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import PageHeader from '../../components/PageHeader'
import {
  getUsers,
  updateUserStatus,
  type AppUser,
} from '../../api/users'

type RoleFilter = 'All' | 'GridOperator' | 'Prosumer'
type StatusFilter = 'All' | 'Active' | 'PendingApproval' | 'Deactivated'

function errorMessage(err: unknown, fallback: string) {
  if (!axios.isAxiosError(err)) return fallback
  if (err.response?.status === 401) {
    return 'Unauthorized (401). Sign out and sign in again so a valid token is sent.'
  }
  if (err.code === 'ERR_NETWORK') {
    return 'Network error. Is the API running on http://localhost:5204?'
  }
  const data = err.response?.data as { message?: string } | undefined
  return data?.message || `${fallback} (HTTP ${err.response?.status ?? '—'})`
}

function formatDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

function statusClass(status: string) {
  if (status === 'Active') return 'bg-success-500/10 text-success-500'
  if (status === 'PendingApproval') return 'bg-warning-500/10 text-warning-500'
  return 'bg-error-500/10 text-error-500'
}

export default function UsersPage() {
  const [users, setUsers] = useState<AppUser[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [toast, setToast] = useState('')
  const [actionId, setActionId] = useState<string | null>(null)
  const [roleFilter, setRoleFilter] = useState<RoleFilter>('All')
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('All')
  const [search, setSearch] = useState('')

  async function loadUsers() {
    setLoading(true)
    setError('')
    try {
      const data = await getUsers()
      setUsers(data)
    } catch (err) {
      setError(errorMessage(err, 'Could not load users.'))
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

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    return users.filter((u) => {
      if (roleFilter !== 'All' && u.userType !== roleFilter) return false
      if (statusFilter !== 'All' && u.status !== statusFilter) return false
      if (!q) return true
      return (
        u.fullName.toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q) ||
        (u.username || '').toLowerCase().includes(q) ||
        (u.nic || '').toLowerCase().includes(q) ||
        u.phoneNumber.toLowerCase().includes(q)
      )
    })
  }, [users, roleFilter, statusFilter, search])

  async function handleStatus(
    user: AppUser,
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
      setUsers((prev) =>
        prev.map((u) => (u.id === user.id ? { ...u, status: newStatus } : u)),
      )
      setToast(
        newStatus === 'Active'
          ? `${user.fullName} set to Active`
          : `${user.fullName} deactivated`,
      )
    } catch (err) {
      setError(errorMessage(err, 'Could not update user status.'))
    } finally {
      setActionId(null)
    }
  }

  const filterBtn = (active: boolean) =>
    [
      'rounded-lg px-3 py-1.5 text-xs font-medium transition',
      active
        ? 'bg-primary text-white shadow-sm'
        : 'bg-surface text-muted hover:text-primary',
    ].join(' ')

  return (
    <div>
      <PageHeader
        title="Users"
        subtitle="Grid Operators and Prosumers across the microgrid."
        action={
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => void loadUsers()}
              disabled={loading}
              className="rounded-xl border border-line bg-panel px-3.5 py-2 text-sm font-medium text-ink transition hover:bg-surface disabled:opacity-60"
            >
              Refresh
            </button>
            <Link
              to="/backoffice/operators"
              className="rounded-xl bg-primary px-3.5 py-2 text-sm font-medium text-white transition hover:opacity-90"
            >
              Create operator
            </Link>
          </div>
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

      <div className="mb-4 flex flex-col gap-3 rounded-2xl border border-line bg-panel p-4 shadow-sm sm:flex-row sm:flex-wrap sm:items-center sm:justify-between">
        <div className="flex flex-wrap gap-2">
          <span className="self-center text-xs font-medium text-muted">Role</span>
          {(['All', 'GridOperator', 'Prosumer'] as RoleFilter[]).map((role) => (
            <button
              key={role}
              type="button"
              className={filterBtn(roleFilter === role)}
              onClick={() => setRoleFilter(role)}
            >
              {role === 'All' ? 'All' : role === 'GridOperator' ? 'Operators' : 'Prosumers'}
            </button>
          ))}
        </div>
        <div className="flex flex-wrap gap-2">
          <span className="self-center text-xs font-medium text-muted">Status</span>
          {(
            ['All', 'Active', 'PendingApproval', 'Deactivated'] as StatusFilter[]
          ).map((status) => (
            <button
              key={status}
              type="button"
              className={filterBtn(statusFilter === status)}
              onClick={() => setStatusFilter(status)}
            >
              {status === 'PendingApproval' ? 'Pending' : status === 'All' ? 'All' : status}
            </button>
          ))}
        </div>
        <input
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search name, email, NIC…"
          className="h-10 w-full rounded-xl border border-line bg-surface px-3 text-sm text-ink outline-none focus:border-primary sm:max-w-xs"
        />
      </div>

      <div className="overflow-hidden rounded-2xl border border-line bg-panel shadow-sm">
        {loading ? (
          <div className="px-6 py-16 text-center text-sm text-muted">
            Loading users…
          </div>
        ) : filtered.length === 0 ? (
          <div className="px-6 py-16 text-center">
            <p className="mb-1 text-sm font-medium text-ink">No users found</p>
            <p className="text-sm text-muted">
              Try another filter, or create a Grid Operator.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="border-b border-line bg-surface/70 text-xs tracking-wide text-muted uppercase">
                <tr>
                  <th className="px-5 py-3.5 font-medium">Name</th>
                  <th className="px-5 py-3.5 font-medium">Role</th>
                  <th className="px-5 py-3.5 font-medium">ID</th>
                  <th className="px-5 py-3.5 font-medium">Contact</th>
                  <th className="px-5 py-3.5 font-medium">Status</th>
                  <th className="px-5 py-3.5 font-medium">Joined</th>
                  <th className="px-5 py-3.5 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((user) => {
                  const busy = actionId === user.id
                  return (
                    <tr
                      key={user.id}
                      className="border-b border-line last:border-b-0"
                    >
                      <td className="px-5 py-4">
                        <p className="font-medium text-ink">{user.fullName}</p>
                      </td>
                      <td className="px-5 py-4 text-muted">{user.userType}</td>
                      <td className="px-5 py-4 text-ink">
                        {user.userType === 'Prosumer'
                          ? user.nic || '—'
                          : user.username || '—'}
                      </td>
                      <td className="px-5 py-4">
                        <p className="text-ink">{user.email}</p>
                        <p className="text-xs text-muted">
                          {user.phoneNumber || '—'}
                        </p>
                      </td>
                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex rounded-lg px-2 py-1 text-xs font-medium ${statusClass(user.status)}`}
                        >
                          {user.status === 'PendingApproval'
                            ? 'Pending'
                            : user.status}
                        </span>
                      </td>
                      <td className="px-5 py-4 text-muted">
                        {formatDate(user.createdAt)}
                      </td>
                      <td className="px-5 py-4">
                        <div className="flex flex-wrap justify-end gap-2">
                          {user.status === 'PendingApproval' ? (
                            <>
                              <button
                                type="button"
                                disabled={busy}
                                onClick={() =>
                                  void handleStatus(user, 'Active')
                                }
                                className="rounded-lg bg-success-500 px-3 py-1.5 text-xs font-medium text-white disabled:opacity-60"
                              >
                                {busy ? '…' : 'Approve'}
                              </button>
                              <button
                                type="button"
                                disabled={busy}
                                onClick={() =>
                                  void handleStatus(user, 'Deactivated')
                                }
                                className="rounded-lg bg-error-500 px-3 py-1.5 text-xs font-medium text-white disabled:opacity-60"
                              >
                                {busy ? '…' : 'Reject'}
                              </button>
                            </>
                          ) : null}
                          {user.status === 'Active' ? (
                            <button
                              type="button"
                              disabled={busy}
                              onClick={() =>
                                void handleStatus(user, 'Deactivated')
                              }
                              className="rounded-lg bg-error-500/10 px-3 py-1.5 text-xs font-medium text-error-500 disabled:opacity-60"
                            >
                              {busy ? '…' : 'Deactivate'}
                            </button>
                          ) : null}
                          {user.status === 'Deactivated' ? (
                            <button
                              type="button"
                              disabled={busy}
                              onClick={() => void handleStatus(user, 'Active')}
                              className="rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary disabled:opacity-60"
                            >
                              {busy ? '…' : 'Reactivate'}
                            </button>
                          ) : null}
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
