import { useEffect, useMemo, useState } from 'react'
import axios from 'axios'
import PageHeader from '../../components/PageHeader'
import { getStations, type Station } from '../../api/stations'
import { useAuth } from '../../context/AuthContext'

function errorMessage(err: unknown, fallback: string) {
  if (!axios.isAxiosError(err)) return fallback
  if (err.response?.status === 401) {
    return 'Unauthorized (401). Sign out and sign in again so a valid token is sent.'
  }
  if (err.response?.status === 403) {
    return 'Forbidden (403). Grid Operator access is required for stations.'
  }
  if (err.code === 'ERR_NETWORK') {
    return 'Network error. Is the API running on http://localhost:5204?'
  }
  const data = err.response?.data as { message?: string } | undefined
  return data?.message || `${fallback} (HTTP ${err.response?.status ?? '—'})`
}

export default function OperatorStationsPage() {
  const { user } = useAuth()
  const [stations, setStations] = useState<Station[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const mine = useMemo(() => {
    const id = user?.userId
    if (!id) return []
    return stations.filter((s) => s.assignedOperatorId === id)
  }, [stations, user?.userId])

  async function loadStations() {
    setLoading(true)
    setError('')
    try {
      const data = await getStations()
      setStations(data)
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

  const activeCount = mine.filter((s) => s.status === 'Active').length

  return (
    <div>
      <PageHeader
        title="My Stations"
        subtitle="Solar stations assigned to you by Backoffice."
        action={
          <button
            type="button"
            onClick={() => void loadStations()}
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
            Assigned{' '}
            <span className="font-semibold text-ink">{mine.length}</span>
          </span>
          <span className="rounded-xl border border-line bg-panel px-3.5 py-2 text-muted">
            Active{' '}
            <span className="font-semibold text-ink">{activeCount}</span>
          </span>
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
            Loading your stations…
          </div>
        ) : !user?.userId ? (
          <div className="px-6 py-16 text-center text-sm text-muted">
            Missing signed-in user id. Sign out and sign in again.
          </div>
        ) : mine.length === 0 ? (
          <div className="px-6 py-16 text-center">
            <p className="mb-1 text-sm font-medium text-ink">
              No stations assigned
            </p>
            <p className="text-sm text-muted">
              Ask Backoffice to assign you as the operator on a station.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="border-b border-line bg-surface/70 text-xs tracking-wide text-muted uppercase">
                <tr>
                  <th className="px-5 py-3.5 font-medium">Station</th>
                  <th className="px-5 py-3.5 font-medium">Location</th>
                  <th className="px-5 py-3.5 font-medium">Capacity / slots</th>
                  <th className="px-5 py-3.5 font-medium">Hours</th>
                  <th className="px-5 py-3.5 font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                {mine.map((station) => {
                  const active = station.status === 'Active'
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
