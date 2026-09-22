import PageHeader from '../../components/PageHeader'

export default function CreateOperatorPage() {
  return (
    <div>
      <PageHeader
        title="Create Grid Operator"
        subtitle="Provision staff accounts. Operators sign in with username and password."
      />
      <div className="rounded-2xl border border-dashed border-line bg-panel px-6 py-16 text-center">
        <p className="mb-1 text-sm font-medium text-ink">Form coming next</p>
        <p className="text-sm text-muted">
          Backoffice-only operator creation will use the register API with role
          locked to GridOperator.
        </p>
      </div>
    </div>
  )
}
