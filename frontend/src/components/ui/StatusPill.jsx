import { PILL_TONES, cx } from './styles'

/**
 * A small state chip. The real risk tiers (auto-clear / review / escalate) map
 * onto the same tones once scoring exists.
 */
export default function StatusPill({ tone = 'neutral', dot = false, children }) {
  return (
    <span
      className={cx(
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ring-1 ring-inset',
        PILL_TONES[tone],
      )}
    >
      {dot && <span aria-hidden="true" className="h-1.5 w-1.5 rounded-full bg-current" />}
      {children}
    </span>
  )
}
