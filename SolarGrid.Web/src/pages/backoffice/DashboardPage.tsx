import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import Chart from 'react-apexcharts'
import type { ApexOptions } from 'apexcharts'
import axios from 'axios'
import { useAuth } from '../../context/AuthContext'
import { getPendingUsers, getUsers, type AppUser } from '../../api/users'
import { getStations, type Station } from '../../api/stations'
import { getReservations, type Reservation } from '../../api/reservations'

type DashStats = {
  pendingApprovals: number
  activeStations: number
  totalStations: number
  openReservations: number
  approvedReservations: number
  completedReservations: number
  cancelledReservations: number
  gridOperators: number
  prosumers: number
  approvedToday: number
}

type ActivityItem = {
  id: string
  title: string
  meta: string
  to: string
  tone: 'warning' | 'primary' | 'success' | 'muted'
}

const emptyStats: DashStats = {
  pendingApprovals: 0,
  activeStations: 0,
  totalStations: 0,
  openReservations: 0,
  approvedReservations: 0,
  completedReservations: 0,
  cancelledReservations: 0,
  gridOperators: 0,
  prosumers: 0,
  approvedToday: 0,
}

function pct(part: number, whole: number) {
  if (whole <= 0) return part > 0 ? 100 : 0
  return Math.min(100, Math.round((part / whole) * 100))
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
  return (
    <div
      className={`animate-pulse rounded-xl bg-surface ${className || ''}`}
    />
  )
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
    case 'pending':
      return (
        <svg {...common}>
          <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="3" />
          <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
        </svg>
      )
    case 'station':
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
        </svg>
      )
    case 'book':
      return (
        <svg {...common}>
          <rect x="3" y="4" width="18" height="16" rx="2" />
          <path d="M8 2v4M16 2v4M3 10h18" />
        </svg>
      )
    default:
      return (
        <svg {...common}>
          <path d="M12 2l2.4 7.4H22l-6 4.6 2.3 7-6.3-4.6L5.7 21l2.3-7-6-4.6h7.6L12 2z" />
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
  percent,
  icon,
  loading,
}: {
  title: string
  value: string
  hint: string
  to: string
  color: string
  percent: number
  icon: string
  loading?: boolean
}) {
  const sweep = Math.max(0, Math.min(100, percent))

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
      <div className="mb-4 flex items-start justify-between gap-3">
        <span
          className="inline-flex size-11 items-center justify-center rounded-xl"
          style={{ backgroundColor: `${color}18`, color }}
        >
          <KpiIcon name={icon} />
        </span>
        <div
          className="relative flex size-14 shrink-0 items-center justify-center rounded-full"
          style={{
            background: `conic-gradient(${color} 0 ${sweep}%, #eef0f5 ${sweep}% 100%)`,
          }}
        >
          <div className="flex size-10 items-center justify-center rounded-full bg-panel text-[10px] font-semibold text-ink">
            {sweep}%
          </div>
        </div>
      </div>
      <p className="text-sm font-medium text-muted">{title}</p>
      <p className="mt-1 text-3xl font-semibold tracking-tight text-ink transition group-hover:text-primary">
        {value}
      </p>
      <p className="mt-1 text-xs text-muted">{hint}</p>
    </Link>
  )
}

function ChartCard({
  title,
  subtitle,
  action,
  children,
}: {
  title: string
  subtitle?: string
  action?: ReactNode
  children: ReactNode
}) {
  return (
    <section className="rounded-2xl border border-line bg-panel shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-2 border-b border-line px-5 py-4">
        <div>
          <h4 className="text-base font-semibold text-ink">{title}</h4>
          {subtitle ? (
            <p className="mt-0.5 text-xs text-muted">{subtitle}</p>
          ) : null}
        </div>
        {action}
      </div>
      <div className="p-4 sm:p-5">{children}</div>
    </section>
  )
}

function toneDot(tone: ActivityItem['tone']) {
  switch (tone) {
    case 'warning':
      return 'bg-warning-500'
    case 'success':
      return 'bg-success-500'
    case 'primary':
      return 'bg-primary'
    default:
      return 'bg-muted'
  }
}

export default function DashboardPage() {
  const { user } = useAuth()
  const [stats, setStats] = useState<DashStats>(emptyStats)
  const [bookingTrend, setBookingTrend] = useState<{
    labels: string[]
    counts: number[]
  }>({ labels: [], counts: [] })
  const [activity, setActivity] = useState<ActivityItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    async function load() {
      setLoading(true)
      setError('')
      try {
        const [pending, stations, reservations, appUsers] = await Promise.all([
          getPendingUsers(),
          getStations(),
          getReservations(),
          getUsers(),
        ])

        if (cancelled) return

        const today = new Date()
        const activeStations = stations.filter((s) => s.status === 'Active').length
        const openReservations = reservations.filter(
          (r) => r.status === 'Pending',
        ).length
        const approvedReservations = reservations.filter(
          (r) => r.status === 'Approved',
        ).length
        const completedReservations = reservations.filter(
          (r) => r.status === 'Completed',
        ).length
        const cancelledReservations = reservations.filter(
          (r) => r.status === 'Cancelled',
        ).length
        const approvedToday = reservations.filter((r) => {
          if (r.status !== 'Approved' || !r.lastModifiedAt) return false
          return isSameDay(new Date(r.lastModifiedAt), today)
        }).length

        setStats({
          pendingApprovals: pending.length,
          activeStations,
          totalStations: stations.length,
          openReservations,
          approvedReservations,
          completedReservations,
          cancelledReservations,
          gridOperators: appUsers.filter((u) => u.userType === 'GridOperator')
            .length,
          prosumers: appUsers.filter((u) => u.userType === 'Prosumer').length,
          approvedToday,
        })

        const days = Array.from({ length: 7 }, (_, i) => {
          const d = new Date()
          d.setHours(0, 0, 0, 0)
          d.setDate(d.getDate() - (6 - i))
          return d
        })

        setBookingTrend({
          labels: days.map((d) =>
            d.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' }),
          ),
          counts: days.map((day) => {
            const next = new Date(day)
            next.setDate(next.getDate() + 1)
            return reservations.filter((r) => {
              const created = new Date(r.createdAt)
              return created >= day && created < next
            }).length
          }),
        })

        setActivity(buildActivity(pending, stations, reservations))
      } catch (err) {
        if (!cancelled) {
          setError(
            axios.isAxiosError(err)
              ? 'Could not load dashboard data. Check API and sign-in.'
              : 'Could not load dashboard data.',
          )
          setStats(emptyStats)
          setActivity([])
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [])

  const reservationDonut = useMemo(() => {
    const series = [
      stats.openReservations,
      stats.approvedReservations,
      stats.completedReservations,
      stats.cancelledReservations,
    ]
    const options: ApexOptions = {
      chart: {
        type: 'donut',
        fontFamily: 'Outfit, Poppins, sans-serif',
        toolbar: { show: false },
      },
      labels: ['Pending', 'Approved', 'Completed', 'Cancelled'],
      colors: ['#ff9920', '#51ce8a', '#4d7cff', '#fc696a'],
      legend: {
        position: 'bottom',
        fontSize: '12px',
        labels: { colors: '#8a92a6' },
      },
      dataLabels: { enabled: false },
      stroke: { width: 2, colors: ['#ffffff'] },
      plotOptions: {
        pie: {
          donut: {
            size: '74%',
            labels: {
              show: true,
              name: { show: true, color: '#8a92a6', fontSize: '12px' },
              value: {
                show: true,
                fontSize: '22px',
                fontWeight: 600,
                color: '#0e0e23',
              },
              total: {
                show: true,
                label: 'Total',
                fontSize: '12px',
                color: '#8a92a6',
                formatter: () => String(series.reduce((a, b) => a + b, 0)),
              },
            },
          },
        },
      },
      tooltip: { theme: 'light' },
    }
    return { series, options }
  }, [stats])

  const trendChart = useMemo(() => {
    const options: ApexOptions = {
      chart: {
        type: 'area',
        fontFamily: 'Outfit, Poppins, sans-serif',
        toolbar: { show: false },
        zoom: { enabled: false },
        sparkline: { enabled: false },
      },
      colors: ['#4d7cff'],
      dataLabels: { enabled: false },
      stroke: { curve: 'smooth', width: 3 },
      fill: {
        type: 'gradient',
        gradient: {
          shadeIntensity: 1,
          opacityFrom: 0.4,
          opacityTo: 0.04,
          stops: [0, 85, 100],
        },
      },
      markers: {
        size: 4,
        colors: ['#ffffff'],
        strokeColors: '#4d7cff',
        strokeWidth: 2,
        hover: { size: 6 },
      },
      grid: {
        borderColor: '#e4e7ec',
        strokeDashArray: 4,
        padding: { left: 8, right: 8 },
      },
      xaxis: {
        categories: bookingTrend.labels,
        labels: { style: { colors: '#8a92a6', fontSize: '11px' } },
        axisBorder: { show: false },
        axisTicks: { show: false },
      },
      yaxis: {
        min: 0,
        tickAmount: 4,
        labels: {
          style: { colors: '#8a92a6', fontSize: '11px' },
          formatter: (v) => String(Math.round(v)),
        },
      },
      tooltip: { theme: 'light' },
    }
    return {
      options,
      series: [{ name: 'New bookings', data: bookingTrend.counts }],
    }
  }, [bookingTrend])

  const totalBookings =
    stats.openReservations +
    stats.approvedReservations +
    stats.completedReservations +
    stats.cancelledReservations

  const weekTotal = bookingTrend.counts.reduce((a, b) => a + b, 0)

  return (
    <div className="space-y-5">
      <div className="relative overflow-hidden rounded-2xl border border-line bg-panel px-5 py-5 shadow-sm sm:px-6">
        <div className="pointer-events-none absolute inset-y-0 right-0 w-1/2 bg-gradient-to-l from-primary-light/80 to-transparent" />
        <div className="relative flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-xs font-medium tracking-wide text-primary uppercase">
              Solar Admin
            </p>
            <h1 className="mt-1 text-xl font-semibold text-ink sm:text-2xl">
              Operations dashboard
            </h1>
            <p className="mt-1 text-sm text-muted">
              Welcome back, {user?.fullName || 'Backoffice'}. Live microgrid
              control overview.
            </p>
          </div>
          {loading ? (
            <span className="rounded-lg bg-surface px-3 py-1.5 text-xs font-medium text-muted">
              Syncing…
            </span>
          ) : (
            <span className="inline-flex items-center gap-1.5 rounded-lg bg-success-500/10 px-3 py-1.5 text-xs font-medium text-success-500">
              <span className="size-1.5 rounded-full bg-success-500" />
              Live data
            </span>
          )}
        </div>
      </div>

      {error ? (
        <div className="rounded-xl border border-error-500/20 bg-error-500/10 px-4 py-2.5 text-sm text-error-500">
          {error}
        </div>
      ) : null}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <KpiCard
          title="Pending Approvals"
          value={String(stats.pendingApprovals)}
          hint={`${stats.prosumers} prosumers total`}
          to="/backoffice/pending"
          color="#ff9920"
          icon="pending"
          percent={pct(stats.pendingApprovals, Math.max(stats.prosumers, 1))}
          loading={loading}
        />
        <KpiCard
          title="Active Stations"
          value={String(stats.activeStations)}
          hint={`${stats.activeStations} of ${stats.totalStations} live`}
          to="/backoffice/stations"
          color="#51ce8a"
          icon="station"
          percent={pct(stats.activeStations, Math.max(stats.totalStations, 1))}
          loading={loading}
        />
        <KpiCard
          title="Open Reservations"
          value={String(stats.openReservations)}
          hint={`${totalBookings} bookings overall`}
          to="/backoffice/reservations"
          color="#4d7cff"
          icon="book"
          percent={pct(stats.openReservations, Math.max(totalBookings, 1))}
          loading={loading}
        />
        <KpiCard
          title="Approved Today"
          value={String(stats.approvedToday)}
          hint={`${stats.gridOperators} grid operators`}
          to="/backoffice/reservations"
          color="#f59e0b"
          icon="badge"
          percent={pct(stats.approvedToday, Math.max(stats.approvedReservations, 1))}
          loading={loading}
        />
      </div>

      <ChartCard
        title="Booking activity"
        subtitle="New reservations created in the last 7 days"
        action={
          <span className="rounded-lg bg-primary-light px-2.5 py-1 text-xs font-medium text-primary">
            {weekTotal} this week
          </span>
        }
      >
        {loading ? (
          <Skeleton className="h-[300px] w-full" />
        ) : (
          <Chart
            type="area"
            height={300}
            series={trendChart.series}
            options={trendChart.options}
          />
        )}
      </ChartCard>

      <div className="grid gap-4 xl:grid-cols-5">
        <div className="xl:col-span-2">
          <ChartCard
            title="Reservation mix"
            subtitle="Status breakdown across all bookings"
          >
            {loading ? (
              <Skeleton className="h-[280px] w-full" />
            ) : totalBookings === 0 ? (
              <div className="flex h-[280px] flex-col items-center justify-center text-center">
                <p className="text-sm font-medium text-ink">No bookings yet</p>
                <p className="mt-1 text-xs text-muted">
                  Charts fill as Prosumers create reservations.
                </p>
              </div>
            ) : (
              <Chart
                type="donut"
                height={280}
                series={reservationDonut.series}
                options={reservationDonut.options}
              />
            )}
          </ChartCard>
        </div>

        <div className="xl:col-span-3">
          <section className="flex h-full flex-col rounded-2xl border border-line bg-panel shadow-sm">
            <div className="flex items-center justify-between border-b border-line px-5 py-4">
              <div>
                <h4 className="text-base font-semibold text-ink">
                  Recent activity
                </h4>
                <p className="mt-0.5 text-xs text-muted">
                  Latest pending users and bookings
                </p>
              </div>
              <Link
                to="/backoffice/users"
                className="text-xs font-medium text-primary hover:underline"
              >
                View users
              </Link>
            </div>
            <div className="flex-1 divide-y divide-line">
              {loading ? (
                <div className="space-y-3 p-5">
                  <Skeleton className="h-12 w-full" />
                  <Skeleton className="h-12 w-full" />
                  <Skeleton className="h-12 w-full" />
                  <Skeleton className="h-12 w-full" />
                </div>
              ) : activity.length === 0 ? (
                <div className="flex h-full min-h-[220px] items-center justify-center p-6 text-sm text-muted">
                  No recent activity yet.
                </div>
              ) : (
                activity.map((item) => (
                  <Link
                    key={item.id}
                    to={item.to}
                    className="flex items-start gap-3 px-5 py-3.5 transition hover:bg-surface"
                  >
                    <span
                      className={`mt-1.5 size-2.5 shrink-0 rounded-full ${toneDot(item.tone)}`}
                    />
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-medium text-ink">
                        {item.title}
                      </span>
                      <span className="block text-xs text-muted">
                        {item.meta}
                      </span>
                    </span>
                    <span className="shrink-0 text-xs font-medium text-primary">
                      Open
                    </span>
                  </Link>
                ))
              )}
            </div>
          </section>
        </div>
      </div>

      <section className="rounded-2xl border border-line bg-panel shadow-sm">
        <div className="border-b border-line px-5 py-4">
          <h4 className="text-base font-semibold text-ink">Quick actions</h4>
        </div>
        <div className="grid gap-2 p-3 sm:grid-cols-2 xl:grid-cols-4">
          {[
            {
              label: 'Pending prosumers',
              meta: `${stats.pendingApprovals} waiting`,
              to: '/backoffice/pending',
              color: 'bg-warning-500/10 text-warning-500',
            },
            {
              label: 'Solar stations',
              meta: `${stats.activeStations} active`,
              to: '/backoffice/stations',
              color: 'bg-success-500/10 text-success-500',
            },
            {
              label: 'Reservations',
              meta: `${stats.openReservations} open`,
              to: '/backoffice/reservations',
              color: 'bg-primary/10 text-primary',
            },
            {
              label: 'Create operator',
              meta: `${stats.gridOperators} on staff`,
              to: '/backoffice/operators',
              color: 'bg-brand-500/10 text-brand-600',
            },
          ].map((item) => (
            <Link
              key={item.to}
              to={item.to}
              className="rounded-xl border border-line px-4 py-3 transition hover:border-primary/30 hover:bg-surface"
            >
              <span
                className={`inline-flex rounded-lg px-2 py-1 text-[11px] font-semibold ${item.color}`}
              >
                Go
              </span>
              <p className="mt-2 text-sm font-semibold text-ink">{item.label}</p>
              <p className="text-xs text-muted">{item.meta}</p>
            </Link>
          ))}
        </div>
      </section>
    </div>
  )
}

function buildActivity(
  pending: AppUser[],
  stations: Station[],
  reservations: Reservation[],
): ActivityItem[] {
  const stationName = (id: string) =>
    stations.find((s) => s.id === id)?.stationName || 'Station'

  const pendingItems: ActivityItem[] = [...pending]
    .sort(
      (a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    )
    .slice(0, 3)
    .map((u) => ({
      id: `pending-${u.id}`,
      title: `Pending approval · ${u.fullName}`,
      meta: `${u.email} · ${formatWhen(u.createdAt)}`,
      to: '/backoffice/pending',
      tone: 'warning' as const,
    }))

  const bookingItems: ActivityItem[] = [...reservations]
    .sort(
      (a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    )
    .slice(0, 4)
    .map((r) => ({
      id: `res-${r.id}`,
      title: `${r.status} booking · ${r.prosumerNic}`,
      meta: `${stationName(r.stationId)} · ${formatWhen(r.reservationDateTime)}`,
      to: '/backoffice/reservations',
      tone:
        r.status === 'Pending'
          ? ('warning' as const)
          : r.status === 'Approved'
            ? ('success' as const)
            : ('primary' as const),
    }))

  return [...pendingItems, ...bookingItems].slice(0, 7)
}
