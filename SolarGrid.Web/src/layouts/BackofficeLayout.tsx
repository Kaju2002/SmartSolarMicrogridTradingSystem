import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const navItems = [
  { to: '/backoffice', label: 'Dashboard', end: true, icon: 'grid' },
  { to: '/backoffice/pending', label: 'Pending Approvals', end: false, icon: 'users' },
  { to: '/backoffice/stations', label: 'Stations', end: false, icon: 'station' },
  { to: '/backoffice/reservations', label: 'Reservations', end: false, icon: 'book' },
  { to: '/backoffice/operators', label: 'Create Grid Operator', end: false, icon: 'badge' },
  { to: '/backoffice/profile', label: 'Profile', end: false, icon: 'user' },
] as const

function NavIcon({ name }: { name: string }) {
  const common = {
    width: 20,
    height: 20,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 2,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
  }

  switch (name) {
    case 'grid':
      return (
        <svg {...common}>
          <rect x="2" y="3" width="20" height="14" rx="2" />
          <path d="M8 21h8M12 17v4" />
        </svg>
      )
    case 'users':
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
    case 'badge':
      return (
        <svg {...common}>
          <path d="M12 2l2.4 7.4H22l-6 4.6 2.3 7-6.3-4.6L5.7 21l2.3-7-6-4.6h7.6L12 2z" />
        </svg>
      )
    default:
      return (
        <svg {...common}>
          <circle cx="12" cy="8" r="3.5" />
          <path d="M5 20a7 7 0 0 1 14 0" />
        </svg>
      )
  }
}

export default function BackofficeLayout() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [mini, setMini] = useState(false)
  const [search, setSearch] = useState('')
  const [profileOpen, setProfileOpen] = useState(false)

  function handleLogout() {
    logout()
    setProfileOpen(false)
    navigate('/login', { replace: true })
  }

  function handleMenuClick() {
    if (window.matchMedia('(min-width: 1024px)').matches) {
      setMini((v) => !v)
    } else {
      setMobileOpen((v) => !v)
    }
  }

  const sidebarWidth = mini ? 'w-[4.75rem]' : 'w-[16.5rem]'

  function renderNav(onNavigate?: () => void, forceExpanded = false) {
    const collapsed = mini && !forceExpanded

    const itemClass = ({ isActive }: { isActive: boolean }) =>
      [
        'relative flex items-center rounded-xl text-sm font-medium transition',
        collapsed ? 'justify-center px-0 py-3' : 'gap-3 px-3 py-2.5',
        isActive
          ? 'bg-primary text-white shadow-sm'
          : 'text-ink/70 hover:bg-primary-light hover:text-primary',
      ].join(' ')

    return (
      <nav
        className={`space-y-1 py-4 ${collapsed ? 'px-2' : 'px-3'}`}
      >
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            title={item.label}
            className={itemClass}
            onClick={onNavigate}
          >
            <NavIcon name={item.icon} />
            {!collapsed ? <span>{item.label}</span> : null}
          </NavLink>
        ))}
      </nav>
    )
  }

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-surface">
      <header className="flex h-[4.5rem] shrink-0 border-b border-line bg-panel">
        <div
          className={`hidden h-full shrink-0 items-center border-r border-line transition-all duration-200 lg:flex ${sidebarWidth} ${
            mini ? 'justify-center px-0' : 'gap-3 px-4'
          }`}
        >
          <img
            src="/brand/logo-letter.png"
            alt="Solar Admin"
            className="size-10 rounded-lg object-cover"
          />
          {!mini ? (
            <div className="min-w-0">
              <p className="truncate text-base font-semibold tracking-tight text-ink">
                Solar Admin
              </p>
              <p className="text-[11px] text-muted">Backoffice portal</p>
            </div>
          ) : null}
        </div>

        <div className="flex min-w-0 flex-1 items-center justify-between gap-3 px-4 lg:px-6">
          <div className="flex min-w-0 flex-1 items-center gap-3">
            <button
              type="button"
              onClick={handleMenuClick}
              className="inline-flex size-10 items-center justify-center rounded-xl bg-primary-light text-primary"
              aria-label={mini ? 'Expand sidebar' : 'Collapse sidebar'}
              title={mini ? 'Expand sidebar' : 'Collapse sidebar'}
            >
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M3 12h18M3 6h18M3 18h18" />
              </svg>
            </button>

            <div className="hidden h-8 w-px bg-line sm:block" />

            <form
              className="relative hidden max-w-md flex-1 md:block"
              onSubmit={(e) => e.preventDefault()}
            >
              <input
                type="search"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search"
                className="h-10 w-full rounded-xl border border-line bg-surface pr-10 pl-4 text-sm text-ink outline-none placeholder:text-muted focus:border-primary"
              />
              <span className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-muted">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="7" />
                  <path d="M21 21l-4.3-4.3" />
                </svg>
              </span>
            </form>
          </div>

          <div className="flex items-center gap-1.5 sm:gap-2">
            <button
              type="button"
              className="inline-flex size-10 items-center justify-center rounded-xl bg-primary-light text-primary"
              title="Notifications"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M22 17H2a3 3 0 0 0 3-3V9a7 7 0 0 1 14 0v5a3 3 0 0 0 3 3zm-8.27 4a2 2 0 0 1-3.46 0" />
              </svg>
            </button>
            <button
              type="button"
              className="hidden size-10 items-center justify-center rounded-xl bg-primary-light text-primary sm:inline-flex"
              title="Messages"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
              </svg>
            </button>

            <div className="mx-1 hidden h-8 w-px bg-line md:block" />

            <button
              type="button"
              className="hidden size-10 items-center justify-center rounded-xl bg-primary-light text-primary md:inline-flex"
              title="Fullscreen"
              onClick={() => {
                if (!document.fullscreenElement) {
                  void document.documentElement.requestFullscreen()
                } else {
                  void document.exitFullscreen()
                }
              }}
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3" />
              </svg>
            </button>

            <button
              type="button"
              onClick={() => setProfileOpen(true)}
              className="ml-1 rounded-full p-0.5 ring-2 ring-transparent transition hover:ring-primary/30"
              title="User"
            >
              <img
                src="/brand/avatar.png"
                alt={user?.fullName || 'User'}
                className="size-10 rounded-full object-cover"
              />
            </button>
          </div>
        </div>
      </header>

      <div className="flex min-h-0 flex-1">
        <aside
          className={`hidden h-full shrink-0 flex-col overflow-y-auto border-r border-line bg-sidebar transition-all duration-200 lg:flex ${sidebarWidth}`}
        >
          {renderNav()}
          {!mini ? (
            <div className="mt-8 px-3 pb-4">
              <img
                src="/brand/sidebar-widget.gif"
                alt=""
                className="w-full rounded-2xl object-cover"
              />
              <p className="mt-2 text-center text-sm font-semibold text-ink">
                Solar Admin
              </p>
            </div>
          ) : (
            <div className="mt-6 px-2 pb-3">
              <img
                src="/brand/sidebar-widget.gif"
                alt=""
                className="mx-auto size-10 rounded-lg object-cover"
              />
            </div>
          )}
        </aside>

        {mobileOpen ? (
          <div className="fixed inset-0 z-40 flex lg:hidden">
            <button
              type="button"
              className="absolute inset-0 bg-ink/40"
              aria-label="Close menu"
              onClick={() => setMobileOpen(false)}
            />
            <aside className="relative z-50 flex h-full w-[16.5rem] flex-col bg-sidebar shadow-xl">
              <div className="flex h-[4.5rem] items-center gap-3 border-b border-line px-4">
                <img
                  src="/brand/logo-letter.png"
                  alt="Solar Admin"
                  className="size-10 rounded-lg object-cover"
                />
                <div className="min-w-0">
                  <p className="truncate text-base font-semibold text-ink">
                    Solar Admin
                  </p>
                  <p className="text-[11px] text-muted">Backoffice portal</p>
                </div>
              </div>
              {renderNav(() => setMobileOpen(false), true)}
              <div className="mt-8 px-3 pb-4">
                <img
                  src="/brand/sidebar-widget.gif"
                  alt=""
                  className="w-full rounded-2xl object-cover"
                />
                <p className="mt-2 text-center text-sm font-semibold text-ink">
                  Solar Admin
                </p>
              </div>
            </aside>
          </div>
        ) : null}

        <div className="flex min-w-0 flex-1 flex-col">
          <main className="flex-1 overflow-y-auto">
            <div className="mx-auto max-w-[1400px] p-4 lg:p-6">
              <Outlet />
            </div>
          </main>

          <footer className="border-t border-line bg-panel px-4 py-3 lg:px-6">
            <div className="mx-auto flex max-w-[1400px] flex-col gap-1 text-xs text-muted sm:flex-row sm:items-center sm:justify-between">
              <p>
                © {new Date().getFullYear()} Solar Admin · Smart Solar Microgrid
                Trading System
              </p>
              <p className="sm:text-right">
                Signed in as{' '}
                <span className="font-medium text-ink">
                  {user?.fullName || 'User'}
                </span>
                {user?.userType ? ` · ${user.userType}` : ''}
              </p>
            </div>
          </footer>
        </div>
      </div>

      {profileOpen ? (
        <div className="fixed inset-0 z-50 flex justify-end">
          <button
            type="button"
            className="absolute inset-0 bg-ink/35"
            aria-label="Close profile panel"
            onClick={() => setProfileOpen(false)}
          />
          <aside className="relative z-10 flex h-full w-full max-w-sm flex-col bg-panel shadow-2xl">
            <div className="flex items-center justify-between border-b border-line px-6 py-5">
              <div>
                <h4 className="text-lg font-semibold text-ink">User Profile</h4>
                <p className="text-xs text-muted">Backoffice session</p>
              </div>
              <button
                type="button"
                onClick={() => setProfileOpen(false)}
                className="inline-flex size-8 items-center justify-center rounded-lg bg-error-500/10 text-error-500"
                aria-label="Close"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <path d="M18 6 6 18M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div className="flex-1 overflow-y-auto px-6 py-5">
              <div className="mb-6 flex gap-4">
                <img
                  src="/brand/avatar.png"
                  alt=""
                  className="size-24 rounded-2xl object-cover"
                />
                <div className="min-w-0 pt-1">
                  <h5 className="truncate text-base font-semibold text-ink">
                    {user?.fullName || 'User'}
                  </h5>
                  <p className="mt-0.5 text-sm text-muted">
                    {user?.userType || 'Backoffice'}
                  </p>
                  <button
                    type="button"
                    onClick={() => {
                      setProfileOpen(false)
                      navigate('/backoffice/profile')
                    }}
                    className="mt-3 inline-flex items-center gap-1 rounded-lg bg-primary/10 px-2.5 py-1.5 text-xs font-medium text-primary"
                  >
                    + View profile
                  </button>
                </div>
              </div>

              <div className="space-y-1 border-t border-line pt-5">
                {[
                  { to: '/backoffice/profile', title: 'My Profile', subtitle: 'Account settings and more' },
                  { to: '/backoffice/pending', title: 'Pending Approvals', subtitle: 'Review prosumer requests' },
                  { to: '/backoffice/stations', title: 'Stations', subtitle: 'Manage solar stations' },
                  { to: '/backoffice/reservations', title: 'Reservations', subtitle: 'Bookings and approvals' },
                ].map((item) => (
                  <Link
                    key={item.to}
                    to={item.to}
                    onClick={() => setProfileOpen(false)}
                    className="flex items-center gap-3 rounded-xl px-2 py-3 transition hover:bg-surface"
                  >
                    <span className="inline-flex size-12 items-center justify-center rounded-xl bg-primary-light text-sm font-semibold text-primary">
                      {item.title.charAt(0)}
                    </span>
                    <span>
                      <span className="block text-sm font-semibold text-ink">
                        {item.title}
                      </span>
                      <span className="block text-xs text-muted">
                        {item.subtitle}
                      </span>
                    </span>
                  </Link>
                ))}
              </div>
            </div>

            <div className="border-t border-line p-4">
              <button
                type="button"
                onClick={handleLogout}
                className="w-full rounded-xl bg-error-500 px-4 py-2.5 text-sm font-medium text-white transition hover:opacity-90"
              >
                Sign Out
              </button>
            </div>
          </aside>
        </div>
      ) : null}
    </div>
  )
}
