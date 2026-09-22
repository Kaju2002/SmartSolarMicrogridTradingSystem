import PageHeader from '../../components/PageHeader'
import { useAuth } from '../../context/AuthContext'

export default function ProfilePage() {
  const { user } = useAuth()

  return (
    <div>
      <PageHeader
        title="Profile"
        subtitle="Your Backoffice account details for this session."
      />
      <div className="max-w-lg rounded-2xl border border-line bg-panel p-6">
        <div className="mb-5 flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-brand-950 text-lg font-semibold text-brand-400">
            {(user?.fullName || 'U').charAt(0).toUpperCase()}
          </div>
          <div>
            <p className="font-semibold text-ink">{user?.fullName || '—'}</p>
            <p className="text-xs text-muted">{user?.userType}</p>
          </div>
        </div>
        <div className="space-y-4 text-sm">
          <div>
            <p className="text-xs text-muted">Full name</p>
            <p className="font-medium text-ink">{user?.fullName || '—'}</p>
          </div>
          <div>
            <p className="text-xs text-muted">Role</p>
            <p className="font-medium text-ink">{user?.userType || '—'}</p>
          </div>
          <div>
            <p className="text-xs text-muted">User ID</p>
            <p className="break-all font-medium text-ink">
              {user?.userId || '—'}
            </p>
          </div>
        </div>
        <p className="mt-6 text-xs text-muted">
          Profile edit against the API can be added later.
        </p>
      </div>
    </div>
  )
}
