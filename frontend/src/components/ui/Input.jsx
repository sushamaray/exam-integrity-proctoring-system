import { cx } from './styles'

/**
 * Labelled text field.
 *
 * It owns the accessibility wiring -- the label's htmlFor, aria-invalid, and
 * pointing aria-describedby at the error text -- so pages cannot forget a piece
 * of it, which is what happened when each form hand-rolled its own inputs.
 *
 * `trailing` renders inside the field on the right (the password toggle uses it).
 */
export default function Input({
  id,
  label,
  error,
  trailing,
  hint,
  className = '',
  ...props
}) {
  const errorId = error ? `${id}-error` : undefined
  const hintId = hint ? `${id}-hint` : undefined
  const describedBy = [errorId, hintId].filter(Boolean).join(' ') || undefined

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium text-ink-800">
        {label}
      </label>

      <div className="relative">
        <input
          {...props}
          id={id}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          className={cx(
            'h-11 w-full rounded-lg border bg-white px-3.5 text-[0.9375rem] text-ink-900 shadow-card transition-colors duration-150 placeholder:text-ink-400',
            trailing && 'pr-11',
            error
              ? 'border-danger-500 hover:border-danger-600'
              : 'border-ink-200 hover:border-ink-300',
            className,
          )}
        />

        {trailing && (
          <div className="absolute inset-y-0 right-0 flex items-center pr-1.5">{trailing}</div>
        )}
      </div>

      {hint && !error && (
        <p id={hintId} className="text-xs text-ink-500">
          {hint}
        </p>
      )}

      {error && (
        <p id={errorId} className="text-sm text-danger-600">
          {error}
        </p>
      )}
    </div>
  )
}
