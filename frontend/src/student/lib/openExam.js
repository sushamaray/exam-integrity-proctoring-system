const TIME = new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' })

export function describeWindow(exam) {
  return `Open until ${TIME.format(new Date(exam.endsAt))} · ${exam.durationMinutes} minutes`
}
