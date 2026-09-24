import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import { useAuth } from '../../context/AuthContext'
import { getStations, type Station } from '../../api/stations'
import { getReservations, type Reservation } from '../../api/reservations'

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

function isSameDay(a: Date, b: Date) {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  )
}

function formatWhen(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function Skeleton({ className }: { className?: string }) {
  return <div className={`animate-pulse rounded-xl bg-surface ${className || ''}`} />
}

function KpiIcon({ name }: { name: string }) {
  const common = {
    width: 22,
    height: 22,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.8,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
  }
  switch (name) {
    case 'station':
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
        </svg>
      )
    case 'pending':
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7v5l3 2" />
        </svg>
      )
    case 'verify':
      return (
        <svg {...common}>
          <rect x="3" y="3" width="7" height="7" rx="1" />
          <rect x="14" y="3" width="7" height="7" rx="1" />
          <rect x="3" y="14" width="7" height="7" rx="1" />
          <path d="M14 14h3v3M20 14v6M14 20h3" />
        </svg>
      )
    default:
      return (
        <svg {...common}>
          <path d="M20 6 9 17l-5-5" />
        </svg>
      )
  }
}

function KpiCard({
  title,
  value,
  hint,
  to,
  color,
  icon,
  loading,
}: {
  title: string
  value: string
  hint: string
  to: string
  color: string
  icon: string
  loading?: boolean
}) {
  if (loading) {
    return (
      <div className="rounded-2xl border border-line bg-panel p-5 shadow-sm">
        <Skeleton className="mb-4 h-10 w-10 rounded-xl" />
        <Skeleton className="mb-2 h-4 w-28" />
        <Skeleton className="mb-3 h-8 w-16" />
        <Skeleton className="h-3 w-36" />
      </div>
    )
  }

  return (
    <Link
      to={to}
      className="group rounded-2xl border border-line bg-panel p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-md"
    >
      <span
        className="mb-4 inline-flex size-11 items-center justify-center rounded-xl"
        style={{ backgroundColor: `${color}18`, color }}
      >
        <KpiIcon name={icon} />
      </span>
      <p className="text-sm font-medium text-muted">{title}</p>
      <p className="mt-1 text-3xl font-semibold tracking-tight text-ink transition group-hover:text-primary">
        {value}
      </p>
      <p className="mt-1 text-xs text-muted">{hint}</p>
    </Link>
  )
}

type AttentionItem = {
  id: string
  kind: 'pending' | 'verify'
  title: string
  meta: string
  to: string
}

export default function OperatorDashboardPage() {
  const { user } = useAuth()
  const [stations, setStations] = useState<Station[]>([])
  const [reservations, setReservations] = useState<Reservation[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [stationList, reservationList] = await Promise.all([
        getStations(),
        getReservations(),
      ])
      setStations(stationList)
      setReservations(reservationList)
    } catch (err) {
      setError(errorMessage(err, 'Could not load dashboard.'))
      setStations([])
      setReservations([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  const myStations = useMemo(() => {
    const id = user?.userId
    if (!id) return []
    return stations.filter((s) => s.assignedOperatorId === id)
  }, [stations, user?.userId])

  const myStationIds = useMemo(
    () => new Set(myStations.map((s) => s.id)),
    [myStations],
  )

  const stationNameById = useMemo(() => {
    const map = new Map<string, string>()
    for (const s of myStations) map.set(s.id, s.stationName)
    return map
  }, [myStations])

  const mine = useMemo(
    () => reservations.filter((r) => myStationIds.has(r.stationId)),
    [reservations, myStationIds],
  )

  const stats = useMemo(() => {
    const today = new Date()
    const pending = mine.filter((r) => r.status === 'Pending').length
    const ready = mine.filter(
      (r) => r.status === 'Approved' && !!r.qrCode,
    ).length
    const completedToday = mine.filter((r) => {
      if (r.status !== 'Completed') return false
      const when = new Date(r.lastModifiedAt || r.createdAt)
      return isSameDay(when, today)
    }).length
    const activeStations = myStations.filter((s) => s.status === 'Active').length

    return {
      stations: myStations.length,
      activeStations,
      pending,
      ready,
      completedToday,
    }
  }, [mine, myStations])

  const attention = useMemo(() => {
    const items: AttentionItem[] = []

    const pendingRows = mine
      .filter((r) => r.status === 'Pending')
      .sort(
        (a, b) =>
          new Date(a.reservationDateTime).getTime() -
          new Date(b.reservationDateTime).getTime(),
      )
      .slice(0, 4)

    for (const r of pendingRows) {
      items.push({
        id: `p-${r.id}`,
        kind: 'pending',
        title: `${r.prosumerNic} · pending approve`,
        meta: `${stationNameById.get(r.stationId) || 'Station'} · ${formatWhen(r.reservationDateTime)}`,
        to: '/operator/reservations',
      })
    }

    const readyRows = mine
      .filter((r) => r.status === 'Approved' && r.qrCode)
      .sort(
        (a, b) =>
          new Date(a.reservationDateTime).getTime() -
          new Date(b.reservationDateTime).getTime(),
      )
      .slice(0, 4)

    for (const r of readyRows) {
      items.push({
        id: `v-${r.id}`,
        kind: 'verify',
        title: `${r.prosumerNic} · ready to verify`,
        meta: `${stationNameById.get(r.stationId) || 'Station'} · ${formatWhen(r.reservationDateTime)}`,
        to: '/operator/verify',
      })
    }

    return items.slice(0, 6)
  }, [mine, stationNameById])

  const recentCompleted = useMemo(
    () =>
      mine
        .filter((r) => r.status === 'Completed')
        .sort(
          (a, b) =>
            new Date(b.lastModifiedAt || b.createdAt).getTime() -
            new Date(a.lastModifiedAt || a.createdAt).getTime(),
        )
        .slice(0, 5),
    [mine],
  )

  const hour = new Date().getHours()
  const greeting =
    hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening'

  return (
    <div className="space-y-5">
      <section className="relative overflow-hidden rounded-2xl border border-line bg-panel p-6 shadow-sm">
        <div
          className="pointer-events-none absolute inset-0 opacity-90"
          style={{
            background:
              'radial-gradient(ellipse 80% 60% at 100% 0%, rgba(77,124,255,0.14), transparent 55%), radial-gradient(ellipse 50% 40% at 0% 100%, rgba(34,197,94,0.08), transparent 50%)',
          }}
        />
        <div className="relative flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-medium tracking-wide text-primary uppercase">
              Operator dashboard
            </p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight text-ink">
              {greeting}, {user?.fullName?.split(' ')[0] || 'Operator'}
            </h1>
            <p className="mt-1 max-w-xl text-sm text-muted">
              Your assigned stations, bookings waiting for approve, and QR
              transfers ready to complete.
            </p>
          </div>
          <button
            type="button"
            onClick={() => void load()}
            disabled={loading}
            className="relative rounded-xl border border-line bg-panel/80 px-3.5 py-2 text-sm font-medium text-ink backdrop-blur transition hover:bg-surface disabled:opacity-60"
          >
            Refresh
          </button>
        </div>
      </section>

      {error ? (
        <div className="rounded-xl border border-error-500/20 bg-error-500/10 px-4 py-2.5 text-sm text-error-500">
          {error}
        </div>
      ) : null}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <KpiCard
          title="My stations"
          value={String(stats.stations)}
          hint={`${stats.activeStations} active`}
          to="/operator/stations"
          color="#4d7cff"
          icon="station"
          loading={loading}
        />
        <KpiCard
          title="Pending"
          value={String(stats.pending)}
          hint="Waiting for your approve"
          to="/operator/reservations"
          color="#f59e0b"
          icon="pending"
          loading={loading}
        />
        <KpiCard
          title="Ready to verify"
          value={String(stats.ready)}
          hint="Approved with QR"
          to="/operator/verify"
          color="#8b5cf6"
          icon="verify"
          loading={loading}
        />
        <KpiCard
          title="Completed today"
          value={String(stats.completedToday)}
          hint="Transfers finalized today"
          to="/operator/verify"
          color="#22c55e"
          icon="done"
          loading={loading}
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <section className="rounded-2xl border border-line bg-panel p-5 shadow-sm lg:col-span-1">
          <h2 className="text-base font-semibold text-ink">Quick actions</h2>
          <p className="mt-0.5 text-xs text-muted">Jump to daily operator tasks</p>
          <div className="mt-4 space-y-2">
            {[
              {
                to: '/operator/reservations',
                title: 'Approve bookings',
                subtitle: `${stats.pending} pending`,
              },
              {
                to: '/operator/verify',
                title: 'Verify QR',
                subtitle: `${stats.ready} ready`,
              },
              {
                to: '/operator/stations',
                title: 'My stations',
                subtitle: `${stats.stations} assigned`,
              },
              {
                to: '/operator/profile',
                title: 'Profile',
                subtitle: 'Update contact details',
              },
            ].map((item) => (
              <Link
                key={item.to}
                to={item.to}
                className="flex items-center gap-3 rounded-xl border border-line px-3 py-3 transition hover:border-primary/30 hover:bg-surface"
              >
                <span className="inline-flex size-10 items-center justify-center rounded-xl bg-primary-light text-sm font-semibold text-primary">
                  {item.title.charAt(0)}
                </span>
                <span className="min-w-0">
                  <span className="block text-sm font-semibold text-ink">
                    {item.title}
                  </span>
                  <span className="block text-xs text-muted">{item.subtitle}</span>
                </span>
              </Link>
            ))}
          </div>
        </section>

        <section className="rounded-2xl border border-line bg-panel shadow-sm lg:col-span-2">
          <div className="flex items-center justify-between gap-2 border-b border-line px-5 py-4">
            <div>
              <h2 className="text-base font-semibold text-ink">Needs attention</h2>
              <p className="text-xs text-muted">
                Pending approvals and QR verifications
              </p>
            </div>
            <Link
              to="/operator/reservations"
              className="text-xs font-medium text-primary hover:underline"
            >
              View all
            </Link>
          </div>
          {loading ? (
            <div className="space-y-3 p-5">
              <Skeleton className="h-14 w-full" />
              <Skeleton className="h-14 w-full" />
              <Skeleton className="h-14 w-full" />
            </div>
          ) : myStations.length === 0 ? (
            <div className="px-5 py-12 text-center text-sm text-muted">
              No stations assigned yet. Ask Backoffice to assign you.
            </div>
          ) : attention.length === 0 ? (
            <div className="px-5 py-12 text-center">
              <p className="text-sm font-medium text-ink">All clear</p>
              <p className="mt-1 text-sm text-muted">
                No pending approvals or QR verifications right now.
              </p>
            </div>
          ) : (
            <ul className="divide-y divide-line">
              {attention.map((item) => (
                <li key={item.id}>
                  <Link
                    to={item.to}
                    className="flex items-center gap-3 px-5 py-3.5 transition hover:bg-surface"
                  >
                    <span
                      className={[
                        'inline-flex rounded-lg px-2 py-1 text-[10px] font-semibold uppercase',
                        item.kind === 'pending'
                          ? 'bg-warning-500/10 text-warning-500'
                          : 'bg-primary/10 text-primary',
                      ].join(' ')}
                    >
                      {item.kind === 'pending' ? 'Approve' : 'Verify'}
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-medium text-ink">
                        {item.title}
                      </span>
                      <span className="block truncate text-xs text-muted">
                        {item.meta}
                      </span>
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <section className="overflow-hidden rounded-2xl border border-line bg-panel shadow-sm">
        <div className="flex items-center justify-between gap-2 border-b border-line px-5 py-4">
          <div>
            <h2 className="text-base font-semibold text-ink">
              Recently completed
            </h2>
            <p className="text-xs text-muted">
              Latest transfers on your stations
            </p>
          </div>
          <Link
            to="/operator/verify"
            className="text-xs font-medium text-primary hover:underline"
          >
            Verify page
          </Link>
        </div>
        {loading ? (
          <div className="space-y-3 p-5">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        ) : recentCompleted.length === 0 ? (
          <div className="px-5 py-10 text-center text-sm text-muted">
            No completed transfers yet.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="border-b border-line bg-surface/70 text-xs tracking-wide text-muted uppercase">
                <tr>
                  <th className="px-5 py-3 font-medium">Prosumer NIC</th>
                  <th className="px-5 py-3 font-medium">Station</th>
                  <th className="px-5 py-3 font-medium">Slot</th>
                  <th className="px-5 py-3 font-medium">Completed</th>
                </tr>
              </thead>
              <tbody>
                {recentCompleted.map((r) => (
                  <tr
                    key={r.id}
                    className="border-b border-line last:border-b-0"
                  >
                    <td className="px-5 py-3.5 font-medium text-ink">
                      {r.prosumerNic}
                    </td>
                    <td className="px-5 py-3.5 text-ink">
                      {stationNameById.get(r.stationId) || r.stationId}
                    </td>
                    <td className="px-5 py-3.5 text-muted">
                      {formatWhen(r.reservationDateTime)}
                    </td>
                    <td className="px-5 py-3.5 text-muted">
                      {formatWhen(r.lastModifiedAt || r.createdAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
