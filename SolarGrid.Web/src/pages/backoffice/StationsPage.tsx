import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import PageHeader from '../../components/PageHeader'
import {
  deactivateStation,
  getStations,
  type Station,
} from '../../api/stations'
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

export default function StationsPage() {
  const [stations, setStations] = useState<Station[]>([])
  const [operators, setOperators] = useState<AppUser[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [toast, setToast] = useState('')
  const [actionId, setActionId] = useState<string | null>(null)

  const operatorNameById = useMemo(() => {
    const map = new Map<string, string>()
    for (const op of operators) {
      map.set(op.id, op.fullName)
    }
    return map
  }, [operators])

  async function loadStations() {
    setLoading(true)
    setError('')
    try {
      const [data, ops] = await Promise.all([
        getStations(),
        getUsers('GridOperator'),
      ])
      setStations(data)
      setOperators(ops)
    } catch (err) {
      setError(errorMessage(err, 'Could not load stations.'))
      setStations([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadStations()
  }, [])

  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(() => setToast(''), 2800)
    return () => window.clearTimeout(timer)
  }, [toast])

  async function handleDeactivate(station: Station) {
    if (station.status === 'Deactivated') return
    if (
      !window.confirm(
        `Deactivate “${station.stationName}”? This fails if active reservations exist.`,
      )
    ) {
      return
    }

    setActionId(station.id)
    setError('')
    try {
      const result = await deactivateStation(station.id)
      if (!result.success) {
        setError(result.message || 'Deactivate failed.')
        return
      }
      setToast(result.message || 'Station deactivated')
      await loadStations()
    } catch (err) {
      setError(errorMessage(err, 'Could not deactivate station.'))
    } finally {
      setActionId(null)
    }
  }

  return (
    <div>
      <PageHeader
        title="Stations"
        subtitle="Manage solar stations on the microgrid."
        action={
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => void loadStations()}
              disabled={loading}
              className="rounded-xl border border-line bg-panel px-3.5 py-2 text-sm font-medium text-ink transition hover:bg-surface disabled:opacity-60"
            >
              Refresh
            </button>
            <Link
              to="/backoffice/stations/new"
              className="rounded-xl bg-primary px-3.5 py-2 text-sm font-medium text-white transition hover:opacity-90"
            >
              Add station
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

      <div className="overflow-hidden rounded-2xl border border-line bg-panel shadow-sm">
        {loading ? (
          <div className="px-6 py-16 text-center text-sm text-muted">
            Loading stations…
          </div>
        ) : stations.length === 0 ? (
          <div className="px-6 py-16 text-center">
            <p className="mb-1 text-sm font-medium text-ink">No stations yet</p>
            <p className="mb-4 text-sm text-muted">
              Register the first solar hub on a separate create page.
            </p>
            <Link
              to="/backoffice/stations/new"
              className="inline-flex rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white"
            >
              Create station
            </Link>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="border-b border-line bg-surface/70 text-xs tracking-wide text-muted uppercase">
                <tr>
                  <th className="px-5 py-3.5 font-medium">Station</th>
                  <th className="px-5 py-3.5 font-medium">Operator</th>
                  <th className="px-5 py-3.5 font-medium">Location</th>
                  <th className="px-5 py-3.5 font-medium">Capacity / slots</th>
                  <th className="px-5 py-3.5 font-medium">Hours</th>
                  <th className="px-5 py-3.5 font-medium">Status</th>
                  <th className="px-5 py-3.5 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {stations.map((station) => {
                  const busy = actionId === station.id
                  const active = station.status === 'Active'
                  const operatorLabel = station.assignedOperatorId
                    ? operatorNameById.get(station.assignedOperatorId) ||
                      'Unknown operator'
                    : 'Unassigned'

                  return (
                    <tr
                      key={station.id}
                      className="border-b border-line last:border-b-0"
                    >
                      <td className="px-5 py-4">
                        <p className="font-medium text-ink">
                          {station.stationName}
                        </p>
                        <p className="text-xs text-muted">
                          {station.availableSlots}/{station.batterySlots} free
                        </p>
                      </td>
                      <td className="px-5 py-4 text-ink">{operatorLabel}</td>
                      <td className="px-5 py-4 text-muted">
                        {station.latitude.toFixed(4)},{' '}
                        {station.longitude.toFixed(4)}
                      </td>
                      <td className="px-5 py-4 text-ink">
                        {station.capacityKWh} kWh · {station.batterySlots} slots
                      </td>
                      <td className="px-5 py-4 text-muted">
                        {station.openTime} – {station.closeTime}
                      </td>
                      <td className="px-5 py-4">
                        <span
                          className={[
                            'inline-flex rounded-lg px-2 py-1 text-xs font-medium',
                            active
                              ? 'bg-success-500/10 text-success-500'
                              : 'bg-error-500/10 text-error-500',
                          ].join(' ')}
                        >
                          {station.status}
                        </span>
                      </td>
                      <td className="px-5 py-4">
                        <div className="flex flex-wrap justify-end gap-2">
                          {active ? (
                            <>
                              <Link
                                to={`/backoffice/stations/${station.id}/edit`}
                                className="rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary"
                              >
                                Edit
                              </Link>
                              <button
                                type="button"
                                disabled={busy}
                                onClick={() => void handleDeactivate(station)}
                                className="rounded-lg bg-error-500 px-3 py-1.5 text-xs font-medium text-white disabled:opacity-60"
                              >
                                {busy ? '…' : 'Deactivate'}
                              </button>
                            </>
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
