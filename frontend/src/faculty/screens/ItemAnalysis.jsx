import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Button from '../../shared/ui/Button.jsx'
import { formatRelative } from '../../shared/lib/time.js'
import { getItemAnalysis, listExams, runItemAnalysis } from '../api.js'

const SELECT = `h-8 px-2 rounded-[6px] border border-navy/14 bg-surface text-ink/66 font-medium
  max-w-[340px] focus-visible:outline-none focus:border-navy`

const GRID = 'grid grid-cols-[40px_minmax(200px,2fr)_96px_88px_116px_150px] min-w-[880px]'

const TONE = {
  ok: 'bg-transparent text-ink/50 border border-navy/18',
  warn: 'bg-navy/8 text-navy border border-navy/18',
  bad: 'bg-navy/16 text-navy-dark border border-navy/18',
}

function verdictFor(diff, disc) {
  if (disc == null) return { label: 'Too few sittings', tone: 'ok' }
  if (disc < 0) return { label: 'Broken · revise', tone: 'bad' }
  if (diff > 0.85) return { label: 'Too easy', tone: 'warn' }
  if (disc < 0.2) return { label: 'Weak discrimination', tone: 'warn' }
  if (diff < 0.35) return { label: 'Hard but sound', tone: 'ok' }
  return { label: 'Good item', tone: 'ok' }
}

export default function ItemAnalysis() {
  const { examId } = useParams()
  const navigate = useNavigate()

  const [exams, setExams] = useState([])
  const [report, setReport] = useState(null)
  const [error, setError] = useState(null)
  const [running, setRunning] = useState(false)

  useEffect(() => {
    let alive = true
    listExams()
      .then((all) => {
        if (!alive) return
        setExams(all)
        if (!examId) {
          if (all.length > 0) navigate(`/faculty/analysis/${all[0].id}`, { replace: true })
          else setError('There are no exams in the courses you staff yet.')
        }
      })
      .catch((err) => alive && setError(err.message))
    return () => { alive = false }
  }, [examId, navigate])

  useEffect(() => {
    if (!examId) return undefined
    let alive = true
    getItemAnalysis(examId)
      .then((d) => alive && setReport({ examId, view: d }))
      .catch((err) => alive && setError(err.message))
    return () => { alive = false }
  }, [examId])

  const run = async () => {
    setRunning(true)
    setError(null)
    try {
      setReport({ examId, view: await runItemAnalysis(examId) })
    } catch (err) {
      setError(err.message)
    } finally {
      setRunning(false)
    }
  }

  if (error) {
    return <main className="p-5 text-sm text-ink/66">Could not load item analysis: {error}</main>
  }

  // Tied to the exam it came from, so switching exams does not briefly show the previous one's
  // numbers -- and without an effect that clears state on every navigation.
  const shown = report?.examId === examId ? report.view : null
  const items = shown?.items ?? []

  return (
    <main className="p-5 text-sm">
      <div className="flex items-start justify-between gap-4 mb-4 flex-wrap">
        <div>
          <h1 className="m-0 mb-1 text-2xl font-semibold leading-tight tracking-[-0.02em]">Item analysis</h1>
          <p className="m-0 text-ink/55 max-w-[88ch]">
            Difficulty is the mean share of a question&apos;s marks that students earned, so a higher
            number means an easier question. Discrimination compares the strongest and weakest 27%
            of the cohort on that question alone — a negative value means the stronger students did
            worse, which usually means the item is miskeyed or ambiguous.
          </p>
        </div>
        <div className="flex items-center gap-3 shrink-0">
          <select
            value={examId ?? ''}
            onChange={(e) => navigate(`/faculty/analysis/${e.target.value}`)}
            aria-label="Exam to analyse"
            className={SELECT}
          >
            {exams.map((exam) => (
              <option key={exam.id} value={exam.id}>{exam.title}</option>
            ))}
          </select>
          <Button size="sm" onClick={run} disabled={running || !examId}>
            {running ? 'Working…' : 'Run analysis'}
          </Button>
        </div>
      </div>

      {shown && (
        <p className="m-0 mb-3 text-ink/42">
          {shown.sittings} submitted {shown.sittings === 1 ? 'sitting' : 'sittings'}
          {shown.computedAt ? ` · last computed ${formatRelative(shown.computedAt)}` : ''}
        </p>
      )}

      {!shown ? (
        <p className="text-ink/42">Loading item analysis…</p>
      ) : items.length === 0 ? (
        <div className="rounded-xl border border-navy/14 bg-surface p-5 max-w-[70ch]">
          <p className="m-0 text-ink/66">
            Nothing to report for this exam yet. Item statistics need submitted sittings with marks
            behind them — press <strong>Run analysis</strong> once students have sat it.
          </p>
        </div>
      ) : (
        <div className="border border-navy/14 rounded-xl bg-surface overflow-x-auto">
          <div className={`${GRID} gap-3 p-2 px-3 border-b border-navy/14 bg-paper text-xs
            font-medium tracking-wide uppercase text-ink/42`}>
            <span>Q</span><span>Question</span><span>Difficulty</span><span>Discrim.</span>
            <span>Distribution</span><span>Verdict</span>
          </div>

          {items.map((it) => {
            const diff = Number(it.difficulty)
            const disc = it.discrimination == null ? null : Number(it.discrimination)
            const v = verdictFor(diff, disc)
            const shares = it.options.map((o) => Number(o.share))
            const max = shares.length > 0 ? Math.max(...shares) : 0
            const barTone = diff > 0.85 || diff < 0.3 ? 'bg-navy/50' : 'bg-navy'
            return (
              <div
                key={it.questionId}
                className={`${GRID} gap-3 items-center min-h-8 py-2.5 px-3 border-b border-navy/7 hover:bg-paper`}
              >
                <span className="tabular-nums text-ink/42">{it.position}</span>
                <span className="overflow-hidden text-ellipsis whitespace-nowrap" title={it.text}>
                  {it.text}
                </span>
                <span className="flex items-center gap-2">
                  <span className="flex-1 h-1.5 rounded-full bg-navy/7">
                    <span className={`block h-full rounded-full ${barTone}`} style={{ width: `${diff * 100}%` }} />
                  </span>
                  <span className="tabular-nums text-ink/66 min-w-[30px]">{diff.toFixed(2)}</span>
                </span>
                <span className={`tabular-nums font-medium ${
                  disc == null ? 'text-ink/38' : disc < 0 ? 'text-navy-dark' : disc < 0.2 ? 'text-navy' : 'text-ink/66'
                }`}>
                  {disc == null ? '—' : `${disc > 0 ? '+' : ''}${disc.toFixed(2)}`}
                </span>
                <span className="flex gap-0.5 items-end h-[22px]">
                  {it.options.length === 0 ? (
                    <span className="text-ink/38 text-xs self-center">
                      {it.type === 'MCQ' ? '—' : 'no options'}
                    </span>
                  ) : it.options.map((o) => (
                    // Solid bar marks the CORRECT option, which the server tells us. It used to be
                    // whichever option came first, which with real data points at the wrong one.
                    <span
                      key={o.label}
                      title={`${o.label}: ${Math.round(Number(o.share) * 100)}%${o.correct ? ' (correct)' : ''}`}
                      className={`w-[7px] rounded-sm ${o.correct ? 'bg-navy' : 'bg-navy/18'}`}
                      style={{ height: Math.max(4, (Number(o.share) / max) * 22) }}
                    />
                  ))}
                </span>
                <span className={`justify-self-start text-xs font-medium px-2.5 py-[5px]
                  rounded-full whitespace-nowrap ${TONE[v.tone]}`}>
                  {v.label}
                </span>
              </div>
            )
          })}
        </div>
      )}

      <p className="mt-3 text-ink/42">
        Nothing is dropped automatically. Faculty decides whether to drop a broken question,
        rescale it, or keep it.
      </p>
    </main>
  )
}
