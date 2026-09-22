import PageHeader from '../../components/PageHeader'

export default function PendingApprovalsPage() {
  return (
    <div>
      <PageHeader
        title="Pending Approvals"
        subtitle="Review Prosumer registrations waiting for Backoffice approval."
      />
      <div className="rounded-2xl border border-dashed border-line bg-panel px-6 py-16 text-center shadow-sm">
        <p className="mb-1 text-sm font-medium text-ink">No data wired yet</p>
        <p className="text-sm text-muted">
          Pending users table with Approve / Reject comes next.
        </p>
      </div>
    </div>
  )
}
