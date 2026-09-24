import { useEffect, useMemo, useState } from 'react'
import axios from 'axios'
import PageHeader from '../../components/PageHeader'
import { useAuth } from '../../context/AuthContext'
import { getStations, type Station } from '../../api/stations'
import {
  approveReservation,
  getReservations,
  type Reservation,
} from '../../api/reservations'

type StatusFilter = 'All' | 'Pending' | 'Approved' | 'Completed' | 'Cancelled'

function errorMessage(err: unknown, fallback: string) {
  if (!axios.isAxiosError(err)) return fallback
  if (err.response?.status === 401) {
    return 'Unauthorized (401). Sign out and sign in again so a valid token is sent.'
  }
  if (err.response?.status === 403) {
    return 'Forbidden (403). Grid Operator access is required.'
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
  switch (status) {
    case 'Pending':
      return 'bg-warning-500/10 text-warning-500'
    case 'Approved':
      return 'bg-success-500/10 text-success-500'
    case 'Completed':
      return 'bg-primary/10 text-primary'
    case 'Cancelled':
      return 'bg-error-500/10 text-error-500'
    default:
      return 'bg-surface text-muted'
  }
}

export default function OperatorReservationsPage() {
  const { user } = useAuth()
  const [reservations, setReservations] = useState<Reservation[]>([])
  const [stations, setStations] = useState<Station[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [toast, setToast] = useState('')
  const [actionId, setActionId] = useState<string | null>(null)
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('All')
  const [search, setSearch] = useState('')
  const [lastQr, setLastQr] = useState<{ id: string; code: string } | null>(
    null,
  )

  const myStationIds = useMemo(() => {
    const id = user?.userId
    if (!id) return new Set<string>()
    return new Set(
      stations
        .filter((s) => s.assignedOperatorId === id)
        .map((s) => s.id),
    )
  }, [stations, user?.userId])

  const stationNameById = useMemo(() => {
    const map = new Map<string, string>()
    for (const s of stations) {
      if (myStationIds.has(s.id)) map.set(s.id, s.stationName)
    }
    return map
  }, [stations, myStationIds])

  const mine = useMemo(
    () => reservations.filter((r) => myStationIds.has(r.stationId)),
    [reservations, myStationIds],
  )

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [list, stationList] = await Promise.all([
        getReservations(),
        getStations(),
      ])
      setReservations(
        [...list].sort(
          (a, b) =>
            new Date(b.reservationDateTime).getTime() -
            new Date(a.reservationDateTime).getTime(),
        ),
      )
      setStations(stationList)
    } catch (err) {
      setError(errorMessage(err, 'Could not load reservations.'))
      setReservations([])
      setStations([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(() => setToast(''), 3200)
    return () => window.clearTimeout(timer)
  }, [toast])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    return mine.filter((r) => {
      if (statusFilter !== 'All' && r.status !== statusFilter) return false
      if (!q) return true
      const station = (stationNameById.get(r.stationId) || '').toLowerCase()
      return (
        r.prosumerNic.toLowerCase().includes(q) ||
        r.stationId.toLowerCase().includes(q) ||
        station.includes(q) ||
        (r.qrCode || '').toLowerCase().includes(q)
      )
    })
  }, [mine, statusFilter, search, stationNameById])

  const pendingCount = mine.filter((r) => r.status === 'Pending').length

  async function handleApprove(reservation: Reservation) {
    if (!user?.userId) {
      setError('Missing signed-in user id. Sign in again.')
      return
    }

    setActionId(reservation.id)
    setError('')
    try {
      const result = await approveReservation(reservation.id)
      if (!result.success) {
        setError(result.message || 'Approve failed.')
        return
      }

      setReservations((prev) =>
        prev.map((r) =>
          r.id === reservation.id
            ? {
                ...r,
                status: 'Approved',
                qrCode: result.qrCode || r.qrCode,
                approvedBy: user.userId,
              }
            : r,
        ),
      )
      if (result.qrCode) {
        setLastQr({ id: reservation.id, code: result.qrCode })
      }
      setToast('Reservation approved — QR generated')
    } catch (err) {
      setError(errorMessage(err, 'Could not approve reservation.'))
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
        title="Reservations"
        subtitle="Bookings for your assigned stations — approve to generate QR."
        action={
          <button
            type="button"
            onClick={() => void load()}
            disabled={loading}
            className="rounded-xl border border-line bg-panel px-3.5 py-2 text-sm font-medium text-ink transition hover:bg-surface disabled:opacity-60"
          >
            Refresh
          </button>
        }
      />

      {!loading && !error ? (
        <div className="mb-4 flex flex-wrap gap-3 text-sm">
          <span className="rounded-xl border border-line bg-panel px-3.5 py-2 text-muted">
            My stations{' '}
            <span className="font-semibold text-ink">{myStationIds.size}</span>
          </span>
          <span className="rounded-xl border border-line bg-panel px-3.5 py-2 text-muted">
            Bookings <span className="font-semibold text-ink">{mine.length}</span>
          </span>
          <span className="rounded-xl border border-line bg-panel px-3.5 py-2 text-muted">
            Pending{' '}
            <span className="font-semibold text-ink">{pendingCount}</span>
          </span>
        </div>
      ) : null}

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

      {lastQr ? (
        <div className="mb-4 rounded-2xl border border-primary/20 bg-primary-light px-4 py-3 text-sm">
          <p className="font-medium text-ink">Latest QR code</p>
          <p className="mt-1 break-all font-mono text-xs text-primary">
            {lastQr.code}
          </p>
          <button
            type="button"
            className="mt-2 text-xs font-medium text-primary underline"
            onClick={() => {
              void navigator.clipboard.writeText(lastQr.code)
              setToast('QR copied')
            }}
          >
            Copy QR
          </button>
        </div>
      ) : null}

      <div className="mb-4 flex flex-col gap-3 rounded-2xl border border-line bg-panel p-4 shadow-sm sm:flex-row sm:flex-wrap sm:items-center sm:justify-between">
        <div className="flex flex-wrap gap-2">
          <span className="self-center text-xs font-medium text-muted">
            Status
          </span>
          {(
            [
              'All',
              'Pending',
              'Approved',
              'Completed',
              'Cancelled',
            ] as StatusFilter[]
          ).map((status) => (
            <button
              key={status}
              type="button"
              className={filterBtn(statusFilter === status)}
              onClick={() => setStatusFilter(status)}
            >
              {status}
            </button>
          ))}
        </div>
        <input
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search NIC, station, QR…"
          className="h-10 w-full rounded-xl border border-line bg-surface px-3 text-sm text-ink outline-none focus:border-primary sm:max-w-xs"
        />
      </div>

      <div className="overflow-hidden rounded-2xl border border-line bg-panel shadow-sm">
        {loading ? (
          <div className="px-6 py-16 text-center text-sm text-muted">
            Loading reservations…
          </div>
        ) : !user?.userId ? (
          <div className="px-6 py-16 text-center text-sm text-muted">
            Missing signed-in user id. Sign out and sign in again.
          </div>
        ) : myStationIds.size === 0 ? (
          <div className="px-6 py-16 text-center">
            <p className="mb-1 text-sm font-medium text-ink">
              No stations assigned
            </p>
            <p className="text-sm text-muted">
              Assign yourself to a station in Backoffice first, then bookings
              for those stations will show here.
            </p>
          </div>
        ) : filtered.length === 0 ? (
          <div className="px-6 py-16 text-center">
            <p className="mb-1 text-sm font-medium text-ink">
              No reservations found
            </p>
            <p className="text-sm text-muted">
              No bookings yet for your assigned stations
              {statusFilter !== 'All' ? ` with status “${statusFilter}”` : ''}.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="border-b border-line bg-surface/70 text-xs tracking-wide text-muted uppercase">
                <tr>
                  <th className="px-5 py-3.5 font-medium">Prosumer NIC</th>
                  <th className="px-5 py-3.5 font-medium">Station</th>
                  <th className="px-5 py-3.5 font-medium">Slot time</th>
                  <th className="px-5 py-3.5 font-medium">Status</th>
                  <th className="px-5 py-3.5 font-medium">QR</th>
                  <th className="px-5 py-3.5 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((reservation) => {
                  const busy = actionId === reservation.id
                  const stationName =
                    stationNameById.get(reservation.stationId) ||
                    reservation.stationId

                  return (
                    <tr
                      key={reservation.id}
                      className="border-b border-line last:border-b-0"
                    >
                      <td className="px-5 py-4 font-medium text-ink">
                        {reservation.prosumerNic}
                      </td>
                      <td className="px-5 py-4 text-ink">{stationName}</td>
                      <td className="px-5 py-4 text-muted">
                        {formatDate(reservation.reservationDateTime)}
                      </td>
                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex rounded-lg px-2 py-1 text-xs font-medium ${statusClass(reservation.status)}`}
                        >
                          {reservation.status}
                        </span>
                      </td>
                      <td className="px-5 py-4">
                        {reservation.qrCode ? (
                          <button
                            type="button"
                            title={reservation.qrCode}
                            className="max-w-[9rem] truncate font-mono text-xs text-primary hover:underline"
                            onClick={() => {
                              setLastQr({
                                id: reservation.id,
                                code: reservation.qrCode!,
                              })
                              void navigator.clipboard.writeText(
                                reservation.qrCode!,
                              )
                              setToast('QR copied')
                            }}
                          >
                            {reservation.qrCode.slice(0, 8)}…
                          </button>
                        ) : (
                          <span className="text-xs text-muted">—</span>
                        )}
                      </td>
                      <td className="px-5 py-4">
                        <div className="flex justify-end">
                          {reservation.status === 'Pending' ? (
                            <button
                              type="button"
                              disabled={busy}
                              onClick={() => void handleApprove(reservation)}
                              className="rounded-lg bg-success-500 px-3 py-1.5 text-xs font-medium text-white disabled:opacity-60"
                            >
                              {busy ? '…' : 'Approve'}
                            </button>
                          ) : (
                            <span className="text-xs text-muted">—</span>
                          )}
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
