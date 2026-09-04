/**
 * The product mark: a shield (integrity, protection) whose interior is drawn as
 * a checked-off timeline rather than the usual tick, nodding at the event stream
 * the system actually reasons over.
 *
 * currentColor throughout, so it takes the colour of whatever it sits in.
 */
export default function Logo({ size = 28, className = '' }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 32 32"
      fill="none"
      role="img"
      aria-label="Exam Integrity"
      className={className}
    >
      <path
        d="M16 2.5 27 6.4v9.3c0 6.6-4.4 12.5-11 14.3-6.6-1.8-11-7.7-11-14.3V6.4L16 2.5Z"
        className="fill-current opacity-15"
      />
      <path
        d="M16 2.5 27 6.4v9.3c0 6.6-4.4 12.5-11 14.3-6.6-1.8-11-7.7-11-14.3V6.4L16 2.5Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
      <path
        d="m11 16.2 3.2 3.2 6.6-6.6"
        stroke="currentColor"
        strokeWidth="2.1"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}
