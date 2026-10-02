const TIME = new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' })

export function describeWindow(exam) {
  return `Open until ${TIME.format(new Date(exam.endsAt))} · ${exam.durationMinutes} minutes`
}

/**
 * Which runner this exam opens in. A coding paper is sat in the editor at /student/coding; the
 * `coding` flag comes from the server, which knows whether the pool holds CODING questions.
 */
export function examPath(exam) {
  return exam.coding ? `/student/coding/${exam.id}` : `/student/exam/${exam.id}`
}
