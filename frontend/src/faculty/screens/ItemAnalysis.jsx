import { useEffect, useState } from 'react'
import { getItemAnalysis } from '../api.js'

const TONE = {
  ok:   { pill: 'bg-transparent text-ink/50 border border-navy/18', disc: 'text-ink/66', bar: 'bg-navy' },
  warn: { pill: 'bg-navy/8 text-navy border border-navy/18', disc: 'text-navy', bar: 'bg-navy/50' },
  bad:  { pill: 'bg-navy/16 text-navy-dark border border-navy/18', disc: 'text-navy-dark', bar: 'bg-navy/50' },
}

function verdictFor(diff, disc) {
  if (disc < 0) return { label: 'Broken · revise', tone: 'bad' }
  if (diff > 0.85) return { label: 'Too easy', tone: 'warn' }
  if (disc < 0.2) return { label: 'Weak discrimination', tone: 'warn' }
  if (diff < 0.35) return { label: 'Hard but sound', tone: 'ok' }
  return { label: 'Good item', tone: 'ok' }
}

export default function ItemAnalysis() {
  const [items, setItems] = useState(null)

  useEffect(() => {
    let alive = true
    getItemAnalysis().then((d) => alive && setItems(d.items))
    return () => { alive = false }
  }, [])

  if (!items) {
    return <main className="p-5 text-sm text-ink/42">Loading item analysis…</main>
  }

  return (
    <main className="p-5 text-sm">
      <h1 className="m-0 mb-1 text-2xl font-semibold leading-tight tracking-[-0.02em]">Item analysis</h1>
      <p className="m-0 mb-4 text-ink/55 max-w-[88ch]">
        Difficulty index is the fraction who answered correctly. Discrimination index compares the
        top and bottom thirds — a negative value means strong students got it wrong more often,
        which usually means the question is broken.
      </p>

      <div className="border border-navy/14 rounded-xl bg-surface overflow-x-auto">
        <div className="grid grid-cols-[40px_minmax(200px,2fr)_96px_88px_116px_150px] min-w-[880px]
          gap-3 p-2 px-3 border-b border-navy/14 bg-paper text-xs font-medium tracking-wide
          uppercase text-ink/42">
          <span>Q</span><span>Question</span><span>Difficulty</span><span>Discrim.</span>
          <span>Distribution</span><span>Verdict</span>
        </div>

        {items.map((it) => {
          const v = verdictFor(it.diff, it.disc)
          const t = TONE[v.tone]
          const max = Math.max(...it.dist)
          const barTone = it.diff > 0.85 || it.diff < 0.3 ? 'bg-navy/50' : 'bg-navy'
          return (
            <div
              key={it.n}
              className="grid grid-cols-[40px_minmax(200px,2fr)_96px_88px_116px_150px] min-w-[880px]
                gap-3 items-center min-h-8 py-2.5 px-3 border-b border-navy/7 hover:bg-paper"
            >
              <span className="tabular-nums text-ink/42">{it.n}</span>
              <span className="overflow-hidden text-ellipsis whitespace-nowrap">{it.text}</span>
              <span className="flex items-center gap-2">
                <span className="flex-1 h-1.5 rounded-full bg-navy/7">
                  <span className={`block h-full rounded-full ${barTone}`} style={{ width: `${it.diff * 100}%` }} />
                </span>
                <span className="tabular-nums text-ink/66 min-w-[30px]">{it.diff.toFixed(2)}</span>
              </span>
              <span className={`tabular-nums font-medium ${it.disc < 0 ? 'text-navy-dark' : it.disc < 0.2 ? 'text-navy' : 'text-ink/66'}`}>
                {it.disc > 0 ? '+' : ''}{it.disc.toFixed(2)}
              </span>
              <span className="flex gap-0.5 items-end h-[22px]">
                {it.dist.map((v2, i) => (
                  <span
                    key={i}
                    className={`w-[7px] rounded-sm ${i === 0 ? 'bg-navy' : 'bg-navy/18'}`}
                    style={{ height: Math.max(4, (v2 / max) * 22) }}
                  />
                ))}
              </span>
              <span className={`justify-self-start text-xs font-medium px-2.5 py-[5px] rounded-full whitespace-nowrap ${t.pill}`}>
                {v.label}
              </span>
            </div>
          )
        })}
      </div>
      <p className="mt-3 text-ink/42">
        Nothing is dropped automatically. Faculty decides whether to drop a broken question,
        rescale it, or keep it.
      </p>
    </main>
  )
}
