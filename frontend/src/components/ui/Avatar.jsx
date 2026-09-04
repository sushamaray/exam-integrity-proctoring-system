import { ROLE_TINT, cx, initialsFrom } from './styles'

/** Initials in a role-tinted disc. */
export default function Avatar({ name, role, size = 'md', className = '' }) {
  const sizes = {
    sm: 'h-8 w-8 text-xs',
    md: 'h-9 w-9 text-sm',
    lg: 'h-11 w-11 text-base',
  }

  return (
    <span
      aria-hidden="true"
      className={cx(
        'inline-flex shrink-0 items-center justify-center rounded-full font-semibold',
        sizes[size],
        ROLE_TINT[role] ?? 'bg-ink-100 text-ink-700',
        className,
      )}
    >
      {initialsFrom(name)}
    </span>
  )
}
