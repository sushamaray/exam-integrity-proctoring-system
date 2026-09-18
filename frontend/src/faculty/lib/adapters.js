export const DETECTOR_LABEL = {
  LARGE_PASTE: 'Large paste',
  TAB_SWITCH: 'Left the exam tab',
  FOCUS_LOSS: 'Focus loss',
  FULLSCREEN_EXIT: 'Fullscreen exit',
  HEARTBEAT_GAP: 'Heartbeat gap',
  IDLE_THEN_BURST: 'Idle, then burst',
  CORRELATED_CLUSTER: 'Correlated cluster',
}

const EVENT_TEXT = {
  PASTE: (p) => `paste · ${p.charCount ?? '?'} chars`,
  COPY: () => 'copy',
  TAB_HIDDEN: () => 'tab hidden',
  TAB_VISIBLE: () => 'tab visible again',
  WINDOW_BLUR: () => 'window blur',
  WINDOW_FOCUS: () => 'focus regained',
  FS_ENTER: () => 'entered fullscreen',
  FS_EXIT: () => 'left fullscreen',
  HEARTBEAT: () => 'heartbeat',
  ANSWER: () => 'answer saved',
}

const FLAG_STATE = {
  RAISED: 'Raised',
  UNDER_REVIEW: 'Under review',
  UPHELD: 'Upheld',
  DISMISSED: 'Dismissed',
}

const SESSION_STATE = {
  NOT_STARTED: 'Not started',
  IN_PROGRESS: 'In progress',
  SUBMITTED: 'Submitted',
  AUTO_SUBMITTED: 'Auto-submitted',
  VOIDED: 'Voided',
}

const EXAM_STATE = { LIVE: 'Live', SCHEDULED: 'Scheduled', DRAFT: 'Draft', CLOSED: 'Completed' }

const CLOCK = new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit', hour12: false })
const CLOCK_S = new Intl.DateTimeFormat(undefined, {
  hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
})
const DAY = new Intl.DateTimeFormat(undefined, { weekday: 'short', day: 'numeric', month: 'short' })

export const hhmm = (iso) => (iso ? CLOCK.format(new Date(iso)) : '—')
export const hhmmss = (iso) => (iso ? CLOCK_S.format(new Date(iso)) : '—')
const tier = (t) => (t ?? 'CLEAR').toLowerCase()
const detectorLabel = (d) => DETECTOR_LABEL[d] ?? d

export function examRow(e) {
  const live = e.status === 'LIVE'
  const closed = e.status === 'CLOSED'
  return {
    id: e.id,
    name: e.title,
    when: `${e.courseCode} · ${DAY.format(new Date(e.startsAt))} · ${hhmm(e.startsAt)} · ${e.durationMinutes} min`,
    state: EXAM_STATE[e.status] ?? e.status,
    pool: `${e.poolSize} / ${e.questionsToDraw ?? e.poolSize}`,
    students: live ? `${e.sittings} · ${e.inProgress} live` : `${e.sittings}`,
    flags: e.openFlags === 0 ? 'None open' : `${e.openFlags} open`,
    action: live ? 'Monitor' : closed ? 'Collusion' : '',
    to: live ? `/faculty/monitor/${e.id}` : `/faculty/collusion/${e.id}`,
  }
}

export function monitorTile(t) {
  let last = 'No signals'
  if (t.state !== 'IN_PROGRESS') last = SESSION_STATE[t.state] ?? t.state
  else if (t.lastSignal) last = `${detectorLabel(t.lastSignal)} · ${hhmm(t.lastSignalAt)}`
  return {
    id: String(t.sessionId),
    name: t.student,
    risk: Math.round(Number(t.score)),
    tier: tier(t.tier),
    last,
    live: t.state === 'IN_PROGRESS',
    flagged: t.flagStatus != null,
    percentile: t.cohortPercentile == null ? null : Number(t.cohortPercentile),
  }
}

export function monitorHeader(exam, tiles) {
  const minutes = Math.max(0, Math.round((Date.now() - new Date(exam.startsAt).getTime()) / 60000))
  return {
    code: `${exam.courseCode} · ${exam.title}`,
    active: tiles.filter((t) => t.live).length,
    flagged: tiles.filter((t) => t.flagged).length,
    elapsed: `${minutes}m since the window opened`,
  }
}

export function monitorFilters(tiles) {
  const count = (t) => tiles.filter((x) => x.tier === t).length
  return [
    { key: 'all', label: `All · ${tiles.length}` },
    { key: 'review', label: `Review · ${count('review')}` },
    { key: 'escalate', label: `Escalate · ${count('escalate')}` },
  ]
}

export function cohortNote(tiles) {
  const live = tiles.filter((t) => t.live)
  if (live.length === 0) return { headline: 'No live sittings', detail: 'Tiles show final scores.' }
  const sorted = live.map((t) => t.risk).sort((a, b) => a - b)
  const median = sorted[Math.floor(sorted.length / 2)]
  const escalated = live.filter((t) => t.tier === 'escalate').length
  return {
    headline: `Cohort median risk ${median}`,
    detail: escalated === 0
      ? 'Nobody is in a correlated cluster right now.'
      : `${escalated} of ${live.length} live sittings are in a correlated cluster.`,
  }
}

const confidenceBand = (c) => (c >= 0.8 ? 'high' : c >= 0.5 ? 'med' : 'low')

function groupLabel(row) {
  const factor = `×${Number(row.multiplier).toFixed(2)}`
  if (row.kind === 'CLUSTER') return `Correlated cluster · ${hhmmss(row.start)}–${hhmmss(row.end)} (${factor})`
  if (row.kind === 'RULE') return 'Exam rule broken (counted in full)'
  return `${row.label} · isolated (${factor})`
}

export function sessionDetail(d) {
  const start = new Date(d.startedAt).getTime()
  const end = new Date(d.submittedAt ?? d.expiresAt).getTime()
  const span = Math.max(1, end - start)
  const pct = (iso) => Math.min(100, Math.max(0, ((new Date(iso).getTime() - start) / span) * 100))

  const rows = d.breakdown ?? []
  const clusters = rows.filter((r) => r.kind === 'CLUSTER' && r.start)

  const hot = new Set()
  let inCluster = false
  for (const r of rows) {
    if (r.depth === 0) inCluster = r.kind === 'CLUSTER'
    else if (inCluster && r.kind === 'SIGNAL') hot.add(r)
  }
  const strongest = clusters.slice().sort((a, b) => Number(b.score) - Number(a.score))[0]

  const tree = [
    { label: 'Session risk', weight: String(Math.round(Number(d.score))), conf: tier(d.tier), depth: 0 },
    ...rows.map((r) => r.kind === 'SIGNAL'
      ? {
          label: `${detectorLabel(r.detector)} · ${hhmmss(r.at)}`,
          weight: Number(r.weight).toFixed(2),
          conf: confidenceBand(Number(r.confidence)),
          depth: r.depth + 1,
        }
      : { label: groupLabel(r), weight: Number(r.score).toFixed(1), conf: '', depth: r.depth + 1 }),
  ]

  return {
    id: d.sessionId,
    examId: d.examId,
    name: d.student,
    risk: Math.round(Number(d.score)),
    tier: tier(d.tier),
    raisedAt: d.flag ? hhmm(d.flag.createdAt) : null,
    state: d.flag ? FLAG_STATE[d.flag.status] : (SESSION_STATE[d.state] ?? d.state),
    flagOpen: d.flag != null && (d.flag.status === 'RAISED' || d.flag.status === 'UNDER_REVIEW'),
    minutes: Math.round(span / 60000),
    startLabel: hhmm(d.startedAt),
    endLabel: hhmm(d.submittedAt ?? d.expiresAt),
    marks: rows.filter((r) => r.kind === 'SIGNAL').map((r) => ({
      label: `${detectorLabel(r.detector)} ${hhmmss(r.at)}`,
      left: pct(r.at),
      hot: hot.has(r),
    })),
    clusterStart: strongest ? pct(strongest.start) : null,
    clusterWidth: strongest ? Math.max(1, pct(strongest.end) - pct(strongest.start)) : null,
    tree,
    rawEvents: (d.events ?? []).map((e) => ({
      t: hhmmss(e.occurredAt),
      text: (EVENT_TEXT[e.type] ?? (() => e.type.toLowerCase()))(e.payload ?? {}),
      hot: e.hot,
    })),
    note: d.flag?.reviewerNote
      ? { text: d.flag.reviewerNote, meta: `${d.flag.reviewedBy ?? ''} · ${hhmm(d.flag.reviewedAt)}` }
      : null,
    cohort: d.cohortPercentile == null
      ? 'Not yet ranked against the cohort.'
      : `Scores above ${Math.round(Number(d.cohortPercentile))}% of this exam's sittings.`,
  }
}

export function reviewQueue(rows) {
  const mapped = rows.map((r) => ({
    id: String(r.sessionId),
    name: r.student,
    risk: r.score == null ? '—' : Math.round(Number(r.score)),
    tier: tier(r.tier),
    signals: r.signals.length ? r.signals.map(detectorLabel).join(' · ') : detectorLabel(r.reason),
    state: FLAG_STATE[r.status] ?? r.status,
  }))
  const count = (pred) => mapped.filter(pred).length
  return {
    rows: mapped,
    stats: [
      { value: mapped.length, label: 'Open flags', tone: 'plain' },
      { value: count((r) => r.tier === 'escalate'), label: 'Escalate', tone: 'escalate' },
      { value: count((r) => r.tier === 'review'), label: 'Review', tone: 'review' },
      { value: count((r) => r.state === 'Under review'), label: 'Being reviewed', tone: 'plain' },
    ],
  }
}
