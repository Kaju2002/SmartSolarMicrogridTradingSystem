import PageHeader from '../../components/PageHeader'

export default function ReservationsPage() {
  return (
    <div>
      <PageHeader
        title="Reservations"
        subtitle="Monitor energy bookings and approve reservations for QR handoff."
      />
      <div className="rounded-2xl border border-dashed border-line bg-panel px-6 py-16 text-center">
        <p className="mb-1 text-sm font-medium text-ink">No reservations loaded</p>
        <p className="text-sm text-muted">
          Bookings table and approve action will connect to the API next.
        </p>
      </div>
    </div>
  )
}
