import PageHeader from '../../components/PageHeader'

type Props = {
  title: string
  subtitle: string
  nextStep: string
}

/** Temporary operator page until the feature is wired. */
export default function OperatorPlaceholder({
  title,
  subtitle,
  nextStep,
}: Props) {
  return (
    <div>
      <PageHeader title={title} subtitle={subtitle} />
      <div className="rounded-2xl border border-dashed border-line bg-panel px-6 py-10 text-center">
        <p className="text-sm font-medium text-ink">Coming next</p>
        <p className="mx-auto mt-2 max-w-md text-sm text-muted">{nextStep}</p>
      </div>
    </div>
  )
}
