import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import axios from 'axios'
import {
  getProfile,
  requestDeactivation,
  updateProfile,
  type UserProfile,
} from '../../api/users'
import { useAuth } from '../../context/AuthContext'

function errorMessage(err: unknown, fallback: string) {
  if (!axios.isAxiosError(err)) return fallback
  if (err.response?.status === 401) {
    return 'Unauthorized (401). Sign out and sign in again so a valid token is sent.'
  }
  if (err.response?.status === 403) {
    return 'Forbidden (403). You can only edit your own profile.'
  }
  if (err.code === 'ERR_NETWORK') {
    return 'Network error. Is the API running on http://localhost:5204?'
  }
  const data = err.response?.data as { message?: string } | undefined
  return data?.message || `${fallback} (HTTP ${err.response?.status ?? '—'})`
}

const inputClass =
  'h-11 w-full rounded-lg border border-line bg-panel px-4 text-sm text-ink shadow-sm outline-none transition placeholder:text-muted/80 focus:border-primary focus:ring-2 focus:ring-primary/15'

const readOnlyClass =
  'flex h-11 w-full items-center rounded-lg border border-line bg-surface px-4 text-sm font-medium text-ink'

function Field({
  label,
  children,
  hint,
}: {
  label: string
  children: ReactNode
  hint?: string
}) {
  return (
    <div className="block">
      <div className="mb-2 flex items-center justify-between gap-2">
        <span className="block text-sm font-medium text-ink">{label}</span>
        {hint ? <span className="text-[11px] text-muted">{hint}</span> : null}
      </div>
      {children}
    </div>
  )
}

function FormCard({
  title,
  children,
}: {
  title: string
  children: ReactNode
}) {
  return (
    <section className="rounded-2xl border border-line bg-panel shadow-sm">
      <div className="border-b border-line px-5 py-4">
        <h2 className="text-base font-semibold text-ink">{title}</h2>
      </div>
      <div className="space-y-5 p-5">{children}</div>
    </section>
  )
}

function formatWhen(value?: string | null) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function ProfilePage() {
  const { user, updateUser, logout } = useAuth()
  const navigate = useNavigate()

  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    phoneNumber: '',
  })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [deactivating, setDeactivating] = useState(false)
  const [error, setError] = useState('')
  const [toast, setToast] = useState('')

  useEffect(() => {
    if (!user?.userId) {
      setLoading(false)
      setError('Missing signed-in user. Sign in again.')
      return
    }

    let cancelled = false
    setLoading(true)
    setError('')

    void getProfile(user.userId)
      .then((data) => {
        if (cancelled) return
        setProfile(data)
        setForm({
          fullName: data.fullName || '',
          email: data.email || '',
          phoneNumber: data.phoneNumber || '',
        })
      })
      .catch((err) => {
        if (cancelled) return
        setError(errorMessage(err, 'Could not load profile.'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [user?.userId])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!user?.userId) return

    setError('')
    setToast('')

    const fullName = form.fullName.trim()
    const email = form.email.trim()
    const phoneNumber = form.phoneNumber.trim()

    if (!fullName || !email || !phoneNumber) {
      setError('Full name, email and phone are required.')
      return
    }

    setSaving(true)
    try {
      const result = await updateProfile(user.userId, {
        fullName,
        email,
        phoneNumber,
      })

      if (!result.success) {
        setError(result.message || 'Could not update profile.')
        return
      }

      setProfile((prev) =>
        prev
          ? { ...prev, fullName, email, phoneNumber, updatedAt: new Date().toISOString() }
          : prev,
      )
      updateUser({ fullName: result.fullName || fullName })
      setToast('Profile saved')
    } catch (err) {
      setError(errorMessage(err, 'Could not update profile.'))
    } finally {
      setSaving(false)
    }
  }

  async function handleDeactivate() {
    if (!user?.userId) return
    const ok = window.confirm(
      'Request account deactivation? You will be signed out and will need Backoffice reactivation to sign in again.',
    )
    if (!ok) return

    setDeactivating(true)
    setError('')
    setToast('')
    try {
      const result = await requestDeactivation(user.userId)
      if (!result.success) {
        setError(result.message || 'Could not deactivate account.')
        return
      }
      logout()
      navigate('/login', { replace: true })
    } catch (err) {
      setError(errorMessage(err, 'Could not deactivate account.'))
    } finally {
      setDeactivating(false)
    }
  }

  const dirty =
    !!profile &&
    (form.fullName.trim() !== (profile.fullName || '') ||
      form.email.trim() !== (profile.email || '') ||
      form.phoneNumber.trim() !== (profile.phoneNumber || ''))

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="mb-1 text-xs text-muted">
            <Link to="/backoffice" className="hover:text-primary">
              Backoffice
            </Link>
            <span className="mx-1.5 text-line">/</span>
            Profile
          </p>
          <h1 className="text-2xl font-semibold tracking-tight text-ink">
            My Profile
          </h1>
          <p className="mt-1 text-sm text-muted">
            View account details and update your contact information.
          </p>
        </div>
      </div>

      {toast ? (
        <div className="rounded-xl border border-success-500/20 bg-success-500/10 px-4 py-3 text-sm text-success-500">
          {toast}
        </div>
      ) : null}

      {error ? (
        <div className="rounded-xl border border-error-500/20 bg-error-500/10 px-4 py-3 text-sm text-error-500">
          {error}
        </div>
      ) : null}

      {loading ? (
        <div className="space-y-4">
          <div className="h-40 animate-pulse rounded-2xl bg-surface" />
          <div className="h-64 animate-pulse rounded-2xl bg-surface" />
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="flex flex-wrap items-center gap-4 rounded-2xl border border-line bg-panel p-5 shadow-sm">
            <div className="flex size-14 items-center justify-center rounded-2xl bg-brand-950 text-xl font-semibold text-brand-400">
              {(form.fullName || profile?.fullName || 'U').charAt(0).toUpperCase()}
            </div>
            <div className="min-w-0">
              <p className="truncate text-lg font-semibold text-ink">
                {form.fullName || profile?.fullName || '—'}
              </p>
              <p className="text-sm text-muted">
                {profile?.userType || user?.userType || '—'}
                {profile?.status ? ` · ${profile.status}` : ''}
              </p>
            </div>
          </div>

          <div className="grid gap-5 lg:grid-cols-2">
            <FormCard title="Account">
              <Field label="Role" hint="Read-only">
                <div className={readOnlyClass}>{profile?.userType || '—'}</div>
              </Field>
              <Field label="Username" hint="Read-only">
                <div className={readOnlyClass}>{profile?.username || '—'}</div>
              </Field>
              <Field label="Status" hint="Read-only">
                <div className={readOnlyClass}>{profile?.status || '—'}</div>
              </Field>
              <Field label="User ID" hint="Read-only">
                <div className={`${readOnlyClass} break-all font-mono text-xs`}>
                  {profile?.id || user?.userId || '—'}
                </div>
              </Field>
              <div className="grid gap-5 sm:grid-cols-2">
                <Field label="Created">
                  <div className={readOnlyClass}>{formatWhen(profile?.createdAt)}</div>
                </Field>
                <Field label="Updated">
                  <div className={readOnlyClass}>{formatWhen(profile?.updatedAt)}</div>
                </Field>
              </div>
            </FormCard>

            <FormCard title="Contact">
              <Field label="Full name">
                <input
                  type="text"
                  value={form.fullName}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, fullName: e.target.value }))
                  }
                  className={inputClass}
                  autoComplete="name"
                  required
                />
              </Field>
              <Field label="Email">
                <input
                  type="email"
                  value={form.email}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, email: e.target.value }))
                  }
                  className={inputClass}
                  autoComplete="email"
                  required
                />
              </Field>
              <Field label="Phone">
                <input
                  type="tel"
                  value={form.phoneNumber}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, phoneNumber: e.target.value }))
                  }
                  className={inputClass}
                  autoComplete="tel"
                  required
                />
              </Field>
            </FormCard>
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-line pt-4">
            <button
              type="button"
              onClick={() => void handleDeactivate()}
              disabled={deactivating || saving}
              className="rounded-lg border border-error-500/30 bg-error-500/5 px-4 py-2.5 text-sm font-medium text-error-500 transition hover:bg-error-500/10 disabled:opacity-50"
            >
              {deactivating ? 'Deactivating…' : 'Request deactivation'}
            </button>

            <div className="flex flex-wrap gap-3">
              <button
                type="button"
                onClick={() => {
                  if (!profile) return
                  setForm({
                    fullName: profile.fullName || '',
                    email: profile.email || '',
                    phoneNumber: profile.phoneNumber || '',
                  })
                  setError('')
                  setToast('')
                }}
                disabled={!dirty || saving}
                className="rounded-lg border border-line bg-panel px-4 py-2.5 text-sm font-medium text-ink transition hover:bg-surface disabled:opacity-50"
              >
                Reset
              </button>
              <button
                type="submit"
                disabled={!dirty || saving}
                className="rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-primary/90 disabled:opacity-50"
              >
                {saving ? 'Saving…' : 'Save changes'}
              </button>
            </div>
          </div>
        </form>
      )}
    </div>
  )
}
