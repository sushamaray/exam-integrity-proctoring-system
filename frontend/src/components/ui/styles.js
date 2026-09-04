/**
 * Shared class maps and helpers for the UI primitives.
 *
 * These live in a plain module rather than beside the components that use them
 * because eslint-plugin-react-refresh fails any .jsx file that exports a
 * non-component: a primitive file exports its component and nothing else.
 */

/** Joins class names, dropping anything falsy. */
export function cx(...values) {
  return values.filter(Boolean).join(' ')
}

export const BUTTON_BASE =
  'inline-flex items-center justify-center gap-2 rounded-lg font-medium whitespace-nowrap transition-colors duration-150 disabled:cursor-not-allowed disabled:opacity-55'

export const BUTTON_VARIANTS = {
  primary: 'bg-brand-600 text-white shadow-card hover:bg-brand-700 active:bg-brand-800',
  secondary:
    'border border-ink-200 bg-white text-ink-800 shadow-card hover:border-ink-300 hover:bg-ink-50',
  ghost: 'text-ink-600 hover:bg-ink-100 hover:text-ink-900',
}

export const BUTTON_SIZES = {
  sm: 'h-8 px-3 text-sm',
  md: 'h-10 px-4 text-sm',
  lg: 'h-11 px-5 text-[0.9375rem]',
}

export const PILL_TONES = {
  neutral: 'bg-ink-100 text-ink-700 ring-ink-200',
  info: 'bg-brand-50 text-brand-700 ring-brand-200',
  warn: 'bg-warn-50 text-warn-700 ring-warn-200',
  danger: 'bg-danger-50 text-danger-700 ring-danger-200',
  success: 'bg-success-50 text-success-700 ring-success-200',
}

/** A distinct tint per role, so the three demo accounts read apart at a glance. */
export const ROLE_TINT = {
  STUDENT: 'bg-brand-100 text-brand-700',
  FACULTY: 'bg-success-50 text-success-700',
  INVIGILATOR: 'bg-warn-50 text-warn-700',
}

export const ROLE_LABEL = {
  STUDENT: 'Student',
  FACULTY: 'Faculty',
  INVIGILATOR: 'Invigilator',
}

/**
 * First and last initial: "Dr. Farhana Islam" -> "FI". Honorifics are dropped so
 * the avatar never reads "DI".
 */
export function initialsFrom(fullName) {
  const words = String(fullName ?? '')
    .split(/\s+/)
    .filter((word) => word && !/^(dr|mr|mrs|ms|prof)\.?$/i.test(word))

  if (words.length === 0) return '?'
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase()
  return (words[0][0] + words[words.length - 1][0]).toUpperCase()
}
