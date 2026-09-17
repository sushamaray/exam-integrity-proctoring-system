import { useEffect, useState } from 'react'

export function useLiveFeed(seed) {
  const [tiles, setTiles] = useState(seed)
  const [prevSeed, setPrevSeed] = useState(seed)

  // Adjusting state during render (not in an effect) when the seed prop
  // changes — the pattern React recommends over a sync setState-in-effect.
  if (seed !== prevSeed) {
    setPrevSeed(seed)
    setTiles(seed)
  }

  useEffect(() => {
    const t = setInterval(() => {
      setTiles((prev) =>
        prev.map((tile) => {
          if (Math.random() > 0.12) return tile
          const risk = Math.max(0, Math.min(100, tile.risk + (Math.random() * 18 - 6)))
          return {
            ...tile,
            risk: Math.round(risk),
            tier: risk > 60 ? 'escalate' : risk > 25 ? 'review' : 'clear',
            last: ['paste', 'blur', 'idle 22s', 'fullscreen exit'][
              Math.floor(Math.random() * 4)
            ],
          }
        })
      )
    }, 1800)
    return () => clearInterval(t)
  }, [])

  return tiles
}
