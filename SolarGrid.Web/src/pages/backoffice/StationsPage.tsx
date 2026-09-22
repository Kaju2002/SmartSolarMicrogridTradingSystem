import PageHeader from '../../components/PageHeader'

export default function StationsPage() {
  return (
    <div>
      <PageHeader
        title="Stations"
        subtitle="Create, update, and deactivate solar stations on the microgrid."
      />
      <div className="rounded-2xl border border-dashed border-line bg-panel px-6 py-16 text-center">
        <p className="mb-1 text-sm font-medium text-ink">No stations loaded</p>
        <p className="text-sm text-muted">
          Station list and create form will connect to the Stations API next.
        </p>
      </div>
    </div>
  )
}
