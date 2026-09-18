import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Button from '../../shared/ui/Button.jsx'
import { getSession, setFlagDecision } from '../api.js'

export default function SessionDetail() {
  const { id } = useParams()
  const [s, setS] = useState(null)
  const [error, setError] = useState(null)
  const [noteOpen, setNoteOpen] = useState(false)
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(() => getSession(id).then(setS).catch((err) => setError(err.message)), [id])

  useEffect(() => {
    let alive = true
    getSession(id)
      .then((d) => alive && setS(d))
      .catch((err) => alive && setError(err.message))
    return () => { alive = false }
  }, [id])

  const decide = async (decision) => {
    if (decision === 'UPHOLD' && !note.trim()) {
      setNoteOpen(true)
      setError('Upholding a flag needs a note explaining why.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      await setFlagDecision(id, decision, note)
      setNote('')
      setNoteOpen(false)
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (!s) {
    return <main className="p-5 text-sm text-ink/42">{error ?? 'Loading session…'}</main>
  }

  return (
    <main className="p-5 text-sm">
      <Link to={`/faculty/monitor/${s.examId}`} className="text-xs font-medium">← Live monitor</Link>

      <div className="flex items-end justify-between gap-4 flex-wrap my-2.5 mb-4">
        <div>
          <h1 className="m-0 mb-1 text-2xl font-semibold leading-tight tracking-[-0.02em]">
            {s.name} · session {s.id}
          </h1>
          <div className="text-ink/55">
            Risk {s.risk} · {s.tier}
            {s.raisedAt && ` · flag raised ${s.raisedAt}`} · state: {s.state}
          </div>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={!s.flagOpen} onClick={() => setNoteOpen((o) => !o)}>
            {noteOpen ? 'Hide note' : 'Add note'}
          </Button>
          <Button variant="outline" size="sm" disabled={!s.flagOpen || busy} onClick={() => decide('DISMISS')}>
            Dismiss
          </Button>
          <Button variant="primary" size="sm" disabled={!s.flagOpen || busy} onClick={() => decide('UPHOLD')}>
            Uphold
          </Button>
        </div>
      </div>

      {(noteOpen || error) && (
        <div className="border border-navy/14 rounded-xl bg-surface p-4 mb-3">
          {error && <p className="m-0 mb-2 text-navy-dark">{error}</p>}
          {noteOpen && (
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              rows={3}
              maxLength={2000}
              placeholder="Why this decision? The student can ask to see this."
              className="w-full p-2.5 rounded-[6px] border border-navy/14 bg-paper focus:outline-none focus:border-navy"
            />
          )}
        </div>
      )}

      <div className="border border-navy/14 rounded-xl bg-surface p-4 mb-3">
        <div className="flex items-center justify-between gap-3 mb-5">
          <span className="text-xs font-medium tracking-wide uppercase text-ink/42">
            Session timeline · {s.minutes} minutes
          </span>
          <span className="text-ink/55">
            {s.clusterWidth != null
              ? 'The band is one correlated cluster, not separate marks'
              : 'No correlated cluster in this sitting'}
          </span>
        </div>
        <div className="relative h-[72px] mx-2">
          <div className="absolute left-0 right-0 top-[34px] h-0.5 bg-navy/14" />
          {s.clusterWidth != null && (
            <>
              <div
                className="absolute top-[18px] h-[34px] rounded-[6px] bg-navy/12 border border-navy/28"
                style={{ left: `${s.clusterStart}%`, width: `${s.clusterWidth}%` }}
              />
              <div
                className="absolute top-[56px] text-xs font-medium text-navy-dark text-center whitespace-nowrap"
                style={{ left: `${s.clusterStart}%`, width: `${s.clusterWidth}%` }}
              >
                correlated cluster
              </div>
            </>
          )}
          {s.marks.map((m, i) => (
            <span
              key={i}
              title={m.label}
              className={`absolute rounded-full border-2 border-surface ${
                m.hot ? 'w-3 h-3 top-7 bg-navy' : 'w-2.5 h-2.5 top-[29px] bg-navy/18'
              }`}
              style={{ left: `${m.left}%` }}
            />
          ))}
          <span className="absolute left-0 top-0 text-xs text-ink/42 tabular-nums">{s.startLabel}</span>
          <span className="absolute right-0 top-0 text-xs text-ink/42 tabular-nums">{s.endLabel}</span>
        </div>
      </div>

      <div className="grid gap-3 items-start" style={{ gridTemplateColumns: 'minmax(0,1.4fr) minmax(280px,1fr)' }}>
        <div className="border border-navy/14 rounded-xl bg-surface overflow-x-auto">
          <div className="grid grid-cols-[minmax(0,1fr)_84px_96px] gap-3 p-2 px-4 border-b
            border-navy/14 bg-paper text-xs font-medium tracking-wide uppercase text-ink/42">
            <span>Signal breakdown</span><span>Weight</span><span>Confidence</span>
          </div>
          {s.tree.map((node, i) => (
            <div
              key={i}
              className="grid grid-cols-[minmax(0,1fr)_84px_96px] gap-3 items-center min-h-8 py-2 px-4
                border-b border-navy/7"
            >
              <span
                style={{ paddingLeft: node.depth * 18 }}
                className={node.depth === 0 ? 'font-semibold' : node.depth === 1 ? 'font-medium' : 'text-ink/66'}
              >
                {node.label}
              </span>
              <span className="tabular-nums text-ink/66">{node.weight}</span>
              <span className={`justify-self-start text-xs font-medium px-2 py-[3px] rounded-full ${
                node.conf === 'escalate' ? 'bg-navy/16 text-navy-dark'
                  : node.conf === 'high' ? 'bg-navy/16 text-navy-dark'
                  : node.conf === 'med' || node.conf === 'review' ? 'bg-navy/8 text-navy'
                  : ''
              }`}>
                {node.conf}
              </span>
            </div>
          ))}
          {s.tree.length === 1 && (
            <div className="p-4 text-ink/42">No signals recorded for this sitting.</div>
          )}
          <div className="p-4 text-ink/55">
            Composite tree — session risk decomposes into correlated clusters and per-question
            groups, which decompose into individual signals. This is the structure the scorer built.
          </div>
        </div>

        <div className="flex flex-col gap-3">
          <div className="border border-navy/14 rounded-xl bg-surface p-4">
            <div className="text-xs font-medium tracking-wide uppercase text-ink/42 mb-3">Raw events · latest 50</div>
            {s.rawEvents.length === 0 && <p className="m-0 text-ink/42">No events recorded.</p>}
            {s.rawEvents.map((e, i) => (
              <div key={i} className="flex gap-3 min-h-8 items-center border-b border-navy/7 last:border-b-0">
                <span className="tabular-nums text-ink/42 font-mono text-[13px]">{e.t}</span>
                <span className={e.hot ? 'text-navy-dark font-medium' : 'text-ink/66'}>{e.text}</span>
              </div>
            ))}
          </div>
          <div className="border border-navy/14 rounded-xl bg-surface p-4">
            <div className="text-xs font-medium tracking-wide uppercase text-ink/42 mb-2.5">Reviewer note</div>
            {s.note ? (
              <>
                <p className="m-0 mb-2 leading-relaxed">{s.note.text}</p>
                <div className="text-ink/42 text-xs">{s.note.meta}</div>
              </>
            ) : (
              <p className="m-0 text-ink/42">No decision recorded yet.</p>
            )}
          </div>
          <div className="border border-navy/14 rounded-xl bg-surface p-4">
            <div className="text-xs font-medium tracking-wide uppercase text-ink/42 mb-2.5">Cohort</div>
            <p className="m-0 leading-relaxed text-ink/66">{s.cohort}</p>
          </div>
        </div>
      </div>
    </main>
  )
}
