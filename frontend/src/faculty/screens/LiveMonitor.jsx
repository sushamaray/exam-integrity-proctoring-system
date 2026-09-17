import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMonitor } from '../api.js'
import { useLiveFeed } from '../hooks/useLiveFeed.js'
import { TIER_DOT, TIER_LABEL, TIER_TEXT, TIER_TILE_BORDER } from '../lib/tone.js'

const EMPTY_TILES = []

const SORTERS = {
  risk: (a, b) => b.risk - a.risk,
  name: (a, b) => a.name.localeCompare(b.name),
  seat: (a, b) => a.id.localeCompare(b.id),
}
const SORT_LABEL = { risk: 'Risk ▾', name: 'Name ▾', seat: 'Seat ▾' }

export default function LiveMonitor() {
  const [data, setData] = useState(null)
  const [filter, setFilter] = useState('all')
  const [sort, setSort] = useState('risk')
  const navigate = useNavigate()

  useEffect(() => {
    let alive = true
    getMonitor().then((d) => alive && setData(d))
    return () => { alive = false }
  }, [])

  // Keep the seed reference stable across renders — a fresh `?? []` fallback
  // every render would look like a "changed seed" to useLiveFeed and loop.
  const seed = useMemo(() => data?.tiles ?? EMPTY_TILES, [data])
  const liveTiles = useLiveFeed(seed)

  if (!data) {
    return <main className="p-5 text-sm text-ink/42">Loading live monitor…</main>
  }

  const { header, filters, cohort } = data
  const visible = liveTiles
    .filter((t) => filter === 'all' || t.tier === filter)
    .slice()
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
            <span className="text-ink/42">Socket connected · {header.elapsed}</span>
          </div>
          <div className="flex items-center gap-2 flex-wrap">
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

      <div className="grid gap-2 mb-3" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(148px, 1fr))' }}>
        {visible.map((t) => (
          <button
            key={t.id}
            type="button"
            onClick={() => navigate(`/faculty/session/${t.id}`)}
            className={`text-left cursor-pointer p-2.5 rounded-xl bg-surface border
              transition-all duration-[400ms] ${TIER_TILE_BORDER[t.tier]}`}
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
