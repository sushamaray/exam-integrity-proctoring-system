const VARIANTS = {
  primary: 'bg-navy text-white hover:bg-navy-dark',
  ghost:   'bg-transparent text-ink/55 hover:bg-navy/7 hover:text-navy',
  outline: 'bg-surface text-ink border border-navy/18 hover:bg-navy/7',
}

const SIZES = {
  md: 'h-11 px-5 text-base',
  sm: 'h-8 px-3 text-sm',
}

export default function Button({
  variant = 'primary', size = 'md', className = '', ...props
}) {
  return (
    <button
      type="button"
      className={`inline-flex items-center justify-center gap-2 rounded-[6px]
        font-medium transition-colors disabled:opacity-40
        disabled:cursor-not-allowed ${VARIANTS[variant]} ${SIZES[size]}
        ${className}`}
      {...props}
    />
  )
}
