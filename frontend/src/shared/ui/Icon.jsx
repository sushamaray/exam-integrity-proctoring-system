const base = {
  width: 17, height: 17, viewBox: '0 0 20 20', fill: 'none',
  stroke: 'currentColor', strokeWidth: 1.5,
  strokeLinecap: 'round', strokeLinejoin: 'round',
}

export const IconGrid = (p) => (
  <svg {...base} {...p}>
    <rect x="2.5" y="3.5" width="6" height="5" rx="1" />
    <rect x="11.5" y="3.5" width="6" height="5" rx="1" />
    <rect x="2.5" y="11.5" width="6" height="5" rx="1" />
    <rect x="11.5" y="11.5" width="6" height="5" rx="1" />
  </svg>
)

export const IconCalendar = (p) => (
  <svg {...base} {...p}>
    <rect x="3" y="4.5" width="14" height="13" rx="2" />
    <path d="M3 8.5h14M7 2.5v3M13 2.5v3" />
  </svg>
)

export const IconFlag = (p) => (
  <svg {...base} {...p}>
    <path d="M5 3v14" />
    <path d="M5 3.8h9.5l-1.6 3.4 1.6 3.4H5z" />
  </svg>
)

export const IconClock = (p) => (
  <svg {...base} {...p}>
    <circle cx="10" cy="10" r="7.2" />
    <path d="M10 6v4.3l2.8 1.7" />
  </svg>
)

export const IconGraph = (p) => (
  <svg {...base} {...p}>
    <circle cx="5" cy="6" r="2.3" />
    <circle cx="14.5" cy="4.8" r="2.3" />
    <circle cx="9" cy="15" r="2.3" />
    <path d="m6.9 7.3 1.4 5.6M12.6 6.6 10.4 13" />
  </svg>
)

export const IconSplit = (p) => (
  <svg {...base} {...p}>
    <rect x="2.5" y="3.5" width="6.5" height="13" rx="1.5" />
    <rect x="11" y="3.5" width="6.5" height="13" rx="1.5" />
  </svg>
)

export const IconBars = (p) => (
  <svg {...base} {...p}>
    <path d="M3 17h14" />
    <rect x="4.5" y="9" width="3" height="5" />
    <rect x="9" y="5" width="3" height="9" />
    <rect x="13.5" y="11" width="3" height="3" />
  </svg>
)

export const IconDoc = (p) => (
  <svg {...base} {...p}>
    <path d="M5.5 2.5h6l3 3v11.5a1 1 0 0 1-1 1h-8a1 1 0 0 1-1-1v-13.5a1 1 0 0 1 1-1z" />
    <path d="M11.5 2.5v3h3M7 10h6M7 13h6M7 16h3" />
  </svg>
)

export const IconCode = (p) => (
  <svg {...base} {...p}>
    <path d="M7 6 2.8 10 7 14M13 6l4.2 4L13 14" />
  </svg>
)

export const IconBook = (p) => (
  <svg {...base} {...p}>
    <path d="M3 4.5c2-1 4.2-1 6.5 0v11.5c-2.3-1-4.5-1-6.5 0z" />
    <path d="M17 4.5c-2-1-4.2-1-6.5 0v11.5c2.3-1 4.5-1 6.5 0z" />
  </svg>
)

export const IconChart = (p) => (
  <svg {...base} {...p}>
    <path d="M3 17V3M3 17h14" />
    <path d="m5.5 13 3-4 2.7 2 4-5.5" />
  </svg>
)

export const IconShield = (p) => (
  <svg {...base} {...p}>
    <path d="M10 2.5 16.5 5v5.3c0 4-2.7 6.7-6.5 7.7-3.8-1-6.5-3.7-6.5-7.7V5z" />
    <path d="m7.3 10 1.9 1.9 3.5-3.9" />
  </svg>
)
