import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Button from '../../shared/ui/Button.jsx'
import { getCollusionGraph, listExams, runSimilarity } from '../api.js'
import { layoutGraph, pairCards } from '../lib/collusion.js'

function Graph({ layout, onEdge }) {
  const by = Object.fromEntries(layout.nodes.map((n) => [n.id, n]))
  return (
    <svg viewBox="0 0 740 380" className="w-full h-auto block">
      {layout.edges.map((e) => (
        <line
          key={e.pairId}
          x1={by[e.a].x} y1={by[e.a].y} x2={by[e.b].x} y2={by[e.b].y}
          stroke="#0B3F6A" strokeWidth={e.width} strokeOpacity={0.32}
          strokeDasharray={e.method === 'WRONG_ANSWER' ? '6 4' : undefined}
          className="cursor-pointer"
          onClick={() => onEdge(e.pairId)}
        >
          <title>{e.detail}</title>
        </line>
      ))}
      {layout.nodes.map((n) => (
        <g key={n.id}>
          <circle cx={n.x} cy={n.y} r={15} fill="#0B3F6A">
            <title>{n.name}</title>
          </circle>
          <text
            x={n.x} y={n.y + 32} textAnchor="middle"
            fill="rgba(10,10,11,0.7)"
            style={{ font: "12px 'Space Grotesk', sans-serif" }}
          >
            {n.label}
          </text>
        </g>
      ))}
    </svg>
  )
}

export default function CollusionGraph() {
  const { examId } = useParams()
  const navigate = useNavigate()
  const [exams, setExams] = useState(null)
  const [g, setG] = useState(null)
  const [error, setError] = useState(null)
  const [running, setRunning] = useState(false)

  useEffect(() => {
    let alive = true
    listExams()
      .then((list) => {
        if (!alive) return
        const newest = list.slice().sort((a, b) => new Date(b.startsAt) - new Date(a.startsAt))
        setExams(newest)
        if (!examId) {
          if (newest.length > 0) navigate(`/faculty/collusion/${newest[0].id}`, { replace: true })
          else setError('There are no exams in the courses you staff yet.')
        }
      })
      .catch((err) => alive && setError(err.message))
    return () => { alive = false }
  }, [examId, navigate])

  useEffect(() => {
    if (!examId) return
    let alive = true
    getCollusionGraph(examId)
      .then((d) => alive && setG(d))
      .catch((err) => alive && setError(err.message))
    return () => { alive = false }
  }, [examId, navigate])

  const run = async () => {
    setRunning(true)
    setError(null)
    try {
      setG(await runSimilarity(examId))
    } catch (err) {
      setError(err.message)
    } finally {
      setRunning(false)
    }
  }

  if (!g || String(g.examId) !== String(examId)) {
    return <main className="p-5 text-sm text-ink/42">{error ?? 'Loading collusion graph…'}</main>
  }

  const layout = layoutGraph(g)
  const cards = pairCards(g)
  const openDiff = (pairId) => navigate(`/faculty/diff/${pairId}`)

  return (
    <main className="p-5 text-sm">
      <div className="flex items-end justify-between gap-4 flex-wrap mb-4">
        <div>
          <h1 className="m-0 mb-1 text-2xl font-semibold leading-tight tracking-[-0.02em]">Collusion graph</h1>
          <p className="m-0 text-ink/55 max-w-[88ch]">
            {g.courseCode} · {g.title}. Written answers are compared by cosine similarity over TF-IDF
            vectors, per question; multiple choice by how unlikely the matching wrong answers are by
            chance. This catches what browser signals cannot: a clean session that shared answers.
          </p>
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <select
            value={examId}
            onChange={(e) => {
              setError(null)
              navigate(`/faculty/collusion/${e.target.value}`)
            }}
            aria-label="Exam to analyse"
            className="h-8 px-2 rounded-[6px] border border-navy/14 bg-surface text-ink/66 font-medium
              max-w-[340px] focus-visible:outline-none focus:border-navy"
          >
            {(exams ?? []).map((e) => (
              <option key={e.id} value={e.id}>
                {e.title} · {e.courseCode}{e.inProgress > 0 ? ` · ${e.inProgress} still sitting` : ''}
              </option>
            ))}
          </select>
          <Button variant={g.computedAt ? 'outline' : 'primary'} size="sm" disabled={running} onClick={run}>
            {running ? 'Running…' : g.computedAt ? 'Re-run analysis' : 'Run analysis'}
          </Button>
        </div>
      </div>

      {error && <p className="m-0 mb-3 text-navy-dark">{error}</p>}

      <div className="grid gap-3 items-start" style={{ gridTemplateColumns: 'minmax(0,1.5fr) minmax(300px,1fr)' }}>
        <div className="border border-navy/14 rounded-xl bg-surface p-4">
          <div className="flex items-center justify-between gap-3 flex-wrap mb-3">
            <span className="text-xs font-medium tracking-wide uppercase text-ink/42">
              {g.edges.length} pair{g.edges.length === 1 ? '' : 's'} above threshold
            </span>
            <span className="text-ink/55">Edge thickness is how far above its threshold a pair is</span>
          </div>
          {layout.nodes.length > 0 ? (
            <Graph layout={layout} onEdge={openDiff} />
          ) : (
            <div className="py-16 text-center text-ink/42">
              {g.computedAt ? 'No pair of sittings is similar enough to show.' : 'Not analysed yet.'}
            </div>
          )}
          <div className="flex gap-5 flex-wrap mt-3 text-ink/55">
            <span className="flex items-center gap-2">
              <span className="w-5 h-0.5 bg-navy/60" />Written answer
            </span>
            <span className="flex items-center gap-2">
              <span className="w-5 border-t-2 border-dashed border-navy/60" />Matching wrong answers
            </span>
            <span className="ml-auto">
              {layout.unconnected} sitting{layout.unconnected === 1 ? '' : 's'} with no edge, not drawn
            </span>
          </div>
        </div>

        <div className="flex flex-col gap-3">
          {cards.map((p) => (
            <div key={p.id} className="border border-navy/14 rounded-xl bg-surface p-3 px-4">
              <div className="flex items-baseline justify-between gap-3">
                <span className="font-medium">{p.pair}</span>
                <span className="text-lg font-semibold tabular-nums text-navy-dark">{p.score}</span>
              </div>
              <div className="text-ink/42 text-xs mt-1">{p.detail}</div>
              <p className="my-2.5 mb-3 leading-relaxed text-ink/66">{p.evidence}</p>
              <Link to={`/faculty/diff/${p.id}`} className="font-medium">Open diff →</Link>
            </div>
          ))}
        </div>
      </div>
    </main>
  )
}
