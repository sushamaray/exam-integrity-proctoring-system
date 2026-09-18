export default function Field({ label, error, hint, className = '', ...props }) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-xs font-medium text-ink/66">{label}</span>
      <input
        aria-invalid={error ? 'true' : undefined}
        className={`h-11 rounded-[6px] border bg-surface px-3.5 text-sm
          focus-visible:outline-2 focus-visible:outline-navy
          ${error ? 'border-navy/45' : 'border-navy/18'} ${className}`}
        {...props}
      />
      {error && <span className="text-xs font-medium text-navy-dark">{error}</span>}
      {!error && hint && <span className="text-xs text-ink/42">{hint}</span>}
    </label>
  )
}
