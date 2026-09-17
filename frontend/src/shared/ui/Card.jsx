export default function Card({ tinted = false, className = '', ...props }) {
  return (
    <div
      className={`rounded-xl border ${
        tinted ? 'bg-navy/7 border-navy/18' : 'bg-surface border-navy/14'
      } ${className}`}
      {...props}
    />
  )
}
