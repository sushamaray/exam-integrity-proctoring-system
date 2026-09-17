const TONES = {
  neutral:  'bg-navy/7 text-ink/66',
  clear:    'bg-transparent text-ink/50 border border-navy/18',
  review:   'bg-navy/8 text-navy border border-navy/18',
  escalate: 'bg-navy/16 text-navy-dark border border-navy/18',
}

export default function Pill({ solid = false, tone = 'neutral', className = '', ...props }) {
  const toneClass = solid ? 'bg-navy text-white' : TONES[tone] ?? TONES.neutral
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-[5px]
        text-xs font-medium whitespace-nowrap ${toneClass} ${className}`}
      {...props}
    />
  )
}
