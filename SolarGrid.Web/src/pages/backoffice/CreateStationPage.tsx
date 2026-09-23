import { useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import axios from 'axios'
import { useAuth } from '../../context/AuthContext'
import { createStation } from '../../api/stations'
import { AppTimePicker } from '../../components/AppTimePicker'

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

const inputClass =
  'h-11 w-full rounded-lg border border-line bg-panel px-4 text-sm text-ink shadow-sm outline-none transition placeholder:text-muted/80 focus:border-primary focus:ring-2 focus:ring-primary/15'

function Field({
  label,
  children,
}: {
  label: string
  children: ReactNode
}) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-medium text-ink">{label}</span>
      {children}
    </label>
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

export default function CreateStationPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [form, setForm] = useState({
    stationName: '',
    latitude: '',
    longitude: '',
    capacityKWh: '',
    batterySlots: '',
    openTime: '06:00',
    closeTime: '18:00',
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')

    if (!user?.userId) {
      setError('Missing signed-in user id. Sign in again.')
      return
    }

    const stationName = form.stationName.trim()
    const latitude = Number(form.latitude)
    const longitude = Number(form.longitude)
    const capacityKWh = Number(form.capacityKWh)
    const batterySlots = Number(form.batterySlots)

    if (!stationName) {
      setError('Station name is required.')
      return
    }
    if (
      Number.isNaN(latitude) ||
      Number.isNaN(longitude) ||
      Number.isNaN(capacityKWh) ||
      Number.isNaN(batterySlots)
    ) {
      setError(
        'Latitude, longitude, capacity, and battery slots must be numbers.',
      )
      return
    }
    if (batterySlots < 1) {
      setError('Battery slots must be at least 1.')
      return
    }

    setSaving(true)
    try {
      const result = await createStation({
        stationName,
        latitude,
        longitude,
        capacityKWh,
        batterySlots,
        openTime: form.openTime || '06:00',
        closeTime: form.closeTime || '18:00',
        createdBy: user.userId,
      })

      if (!result.success) {
        setError(result.message || 'Create failed.')
        return
      }

      navigate('/backoffice/stations', { replace: true })
    } catch (err) {
      setError(errorMessage(err, 'Could not create station.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="mb-1 text-xs text-muted">
            <Link to="/backoffice/stations" className="hover:text-primary">
              Stations
            </Link>
            <span className="mx-1.5">/</span>
            <span className="text-ink">Create</span>
          </p>
          <h1 className="text-2xl font-semibold tracking-tight text-ink">
            Create Station
          </h1>
          <p className="mt-1 text-sm text-muted">
            Register a new solar hub on the microgrid.
          </p>
        </div>
        <Link
          to="/backoffice/stations"
          className="inline-flex items-center gap-2 rounded-lg border border-line bg-panel px-3.5 py-2 text-sm font-medium text-ink shadow-sm transition hover:bg-surface"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <path d="M15 18l-6-6 6-6" />
          </svg>
          Back to stations
        </Link>
      </div>

      {error ? (
        <div className="rounded-xl border border-error-500/20 bg-error-500/10 px-4 py-2.5 text-sm text-error-500">
          {error}
        </div>
      ) : null}

      <form onSubmit={handleSubmit} className="space-y-5">
        <div className="grid gap-5 xl:grid-cols-2">
          <FormCard title="Station details">
            <Field label="Station name">
              <input
                className={inputClass}
                value={form.stationName}
                onChange={(e) =>
                  setForm((f) => ({ ...f, stationName: e.target.value }))
                }
                placeholder="e.g. Jaffna Central Hub"
                required
              />
            </Field>

            <div className="grid gap-5 sm:grid-cols-2">
              <Field label="Latitude">
                <input
                  className={inputClass}
                  type="number"
                  step="any"
                  value={form.latitude}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, latitude: e.target.value }))
                  }
                  placeholder="9.6615"
                  required
                />
              </Field>
              <Field label="Longitude">
                <input
                  className={inputClass}
                  type="number"
                  step="any"
                  value={form.longitude}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, longitude: e.target.value }))
                  }
                  placeholder="80.0255"
                  required
                />
              </Field>
            </div>
          </FormCard>

          <FormCard title="Capacity & hours">
            <div className="grid gap-5 sm:grid-cols-2">
              <Field label="Capacity (kWh)">
                <input
                  className={inputClass}
                  type="number"
                  step="any"
                  min="0"
                  value={form.capacityKWh}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, capacityKWh: e.target.value }))
                  }
                  placeholder="500"
                  required
                />
              </Field>
              <Field label="Battery slots">
                <input
                  className={inputClass}
                  type="number"
                  min="1"
                  value={form.batterySlots}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, batterySlots: e.target.value }))
                  }
                  placeholder="12"
                  required
                />
              </Field>
            </div>

            <div className="grid gap-5 sm:grid-cols-2">
              <AppTimePicker
                label="Open time"
                value={form.openTime}
                onChange={(openTime) => setForm((f) => ({ ...f, openTime }))}
              />
              <AppTimePicker
                label="Close time"
                value={form.closeTime}
                onChange={(closeTime) => setForm((f) => ({ ...f, closeTime }))}
              />
            </div>
          </FormCard>
        </div>

        <div className="flex flex-wrap items-center justify-end gap-3 rounded-2xl border border-line bg-panel px-5 py-4 shadow-sm">
          <Link
            to="/backoffice/stations"
            className="rounded-lg border border-line px-5 py-2.5 text-sm font-medium text-ink transition hover:bg-surface"
          >
            Cancel
          </Link>
          <button
            type="submit"
            disabled={saving}
            className="rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-white shadow-sm transition hover:opacity-90 disabled:opacity-60"
          >
            {saving ? 'Creating…' : 'Create station'}
          </button>
        </div>
      </form>
    </div>
  )
}
