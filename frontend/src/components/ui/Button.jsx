import { BUTTON_BASE, BUTTON_SIZES, BUTTON_VARIANTS, cx } from './styles'

/**
 * The app's only button.
 *
 * `loading` keeps the label in place and swaps in a spinner, so the control does
 * not change width mid-submit, and marks the button aria-busy for screen readers.
 */
export default function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  fullWidth = false,
  className = '',
  children,
  ...props
}) {
  return (
    <button
      {...props}
      aria-busy={loading || undefined}
      disabled={props.disabled || loading}
      className={cx(
        BUTTON_BASE,
        BUTTON_VARIANTS[variant],
        BUTTON_SIZES[size],
        fullWidth && 'w-full',
        className,
      )}
    >
      {loading && (
        <svg
          className="h-4 w-4 animate-spin"
          viewBox="0 0 16 16"
          fill="none"
          aria-hidden="true"
        >
          <circle cx="8" cy="8" r="6.5" stroke="currentColor" strokeWidth="2" opacity="0.25" />
          <path
            d="M14.5 8A6.5 6.5 0 0 0 8 1.5"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          />
        </svg>
      )}
      {children}
    </button>
  )
}
