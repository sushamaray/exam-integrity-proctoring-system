import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getReviewQueue } from '../api.js'
import { TIER_PILL, TIER_LABEL, TIER_TEXT } from '../lib/tone.js'

const STAT_TONE = { plain: 'text-ink', review: 'text-navy', escalate: 'text-navy-dark' }

export default function ReviewQueue() {
  const [data, setData] = useState(null)

  useEffect(() => {
    let alive = true
    getReviewQueue().then((d) => alive && setData(d))
    return () => { alive = false }
  }, [])

  if (!data) {
    return <main className="p-5 text-sm text-ink/42">Loading review queue…</main>
  }

  return (
    <main className="p-5 text-sm">
      <h1 className="m-0 mb-1 text-2xl font-semibold leading-tight tracking-[-0.02em]">Review queue</h1>
      <p className="m-0 mb-4 text-ink/55">
        Every row is a score with its contributing signals. The system never outputs a verdict.
      </p>

      <div className="grid gap-3 mb-3" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))' }}>
        {data.stats.map((k) => (
          <div key={k.label} className="border border-navy/14 rounded-xl bg-surface p-3 px-4">
            <div className={`text-2xl font-semibold tracking-[-0.02em] ${STAT_TONE[k.tone]}`}>{k.value}</div>
            <div className="text-xs text-ink/42 mt-0.5">{k.label}</div>
          </div>
        ))}
      </div>

      <div className="border border-navy/14 rounded-xl bg-surface overflow-x-auto">
        <div className="grid grid-cols-[88px_minmax(120px,1fr)_60px_minmax(200px,1.6fr)_100px_120px]
          min-w-[900px] gap-3 p-2 px-3 border-b border-navy/14 bg-paper text-xs font-medium
          tracking-wide uppercase text-ink/42">
          <span>Session</span><span>Student</span><span>Risk</span>
          <span>Contributing signals</span><span>Tier</span><span>State</span>
        </div>
        {data.rows.map((r) => (
          <div
            key={r.id}
            className="grid grid-cols-[88px_minmax(120px,1fr)_60px_minmax(200px,1.6fr)_100px_120px]
              min-w-[900px] gap-3 items-center min-h-8 py-2.5 px-3 border-b border-navy/7 hover:bg-paper"
          >
            <Link to={`/faculty/session/${r.id}`} className="font-medium font-mono text-[13px]">{r.id}</Link>
            <span>{r.name}</span>
            <span className={`text-base font-semibold tabular-nums ${TIER_TEXT[r.tier]}`}>{r.risk}</span>
            <span className="text-ink/66">{r.signals}</span>
            <span className={`justify-self-start text-xs font-medium px-2.5 py-[5px] rounded-full whitespace-nowrap ${TIER_PILL[r.tier]}`}>
              {TIER_LABEL[r.tier]}
            </span>
            <span className="text-ink/55">{r.state}</span>
          </div>
        ))}
      </div>
    </main>
  )
}
