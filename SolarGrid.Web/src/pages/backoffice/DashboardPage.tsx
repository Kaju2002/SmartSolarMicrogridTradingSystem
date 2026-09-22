import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

function GaugeCard({
  title,
  value,
  hint,
  to,
  color,
}: {
  title: string
  value: string
  hint: string
  to: string
  color: string
}) {
  return (
    <Link
      to={to}
      className="rounded-2xl border border-line bg-panel p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <h4 className="mb-2 text-base font-medium text-ink">{title}</h4>
          <div className="mb-3 flex flex-wrap items-center gap-3 text-xs text-muted">
            <span className="inline-flex items-center gap-1.5">
              <span className="size-2 rounded-full bg-error-500" /> High
            </span>
            <span className="inline-flex items-center gap-1.5">
              <span className="size-2 rounded-full bg-success-500" /> Moderate
            </span>
            <span className="inline-flex items-center gap-1.5">
              <span className="size-2 rounded-full bg-warning-500" /> Low
            </span>
          </div>
          <p className="text-3xl font-semibold tracking-tight text-ink">{value}</p>
          <p className="mt-1 text-xs text-muted">{hint}</p>
        </div>
        <div
          className="relative mt-1 flex size-[5.5rem] shrink-0 items-center justify-center rounded-full"
          style={{
            background: `conic-gradient(${color} 0 65%, #e9ecef 65% 100%)`,
          }}
        >
          <div className="flex size-[3.75rem] items-center justify-center rounded-full bg-panel text-xs font-semibold text-ink">
            Open
          </div>
        </div>
      </div>
    </Link>
  )
}

export default function DashboardPage() {
  const { user } = useAuth()

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-xl font-semibold text-ink sm:text-2xl">Dashboard</h1>
        <p className="text-sm text-muted">
          Welcome, {user?.fullName || 'Backoffice'}. Microgrid operations overview.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <GaugeCard
          title="Pending Approvals"
          value="—"
          hint="Prosumers waiting for review"
          to="/backoffice/pending"
          color="#ff9920"
        />
        <GaugeCard
          title="Active Stations"
          value="—"
          hint="Stations live on the grid"
          to="/backoffice/stations"
          color="#51ce8a"
        />
        <GaugeCard
          title="Open Reservations"
          value="—"
          hint="Bookings awaiting action"
          to="/backoffice/reservations"
          color="#4d7cff"
        />
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        <section className="rounded-2xl border border-line bg-panel shadow-sm">
          <div className="border-b border-line px-5 py-4">
            <h4 className="text-base font-semibold text-ink">
              Operations Monitoring
            </h4>
          </div>
          <div className="grid gap-6 p-5 md:grid-cols-2">
            <div>
              <p className="mb-2 text-sm text-muted">
                <span className="mr-2 inline-block size-2 rounded-full bg-success-500" />
                Approved bookings
              </p>
              <p className="mb-2 text-4xl font-semibold text-ink">—</p>
              <p className="text-xs text-muted">Ready for QR / station handoff</p>

              <hr className="my-6 border-line" />

              <p className="mb-2 text-sm text-muted">
                <span className="mr-2 inline-block size-2 rounded-full bg-warning-500" />
                Grid operators
              </p>
              <p className="mb-2 text-4xl font-semibold text-ink">—</p>
              <p className="text-xs text-muted">Staff accounts on the portal</p>
            </div>

            <div className="flex flex-col justify-between">
              <div className="mb-4 flex flex-1 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-50 to-orange-100 p-6">
                <div className="text-center">
                  <div className="mx-auto mb-3 flex size-16 items-center justify-center rounded-2xl bg-brand-500 text-white shadow-lg shadow-brand-500/30">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                      <circle cx="12" cy="12" r="4" fill="white" />
                      <path
                        d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1 7 17M17 7l2.1-2.1"
                        stroke="white"
                        strokeWidth="1.8"
                        strokeLinecap="round"
                      />
                    </svg>
                  </div>
                  <p className="text-sm font-semibold text-ink">Solar Microgrid</p>
                  <p className="text-xs text-muted">Trading control desk</p>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3 rounded-xl bg-surface p-3">
                <div>
                  <p className="text-[11px] text-muted">Stations</p>
                  <p className="text-sm font-semibold text-ink">Manage →</p>
                </div>
                <div>
                  <p className="text-[11px] text-muted">Bookings</p>
                  <p className="text-sm font-semibold text-ink">Review →</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-line bg-panel shadow-sm">
          <div className="flex items-center justify-between border-b border-line px-5 py-4">
            <h4 className="text-base font-semibold text-ink">Quick Actions</h4>
          </div>
          <div className="space-y-0 p-2">
            {[
              {
                label: 'Review pending prosumers',
                meta: 'Approve / reject registrations',
                to: '/backoffice/pending',
                border: 'border-primary',
              },
              {
                label: 'Manage solar stations',
                meta: 'Create, edit, deactivate',
                to: '/backoffice/stations',
                border: 'border-success-500',
              },
              {
                label: 'Approve energy reservations',
                meta: 'Generate QR for operators',
                to: '/backoffice/reservations',
                border: 'border-warning-500',
              },
              {
                label: 'Create grid operator',
                meta: 'Provision staff login',
                to: '/backoffice/operators',
                border: 'border-brand-500',
              },
            ].map((item) => (
              <Link
                key={item.to}
                to={item.to}
                className="flex items-center justify-between rounded-xl px-3 py-3 transition hover:bg-surface"
              >
                <div className={`border-l-4 ps-3 ${item.border}`}>
                  <p className="text-sm font-medium text-ink">{item.label}</p>
                  <p className="text-xs text-muted">{item.meta}</p>
                </div>
                <span className="text-xs font-medium text-primary">Open</span>
              </Link>
            ))}
          </div>
        </section>
      </div>

      <section className="rounded-2xl border border-line bg-panel shadow-sm">
        <div className="border-b border-line px-5 py-4">
          <h4 className="text-base font-semibold text-ink">System Information</h4>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead>
              <tr className="border-b border-line text-muted">
                <th className="px-5 py-3 font-medium">Module</th>
                <th className="px-5 py-3 font-medium">Status</th>
                <th className="px-5 py-3 font-medium">Owner</th>
                <th className="px-5 py-3 font-medium">Next step</th>
              </tr>
            </thead>
            <tbody>
              {[
                {
                  name: 'Pending Approvals',
                  status: 'Ready to wire',
                  badge: 'bg-warning-500/10 text-warning-500',
                  owner: 'Backoffice',
                  next: 'List + approve API',
                },
                {
                  name: 'Stations',
                  status: 'API available',
                  badge: 'bg-primary/10 text-primary',
                  owner: 'Backoffice',
                  next: 'CRUD screens',
                },
                {
                  name: 'Reservations',
                  status: 'API available',
                  badge: 'bg-primary/10 text-primary',
                  owner: 'Backoffice',
                  next: 'Approve + QR',
                },
                {
                  name: 'Grid Operators',
                  status: 'Create via Backoffice',
                  badge: 'bg-success-50 text-success-500',
                  owner: 'Backoffice',
                  next: 'Operator form',
                },
              ].map((row) => (
                <tr key={row.name} className="border-b border-line last:border-0">
                  <td className="px-5 py-3 font-medium text-ink">{row.name}</td>
                  <td className="px-5 py-3">
                    <span
                      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${row.badge}`}
                    >
                      {row.status}
                    </span>
                  </td>
                  <td className="px-5 py-3 text-muted">{row.owner}</td>
                  <td className="px-5 py-3 text-muted">{row.next}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}
