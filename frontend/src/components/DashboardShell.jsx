import { BRAND } from '../brand'
import { useAuth } from '../auth/authContext'
import Avatar from './ui/Avatar'
import Button from './ui/Button'
import Logo from './ui/Logo'
import StatusPill from './ui/StatusPill'
import { ROLE_LABEL } from './ui/styles'

/**
 * The frame every signed-in screen sits in.
 *
 * For update 1 its job is to make the checkpoint's actual claim visible: the
 * session is real, it came from the server, and it knows which role you hold.
 * Once exam and review screens exist they slot into the same header and gutter.
 */
export default function DashboardShell({ eyebrow, title, intro }) {
  const { user, logout } = useAuth()
  const roleLabel = ROLE_LABEL[user.role] ?? user.role

  return (
    <div className="min-h-full bg-ink-50">
      <header className="sticky top-0 z-10 border-b border-ink-200 bg-white/80 backdrop-blur-md">
        <div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-6 py-3">
          <div className="flex items-center gap-2.5">
            <Logo size={26} className="text-brand-600" />
            <span className="text-sm font-semibold tracking-tight text-ink-900">{BRAND.name}</span>
          </div>

          <div className="flex items-center gap-3">
            <div className="hidden text-right sm:block">
              <p className="text-sm leading-tight font-medium text-ink-900">{user.fullName}</p>
              <p className="text-xs text-ink-500">{roleLabel}</p>
            </div>
            <Avatar name={user.fullName} role={user.role} />
            <Button variant="ghost" size="sm" onClick={logout}>
              Sign out
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-10 sm:py-14">
        <p className="text-xs font-semibold tracking-wider text-brand-600 uppercase">{eyebrow}</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight text-ink-900">{title}</h1>
        <p className="mt-2.5 max-w-2xl text-ink-500">{intro}</p>

        {/* Three facts the server told us, rather than three decorative boxes.
            This is the evidence that update 1 works. */}
        <dl className="mt-8 grid grid-cols-1 gap-3 sm:grid-cols-3">
          <div className="rounded-xl border border-ink-200 bg-white p-4 shadow-card">
            <dt className="text-xs font-medium tracking-wide text-ink-500 uppercase">Account</dt>
            <dd className="mt-1.5 truncate text-sm font-medium text-ink-900">{user.fullName}</dd>
            <dd className="truncate text-xs text-ink-500">{user.email}</dd>
          </div>

          <div className="rounded-xl border border-ink-200 bg-white p-4 shadow-card">
            <dt className="text-xs font-medium tracking-wide text-ink-500 uppercase">Role</dt>
            <dd className="mt-2">
              <StatusPill tone="info">{roleLabel}</StatusPill>
            </dd>
          </div>

          <div className="rounded-xl border border-ink-200 bg-white p-4 shadow-card">
            <dt className="text-xs font-medium tracking-wide text-ink-500 uppercase">Session</dt>
            <dd className="mt-2">
              <StatusPill tone="success" dot>
                Active
              </StatusPill>
            </dd>
            <dd className="mt-1.5 text-xs text-ink-500">Server-side, survives a refresh</dd>
          </div>
        </dl>
      </main>
    </div>
  )
}
