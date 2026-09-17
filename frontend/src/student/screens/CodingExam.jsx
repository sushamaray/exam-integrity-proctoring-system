import { useEffect, useState } from 'react'
import Button from '../../shared/ui/Button.jsx'
import { getCodingProblem } from '../api.js'
import { useProctoring } from '../hooks/useProctoring.js'

export default function CodingExam() {
  const [problem, setProblem] = useState(null)
  useProctoring('cse4103-coding')

  useEffect(() => {
    let alive = true
    getCodingProblem().then((d) => alive && setProblem(d))
    return () => { alive = false }
  }, [])

  if (!problem) {
    return <main className="p-5 text-sm text-ink/42">Loading coding exam…</main>
  }

  return (
    <main className="p-5 text-sm flex flex-col h-screen">
      <div className="flex items-center justify-between gap-4 mb-3">
        <div>
          <h1 className="m-0 text-xl font-semibold tracking-[-0.015em]">{problem.title}</h1>
          <p className="m-0 mt-1 text-ink/55 max-w-[70ch]">{problem.statement}</p>
        </div>
        <div className="flex gap-2 shrink-0">
          <Button variant="outline" size="sm" disabled>Run</Button>
          <Button size="sm" disabled>Submit</Button>
        </div>
      </div>

      <div className="grid gap-3 flex-1 min-h-0" style={{ gridTemplateColumns: 'minmax(0,1.6fr) minmax(260px,1fr)' }}>
        <div className="relative">
          <pre className="font-mono text-sm bg-surface border border-navy/14 rounded-xl p-4
            overflow-auto select-none opacity-60 h-full m-0">
{problem.starter}
          </pre>
          <div className="absolute inset-0 grid place-items-center rounded-xl bg-paper/60 backdrop-blur-[1px]">
            <span className="rounded-full border border-navy/18 bg-surface px-3 py-1.5
              text-xs font-medium text-ink/55">
              Code execution — coming soon
            </span>
          </div>
        </div>

        <div className="border border-navy/14 rounded-xl bg-surface p-4 overflow-y-auto">
          <div className="text-xs font-medium tracking-wide uppercase text-ink/42 mb-3">Test cases</div>
          <div className="flex flex-col gap-2">
            {problem.tests.map((t) => (
              <div key={t.name} className="flex items-center justify-between gap-3 py-2 border-b border-navy/7 last:border-b-0">
                <span className="font-medium">{t.name}</span>
                <span className="text-ink/42">{t.detail}</span>
                <span className="w-16 h-4 rounded-[3px] bg-navy/7 border border-navy/14" />
              </div>
            ))}
          </div>
        </div>
      </div>
    </main>
  )
}
