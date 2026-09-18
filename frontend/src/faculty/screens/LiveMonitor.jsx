import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getMonitor, listExams } from '../api.js'
import { cohortNote, monitorFilters, monitorHeader, monitorTile } from '../lib/adapters.js'
import { useRiskSocket } from '../hooks/useRiskSocket.js'
import { TIER_DOT, TIER_LABEL, TIER_TEXT, TIER_TILE_BORDER } from '../lib/tone.js'

const SORTERS = {
  risk: (a, b) => b.risk - a.risk,
  name: (a, b) => a.name.localeCompare(b.name),
  seat: (a, b) => a.id.localeCompare(b.id, undefined, { numeric: true }),
}
const SORT_LABEL = { risk: 'Risk ▾', name: 'Name ▾', seat: 'Seat ▾' }

const STATUS_ORDER = { LIVE: 0, SCHEDULED: 1, CLOSED: 2, DRAFT: 3 }
const STATUS_LABEL = { LIVE: 'Live', SCHEDULED: 'Scheduled', CLOSED: 'Closed', DRAFT: 'Draft' }

function orderExams(exams) {
  return exams.slice().sort((a, b) =>
    (STATUS_ORDER[a.status] ?? 9) - (STATUS_ORDER[b.status] ?? 9)
    || new Date(b.startsAt) - new Date(a.startsAt))
}

function examOption(e) {
  const now = e.status === 'LIVE' ? ` · ${e.inProgress} sitting now` : ''
  return `${STATUS_LABEL[e.status] ?? e.status} · ${e.title} · ${e.courseCode}${now}`
}

export default function LiveMonitor() {
  const { examId } = useParams()
  const navigate = useNavigate()
  const [exams, setExams] = useState(null)
  const [exam, setExam] = useState(null)
  const [tiles, setTiles] = useState({})
  const [error, setError] = useState(null)
  const [filter, setFilter] = useState('all')
  const [sort, setSort] = useState('risk')

  useEffect(() => {
    let alive = true
    listExams()
      .then((list) => {
        if (!alive) return
        const ordered = orderExams(list)
        setExams(ordered)
        if (!examId) {
          if (ordered.length > 0) navigate(`/faculty/monitor/${ordered[0].id}`, { replace: true })
          else setError('There are no exams in the courses you staff yet.')
        }
      })
      .catch((err) => alive && setError(err.message))
    return () => { alive = false }
  }, [examId, navigate])

  useEffect(() => {
    if (!examId) return
    let alive = true
    getMonitor(examId)
      .then((d) => {
        if (!alive) return
        setExam(d.exam)
        setTiles(Object.fromEntries(d.tiles.map((t) => [t.id, t])))
      })
      .catch((err) => alive && setError(err.message))
    return () => { alive = false }
  }, [examId])

  const onTiles = useCallback((changed) => {
    setTiles((prev) => {
      const next = { ...prev }
      for (const t of changed) {
        const tile = monitorTile(t)
        next[tile.id] = tile
      }
      return next
    })
  }, [])

  const current = exam != null && String(exam.examId) === String(examId)
  const connected = useRiskSocket(current ? examId : null, onTiles)

  if (error) {
    return <main className="p-5 text-sm text-ink/66">{error}</main>
  }
  if (!current) {
    return <main className="p-5 text-sm text-ink/42">Loading live monitor…</main>
  }

  const all = Object.values(tiles)
  const header = monitorHeader(exam, all)
  const filters = monitorFilters(all)
  const cohort = cohortNote(all)
  const visible = all
    .filter((t) => filter === 'all' || t.tier === filter)
    .sort(SORTERS[sort])

  return (
    <main className="p-5 text-sm">
      <div className="border border-navy/14 rounded-xl bg-surface p-3 px-4 mb-3">
        <div className="flex items-center justify-between gap-4 flex-wrap">
          <div className="flex items-baseline gap-4 flex-wrap">
            <span className="text-xl font-semibold tracking-tight">{header.code}</span>
            <span className="flex items-center gap-1.5 text-ink/66">
              <span className="w-[7px] h-[7px] rounded-full bg-navy live-dot" />
              {header.active} active
            </span>
            <span className="text-ink/66">{header.flagged} flagged</span>
            <span className="text-ink/42">
              {connected ? 'Live updates on' : 'Reconnecting…'} · {header.elapsed}
            </span>
          </div>
          <div className="flex items-center gap-2 flex-wrap">
            <select
              value={examId}
              onChange={(e) => navigate(`/faculty/monitor/${e.target.value}`)}
              aria-label="Exam to monitor"
              className="h-8 px-2 rounded-[6px] border border-navy/14 bg-surface text-ink/66 font-medium
                max-w-[320px] focus-visible:outline-none focus:border-navy"
            >
              {(exams ?? []).map((e) => <option key={e.id} value={e.id}>{examOption(e)}</option>)}
            </select>
            {filters.map((f) => (
              <button
                key={f.key}
                type="button"
                onClick={() => setFilter(f.key)}
                className={`font-inherit text-sm font-medium h-8 px-3 rounded-[6px] cursor-pointer border ${
                  filter === f.key
                    ? 'border-navy bg-navy/7 text-navy-dark'
                    : 'border-navy/14 bg-surface text-ink/66'
                }`}
              >
                {f.label}
              </button>
            ))}
            <label className="text-ink/42 flex items-center gap-1">
              Sort:
              <select
                value={sort}
                onChange={(e) => setSort(e.target.value)}
                className="bg-transparent text-ink/66 font-medium focus-visible:outline-none"
              >
                {Object.entries(SORT_LABEL).map(([k, label]) => (
                  <option key={k} value={k}>{label}</option>
                ))}
              </select>
            </label>
          </div>
        </div>

        <div className="flex items-center gap-3 flex-wrap mt-3 p-2.5 px-3 rounded-[6px]
          bg-navy/7 border border-navy/18">
          <span className="font-medium text-navy-dark">{cohort.headline}</span>
          <span className="text-navy-dark">{cohort.detail}</span>
        </div>
      </div>

      {visible.length === 0 && (
        <div className="p-6 mb-3 border border-navy/14 rounded-xl bg-surface text-ink/42">
          {all.length === 0
            ? 'No one has started this exam yet. Tiles appear here as students begin.'
            : 'No sittings match this filter.'}
        </div>
      )}

      <div className="grid gap-2 mb-3" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(148px, 1fr))' }}>
        {visible.map((t) => (
          <button
            key={t.id}
            type="button"
            onClick={() => navigate(`/faculty/session/${t.id}`)}
            className={`text-left cursor-pointer p-2.5 rounded-xl bg-surface border
              transition-all duration-[400ms] ${TIER_TILE_BORDER[t.tier]} ${t.live ? '' : 'opacity-60'}`}
          >
            <div className="flex items-center justify-between gap-2">
              <span className="font-medium">{t.name}</span>
              <span className={`w-[9px] h-[9px] rounded-full shrink-0 ${TIER_DOT[t.tier]}`} />
            </div>
            <div className="flex items-baseline gap-1.5 mt-2">
              <span className={`text-xl font-semibold tabular-nums tracking-tight ${TIER_TEXT[t.tier]}`}>
                {String(t.risk).padStart(2, '0')}
              </span>
              <span className={`text-xs font-medium ${TIER_TEXT[t.tier]}`}>{TIER_LABEL[t.tier]}</span>
            </div>
            <div className="text-xs text-ink/42 mt-1">{t.last}</div>
          </button>
        ))}
      </div>

      <div className="flex items-center gap-5 flex-wrap p-2.5 px-4 border border-navy/14
        rounded-xl bg-surface text-ink/55">
        <span className="flex items-center gap-2">
          <span className={`w-[9px] h-[9px] rounded-full ${TIER_DOT.clear}`} />Clear · auto-cleared at submission
        </span>
        <span className="flex items-center gap-2">
          <span className={`w-[9px] h-[9px] rounded-full ${TIER_DOT.review}`} />Review · a human decides
        </span>
        <span className="flex items-center gap-2">
          <span className={`w-[9px] h-[9px] rounded-full ${TIER_DOT.escalate}`} />Escalate · correlated cluster
        </span>
        <span className="ml-auto text-ink/42">
          Scores animate over 400ms. Tier is always paired with a label, never colour alone.
        </span>
      </div>
    </main>
  )
}
