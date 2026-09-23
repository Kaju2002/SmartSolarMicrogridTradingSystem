import { useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import axios from 'axios'
import { createGridOperator } from '../../api/auth'

function errorMessage(err: unknown, fallback: string) {
  if (!axios.isAxiosError(err)) return fallback
  if (err.response?.status === 401) {
    return 'Unauthorized (401). Sign out and sign in again so a valid token is sent.'
  }
  if (err.response?.status === 409) {
    const data = err.response.data as { message?: string } | undefined
    return data?.message || 'Username already exists.'
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

export default function CreateOperatorPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    fullName: '',
    username: '',
    email: '',
    phoneNumber: '',
    password: '',
    confirmPassword: '',
  })
  const [showPassword, setShowPassword] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [toast, setToast] = useState('')

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setToast('')

    const fullName = form.fullName.trim()
    const username = form.username.trim()
    const email = form.email.trim()
    const phoneNumber = form.phoneNumber.trim()

    if (!fullName || !username || !email || !phoneNumber || !form.password) {
      setError('All fields are required.')
      return
    }
    if (username.length < 3) {
      setError('Username must be at least 3 characters.')
      return
    }
    if (form.password.length < 6) {
      setError('Password must be at least 6 characters.')
      return
    }
    if (form.password !== form.confirmPassword) {
      setError('Password and confirm password do not match.')
      return
    }

    setSaving(true)
    try {
      const result = await createGridOperator({
        username,
        password: form.password,
        fullName,
        email,
        phoneNumber,
      })

      if (!result.success) {
        setError(result.message || 'Could not create operator.')
        return
      }

      setToast(`${result.fullName || fullName} created as Grid Operator`)
      setForm({
        fullName: '',
        username: '',
        email: '',
        phoneNumber: '',
        password: '',
        confirmPassword: '',
      })

      window.setTimeout(() => {
        navigate('/backoffice', { replace: true })
      }, 900)
    } catch (err) {
      setError(errorMessage(err, 'Could not create operator.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="mb-1 text-xs text-muted">
            <Link to="/backoffice" className="hover:text-primary">
              Backoffice
            </Link>
            <span className="mx-1.5">/</span>
            <span className="text-ink">Create Grid Operator</span>
          </p>
          <h1 className="text-2xl font-semibold tracking-tight text-ink">
            Create Grid Operator
          </h1>
          <p className="mt-1 text-sm text-muted">
            Provision staff accounts. Operators sign in with username and
            password.
          </p>
        </div>
      </div>

      {toast ? (
        <div className="rounded-xl border border-success-500/20 bg-success-500/10 px-4 py-2.5 text-sm text-success-500">
          {toast}
        </div>
      ) : null}

      {error ? (
        <div className="rounded-xl border border-error-500/20 bg-error-500/10 px-4 py-2.5 text-sm text-error-500">
          {error}
        </div>
      ) : null}

      <form onSubmit={handleSubmit} className="space-y-5">
        <div className="grid gap-5 xl:grid-cols-2">
          <FormCard title="Account">
            <Field label="Role" hint="Locked">
              <div className={readOnlyClass}>GridOperator</div>
            </Field>

            <Field label="Full name">
              <input
                className={inputClass}
                value={form.fullName}
                onChange={(e) =>
                  setForm((f) => ({ ...f, fullName: e.target.value }))
                }
                placeholder="e.g. Nimal Perera"
                required
              />
            </Field>

            <Field label="Username">
              <input
                className={inputClass}
                value={form.username}
                onChange={(e) =>
                  setForm((f) => ({ ...f, username: e.target.value }))
                }
                placeholder="e.g. nimal.op"
                autoComplete="off"
                required
              />
            </Field>
          </FormCard>

          <FormCard title="Contact & password">
            <Field label="Email">
              <input
                className={inputClass}
                type="email"
                value={form.email}
                onChange={(e) =>
                  setForm((f) => ({ ...f, email: e.target.value }))
                }
                placeholder="operator@solargrid.lk"
                required
              />
            </Field>

            <Field label="Phone number">
              <input
                className={inputClass}
                value={form.phoneNumber}
                onChange={(e) =>
                  setForm((f) => ({ ...f, phoneNumber: e.target.value }))
                }
                placeholder="07XXXXXXXX"
                required
              />
            </Field>

            <div className="grid gap-5 sm:grid-cols-2">
              <Field label="Password">
                <div className="relative">
                  <input
                    className={`${inputClass} pr-11`}
                    type={showPassword ? 'text' : 'password'}
                    value={form.password}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, password: e.target.value }))
                    }
                    autoComplete="new-password"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((v) => !v)}
                    className="absolute top-1/2 right-3 -translate-y-1/2 text-xs font-medium text-muted hover:text-primary"
                  >
                    {showPassword ? 'Hide' : 'Show'}
                  </button>
                </div>
              </Field>
              <Field label="Confirm password">
                <input
                  className={inputClass}
                  type={showPassword ? 'text' : 'password'}
                  value={form.confirmPassword}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      confirmPassword: e.target.value,
                    }))
                  }
                  autoComplete="new-password"
                  required
                />
              </Field>
            </div>
          </FormCard>
        </div>

        <div className="flex flex-wrap items-center justify-end gap-3 rounded-2xl border border-line bg-panel px-5 py-4 shadow-sm">
          <Link
            to="/backoffice"
            className="rounded-lg border border-line px-5 py-2.5 text-sm font-medium text-ink transition hover:bg-surface"
          >
            Cancel
          </Link>
          <button
            type="submit"
            disabled={saving}
            className="rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-white shadow-sm transition hover:opacity-90 disabled:opacity-60"
          >
            {saving ? 'Creating…' : 'Create operator'}
          </button>
        </div>
      </form>
    </div>
  )
}
