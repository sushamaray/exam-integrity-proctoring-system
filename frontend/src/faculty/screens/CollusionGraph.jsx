import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getCollusionGraph } from '../api.js'

function ForceGraph({ nodes, edges }) {
  const by = Object.fromEntries(nodes.map((n) => [n.id, n]))
  return (
    <svg viewBox="0 0 740 380" className="w-full h-auto block">
      {edges.map(([a, b, w], i) => (
        <line
          key={i}
          x1={by[a].x} y1={by[a].y} x2={by[b].x} y2={by[b].y}
          stroke="#0B3F6A" strokeWidth={(w - 0.7) * 20} strokeOpacity={0.32}
        />
      ))}
      {nodes.map((n) => (
        <g key={n.id}>
          <circle cx={n.x} cy={n.y} r={n.on ? 15 : 10} fill={n.on ? '#0B3F6A' : 'rgba(11,63,106,0.18)'} />
          <text
            x={n.x} y={n.y + (n.on ? 32 : 27)} textAnchor="middle"
            fill={n.on ? 'rgba(10,10,11,0.7)' : 'rgba(10,10,11,0.42)'}
            style={{ font: "12px 'Space Grotesk', sans-serif" }}
          >
            {n.id}
          </text>
        </g>
      ))}
    </svg>
  )
}

export default function CollusionGraph() {
  const [g, setG] = useState(null)

  useEffect(() => {
    let alive = true
    getCollusionGraph().then((d) => alive && setG(d))
    return () => { alive = false }
  }, [])

  if (!g) {
    return <main className="p-5 text-sm text-ink/42">Loading collusion graph…</main>
  }

  return (
    <main className="p-5 text-sm">
      <h1 className="m-0 mb-1 text-2xl font-semibold leading-tight tracking-[-0.02em]">Collusion graph</h1>
      <p className="m-0 mb-4 text-ink/55 max-w-[88ch]">
        Cosine similarity over TF-IDF vectors of normalised answer text, computed after submission.
        This catches what browser signals cannot: a clean session that shared answers.
      </p>

      <div className="grid gap-3 items-start" style={{ gridTemplateColumns: 'minmax(0,1.5fr) minmax(300px,1fr)' }}>
        <div className="border border-navy/14 rounded-xl bg-surface p-4">
          <div className="flex items-center justify-between gap-3 flex-wrap mb-3">
            <span className="text-xs font-medium tracking-wide uppercase text-ink/42">{g.question}</span>
            <span className="text-ink/55">Edge threshold {g.threshold} · edge thickness is the score</span>
          </div>
          <ForceGraph nodes={g.nodes} edges={g.edges} />
          <div className="flex gap-5 flex-wrap mt-3 text-ink/55">
            <span className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-navy" />Above threshold cluster
            </span>
            <span className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-navy/18" />Unconnected
            </span>
            <span className="ml-auto">
              <Link to="/faculty/diff/a/b">Click an edge to open the answer diff →</Link>
            </span>
          </div>
        </div>

        <div className="flex flex-col gap-3">
          {g.pairs.map((p) => (
            <div key={p.pair} className="border border-navy/14 rounded-xl bg-surface p-3 px-4">
              <div className="flex items-baseline justify-between gap-3">
                <span className="font-medium">{p.pair}</span>
                <span className="text-lg font-semibold tabular-nums text-navy-dark">{p.score}</span>
              </div>
              <div className="text-ink/42 text-xs mt-1">{p.detail}</div>
              <p className="my-2.5 mb-3 leading-relaxed text-ink/66">{p.evidence}</p>
              <Link to="/faculty/diff/a/b" className="font-medium">Open diff →</Link>
            </div>
          ))}
        </div>
      </div>
    </main>
  )
}
