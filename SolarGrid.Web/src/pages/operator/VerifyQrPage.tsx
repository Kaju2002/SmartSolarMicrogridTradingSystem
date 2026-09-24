import { useEffect, useMemo, useState } from 'react'
import axios from 'axios'
import PageHeader from '../../components/PageHeader'
import { useAuth } from '../../context/AuthContext'
import { getStations, type Station } from '../../api/stations'
import { getReservations, type Reservation } from '../../api/reservations'
import { verifyQr } from '../../api/verification'

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

function formatDate(value?: string | null) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

export default function OperatorVerifyQrPage() {
  const { user } = useAuth()
  const [reservations, setReservations] = useState<Reservation[]>([])
  const [stations, setStations] = useState<Station[]>([])
  const [loading, setLoading] = useState(true)
  const [busyKey, setBusyKey] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState<{
    message: string
    reservationId?: string | null
    status?: string | null
  } | null>(null)

  const myStationIds = useMemo(() => {
    const id = user?.userId
    if (!id) return new Set<string>()
    return new Set(
      stations.filter((s) => s.assignedOperatorId === id).map((s) => s.id),
    )
  }, [stations, user?.userId])

  const stationNameById = useMemo(() => {
    const map = new Map<string, string>()
    for (const s of stations) {
      if (myStationIds.has(s.id)) map.set(s.id, s.stationName)
    }
    return map
  }, [stations, myStationIds])

  const mine = useMemo(
    () => reservations.filter((r) => myStationIds.has(r.stationId)),
    [reservations, myStationIds],
  )

  const readyToVerify = useMemo(
    () =>
      mine
        .filter((r) => r.status === 'Approved' && r.qrCode)
        .sort(
          (a, b) =>
            new Date(a.reservationDateTime).getTime() -
            new Date(b.reservationDateTime).getTime(),
        ),
    [mine],
  )

  const recentlyCompleted = useMemo(
    () =>
      mine
        .filter((r) => r.status === 'Completed')
        .sort(
          (a, b) =>
            new Date(b.lastModifiedAt || b.createdAt).getTime() -
            new Date(a.lastModifiedAt || a.createdAt).getTime(),
        )
        .slice(0, 8),
    [mine],
  )

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [list, stationList] = await Promise.all([
        getReservations(),
        getStations(),
      ])
      setReservations(list)
      setStations(stationList)
    } catch (err) {
      setError(errorMessage(err, 'Could not load bookings.'))
      setReservations([])
      setStations([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function runVerify(code: string, busyId: string) {
    const trimmed = code.trim()
    if (!trimmed) {
      setError('QR code is missing for this booking.')
      setSuccess(null)
      return
    }

    setBusyKey(busyId)
    setError('')
    setSuccess(null)
    try {
      const result = await verifyQr({ qrCode: trimmed })
      if (!result.success) {
        setError(result.message || 'Verification failed.')
        return
      }
      setSuccess({
        message: result.message || 'Transfer finalized',
        reservationId: result.reservationId,
        status: result.status,
      })
      await load()
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.data) {
        const data = err.response.data as { message?: string }
        if (data.message) {
          setError(data.message)
          return
        }
      }
      setError(errorMessage(err, 'Could not verify QR.'))
    } finally {
      setBusyKey(null)
    }
  }

  return (
    <div>
      <PageHeader
        title="Verify QR"
        subtitle="Finalize energy transfer for approved bookings at your stations."
        action={
          <button
            type="button"
            onClick={() => void load()}
            disabled={loading || busyKey !== null}
            className="rounded-xl border border-line bg-panel px-3.5 py-2 text-sm font-medium text-ink transition hover:bg-surface disabled:opacity-60"
          >
            Refresh
          </button>
        }
      />

      {error ? (
        <div className="mb-4 rounded-xl border border-error-500/20 bg-error-500/10 px-4 py-2.5 text-sm text-error-500">
          {error}
        </div>
      ) : null}

      {success ? (
        <div className="mb-4 rounded-xl border border-success-500/20 bg-success-500/10 px-4 py-3 text-sm text-success-500">
          <p className="font-medium">{success.message}</p>
          {success.status ? (
            <p className="mt-1 text-xs opacity-90">Status: {success.status}</p>
          ) : null}
          {success.reservationId ? (
            <p className="mt-0.5 break-all font-mono text-xs opacity-80">
              Reservation: {success.reservationId}
            </p>
          ) : null}
        </div>
      ) : null}

      <section className="mb-5 overflow-hidden rounded-2xl border border-line bg-panel shadow-sm">
        <div className="border-b border-line px-5 py-4">
          <h2 className="text-base font-semibold text-ink">
            Ready to verify
          </h2>
          <p className="mt-0.5 text-xs text-muted">
            Approved bookings on your stations with a QR code.
          </p>
        </div>
        {loading ? (
          <div className="px-6 py-12 text-center text-sm text-muted">
            Loading…
          </div>
        ) : myStationIds.size === 0 ? (
          <div className="px-6 py-12 text-center text-sm text-muted">
            No stations assigned. Ask Backoffice to assign you first.
          </div>
        ) : readyToVerify.length === 0 ? (
          <div className="px-6 py-12 text-center">
            <p className="mb-1 text-sm font-medium text-ink">
              Nothing waiting
            </p>
            <p className="text-sm text-muted">
              Approve a pending reservation first, then it will appear here.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="border-b border-line bg-surface/70 text-xs tracking-wide text-muted uppercase">
                <tr>
                  <th className="px-5 py-3.5 font-medium">Prosumer NIC</th>
                  <th className="px-5 py-3.5 font-medium">Station</th>
                  <th className="px-5 py-3.5 font-medium">Slot time</th>
                  <th className="px-5 py-3.5 font-medium">QR</th>
                  <th className="px-5 py-3.5 text-right font-medium">Action</th>
                </tr>
              </thead>
              <tbody>
                {readyToVerify.map((r) => {
                  const busy = busyKey === r.id
                  const stationName =
                    stationNameById.get(r.stationId) || r.stationId
                  return (
                    <tr
                      key={r.id}
                      className="border-b border-line last:border-b-0"
                    >
                      <td className="px-5 py-4 font-medium text-ink">
                        {r.prosumerNic}
                      </td>
                      <td className="px-5 py-4 text-ink">{stationName}</td>
                      <td className="px-5 py-4 text-muted">
                        {formatDate(r.reservationDateTime)}
                      </td>
                      <td className="px-5 py-4">
                        <button
                          type="button"
                          title={r.qrCode || ''}
                          className="max-w-[10rem] truncate font-mono text-xs text-primary hover:underline"
                          onClick={() => {
                            if (!r.qrCode) return
                            void navigator.clipboard.writeText(r.qrCode)
                          }}
                        >
                          {r.qrCode?.slice(0, 10)}…
                        </button>
                      </td>
                      <td className="px-5 py-4">
                        <div className="flex justify-end">
                          <button
                            type="button"
                            disabled={busyKey !== null || !r.qrCode}
                            onClick={() => void runVerify(r.qrCode || '', r.id)}
                            className="rounded-lg bg-success-500 px-3 py-1.5 text-xs font-medium text-white disabled:opacity-60"
                          >
                            {busy ? '…' : 'Verify'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="overflow-hidden rounded-2xl border border-line bg-panel shadow-sm">
        <div className="border-b border-line px-5 py-4">
          <h2 className="text-base font-semibold text-ink">
            Recently completed
          </h2>
          <p className="mt-0.5 text-xs text-muted">
            Last transfers marked Completed on your stations.
          </p>
        </div>
        {loading ? (
          <div className="px-6 py-10 text-center text-sm text-muted">
            Loading…
          </div>
        ) : recentlyCompleted.length === 0 ? (
          <div className="px-6 py-10 text-center text-sm text-muted">
            No completed transfers yet.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="border-b border-line bg-surface/70 text-xs tracking-wide text-muted uppercase">
                <tr>
                  <th className="px-5 py-3.5 font-medium">Prosumer NIC</th>
                  <th className="px-5 py-3.5 font-medium">Station</th>
                  <th className="px-5 py-3.5 font-medium">Slot time</th>
                  <th className="px-5 py-3.5 font-medium">Completed</th>
                </tr>
              </thead>
              <tbody>
                {recentlyCompleted.map((r) => (
                  <tr
                    key={r.id}
                    className="border-b border-line last:border-b-0"
                  >
                    <td className="px-5 py-4 font-medium text-ink">
                      {r.prosumerNic}
                    </td>
                    <td className="px-5 py-4 text-ink">
                      {stationNameById.get(r.stationId) || r.stationId}
                    </td>
                    <td className="px-5 py-4 text-muted">
                      {formatDate(r.reservationDateTime)}
                    </td>
                    <td className="px-5 py-4 text-muted">
                      {formatDate(r.lastModifiedAt || r.createdAt)}
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
