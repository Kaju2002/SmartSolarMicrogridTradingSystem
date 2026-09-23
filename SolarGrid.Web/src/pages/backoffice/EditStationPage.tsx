import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import axios from 'axios'
import { AppTimePicker } from '../../components/AppTimePicker'
import { getStations, updateStation, type Station } from '../../api/stations'
import { getUsers, type AppUser } from '../../api/users'

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

const readOnlyClass =
  'flex h-11 w-full items-center rounded-lg border border-line bg-surface px-4 text-sm text-ink'

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

export default function EditStationPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [station, setStation] = useState<Station | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({
    capacityKWh: '',
    batterySlots: '',
    openTime: '06:00',
    closeTime: '18:00',
    assignedOperatorId: '',
  })
  const [operators, setOperators] = useState<AppUser[]>([])

  useEffect(() => {
    let cancelled = false

    async function load() {
      if (!id) {
        setError('Missing station id.')
        setLoading(false)
        return
      }

      setLoading(true)
      setError('')
      try {
        const [list, ops] = await Promise.all([
          getStations(),
          getUsers('GridOperator'),
        ])
        const found = list.find((s) => s.id === id) ?? null
        if (cancelled) return

        setOperators(ops)

        if (!found) {
          setStation(null)
          setError('Station not found.')
          return
        }

        setStation(found)
        setForm({
          capacityKWh: String(found.capacityKWh),
          batterySlots: String(found.batterySlots),
          openTime: found.openTime || '06:00',
          closeTime: found.closeTime || '18:00',
          assignedOperatorId: found.assignedOperatorId || '',
        })
      } catch (err) {
        if (!cancelled) {
          setError(errorMessage(err, 'Could not load station.'))
          setStation(null)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [id])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!id || !station) return

    setError('')
    const capacityKWh = Number(form.capacityKWh)
    const batterySlots = Number(form.batterySlots)

    if (Number.isNaN(capacityKWh) || Number.isNaN(batterySlots)) {
      setError('Capacity and battery slots must be numbers.')
      return
    }
    if (batterySlots < 1) {
      setError('Battery slots must be at least 1.')
      return
    }

    setSaving(true)
    try {
      const result = await updateStation(id, {
        capacityKWh,
        batterySlots,
        openTime: form.openTime,
        closeTime: form.closeTime,
        assignedOperatorId: form.assignedOperatorId || null,
        updateAssignedOperator: true,
      })

      if (!result.success) {
        setError(result.message || 'Update failed.')
        return
      }

      navigate('/backoffice/stations', { replace: true })
    } catch (err) {
      setError(errorMessage(err, 'Could not update station.'))
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="rounded-2xl border border-line bg-panel px-6 py-16 text-center text-sm text-muted">
        Loading station…
      </div>
    )
  }

  if (!station) {
    return (
      <div className="space-y-4">
        <p className="text-sm text-error-500">{error || 'Station not found.'}</p>
        <Link
          to="/backoffice/stations"
          className="inline-flex rounded-lg border border-line bg-panel px-3.5 py-2 text-sm font-medium text-ink"
        >
          Back to stations
        </Link>
      </div>
    )
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
            <span className="text-ink">Edit</span>
          </p>
          <h1 className="text-2xl font-semibold tracking-tight text-ink">
            Edit Station
          </h1>
          <p className="mt-1 text-sm text-muted">
            Update capacity, hours, and assigned Grid Operator. Name and
            location stay fixed.
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
            <Field label="Station name" hint="Read only">
              <div className={readOnlyClass}>{station.stationName}</div>
            </Field>

            <div className="grid gap-5 sm:grid-cols-2">
              <Field label="Latitude" hint="Read only">
                <div className={readOnlyClass}>
                  {station.latitude.toFixed(4)}
                </div>
              </Field>
              <Field label="Longitude" hint="Read only">
                <div className={readOnlyClass}>
                  {station.longitude.toFixed(4)}
                </div>
              </Field>
            </div>

            <Field label="Status" hint="Read only">
              <div className={readOnlyClass}>
                <span
                  className={[
                    'inline-flex rounded-lg px-2 py-0.5 text-xs font-medium',
                    station.status === 'Active'
                      ? 'bg-success-500/10 text-success-500'
                      : 'bg-error-500/10 text-error-500',
                  ].join(' ')}
                >
                  {station.status}
                </span>
              </div>
            </Field>
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

            <Field label="Assigned Grid Operator">
              <select
                className={inputClass}
                value={form.assignedOperatorId}
                onChange={(e) =>
                  setForm((f) => ({
                    ...f,
                    assignedOperatorId: e.target.value,
                  }))
                }
              >
                <option value="">Unassigned</option>
                {operators.map((op) => (
                  <option
                    key={op.id}
                    value={op.id}
                    disabled={op.status !== 'Active'}
                  >
                    {op.fullName}
                    {op.username ? ` (${op.username})` : ''}
                    {op.status !== 'Active' ? ` — ${op.status}` : ''}
                  </option>
                ))}
              </select>
            </Field>
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
            disabled={saving || station.status !== 'Active'}
            className="rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-white shadow-sm transition hover:opacity-90 disabled:opacity-60"
          >
            {saving ? 'Saving…' : 'Save changes'}
          </button>
        </div>
      </form>
    </div>
  )
}
