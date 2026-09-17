import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../../shared/ui/Button.jsx'
import { getAnswerDiff } from '../api.js'

function Segments({ segments }) {
  return (
    <p className="m-0 text-base leading-[1.7]">
      {segments.map((seg, i) =>
        seg.hot ? (
          <span key={i} className="bg-navy/12 border-b-2 border-navy/45">{seg.text}</span>
        ) : (
          <span key={i}>{seg.text}</span>
        )
      )}
    </p>
  )
}

export default function AnswerDiff() {
  const [d, setD] = useState(null)

  useEffect(() => {
    let alive = true
    getAnswerDiff().then((v) => alive && setD(v))
    return () => { alive = false }
  }, [])

  if (!d) {
    return <main className="p-5 text-sm text-ink/42">Loading answer diff…</main>
  }

  return (
    <main className="p-5 text-sm">
      <Link to="/faculty/collusion" className="text-xs font-medium">← Collusion graph</Link>

      <div className="flex items-end justify-between gap-4 flex-wrap my-2.5 mb-4">
        <div>
          <h1 className="m-0 mb-1 text-2xl font-semibold leading-tight tracking-[-0.02em]">{d.pairLabel}</h1>
          <div className="text-ink/55">{d.meta}</div>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm">Not collusion</Button>
          <Button variant="primary" size="sm">Refer both to committee</Button>
        </div>
      </div>

      <div className="grid gap-3 mb-3" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))' }}>
        {[d.left, d.right].map((side) => (
          <div key={side.name} className="border border-navy/14 rounded-xl bg-surface p-4">
            <div className="flex items-center justify-between gap-3 mb-3">
              <span className="font-medium">{side.name}</span>
              <span className="text-xs text-ink/42">{side.meta}</span>
            </div>
            <Segments segments={side.segments} />
          </div>
        ))}
      </div>

      <div className="border border-navy/18 rounded-xl bg-navy/5 p-3 px-4">
        <span className="font-medium text-navy">Matching mistake: </span>
        <span className="text-ink/70">{d.mistake}</span>
      </div>
    </main>
  )
}
