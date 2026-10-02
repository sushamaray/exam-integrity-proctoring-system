import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Button from '../../shared/ui/Button.jsx'
import Card from '../../shared/ui/Card.jsx'
import { getBriefing, listOpenExams } from '../api.js'
import { describeWindow, examPath } from '../lib/openExam.js'

export default function Briefing() {
  const { examId } = useParams()
  const [data, setData] = useState(null)
  const [openExam, setOpenExam] = useState(null)

  useEffect(() => {
    let alive = true
    getBriefing().then((d) => alive && setData(d))

    listOpenExams()
      .then((open) => {
        if (!alive) return
        setOpenExam(open.find((e) => String(e.id) === examId) ?? open[0] ?? null)
      })
      .catch(() => {})
    return () => { alive = false }
  }, [examId])

  if (!data) {
    return <main className="p-5 text-sm text-ink/42">Loading briefing…</main>
  }

  const { readiness, recorded, notRecorded } = data

  return (
    <main className="p-5 text-sm max-w-[820px]">
      <h1 className="m-0 mb-1 text-2xl font-semibold leading-tight tracking-[-0.02em]">Exam briefing</h1>
      <p className="m-0 mb-4 text-ink/55">
        {openExam ? `${openExam.title} · ${describeWindow(openExam)}` : 'No exam is open right now'}
      </p>

      <Card className="p-4 mb-4">
        <div className="text-xs font-medium tracking-wide uppercase text-ink/42 mb-3">System check</div>
        <div className="flex flex-col gap-2">
          {readiness.map((c) => (
            <div key={c.label} className="flex items-center justify-between gap-3 py-1.5 border-b border-navy/7 last:border-b-0">
              <span>
                <span className="font-medium">{c.label}</span>
                <span className="text-ink/42 ml-2">{c.detail}</span>
              </span>
              <span className={`text-xs font-medium px-2.5 py-[5px] rounded-full border ${
                c.ok ? 'bg-navy/7 border-navy/18 text-navy' : 'bg-navy/5 border-navy/18 text-navy'
              }`}>
                {c.ok ? 'Pass' : 'Check'}
              </span>
            </div>
          ))}
        </div>
      </Card>

      <div className="grid gap-4 mb-4" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))' }}>
        <Card className="p-4">
          <div className="text-xs font-medium tracking-wide uppercase text-ink/42 mb-3">Recorded during the exam</div>
          <ul className="m-0 pl-5 flex flex-col gap-1.5 text-ink/66">
            {recorded.map((r) => <li key={r}>{r}</li>)}
          </ul>
        </Card>
        <Card className="p-4">
          <div className="text-xs font-medium tracking-wide uppercase text-ink/42 mb-3">Never recorded</div>
          <ul className="m-0 pl-5 flex flex-col gap-1.5 text-ink/66">
            {notRecorded.map((r) => <li key={r}>{r}</li>)}
          </ul>
        </Card>
      </div>

      <div className="flex items-center justify-between gap-4 flex-wrap">
        <p className="m-0 text-ink/55 max-w-[46ch]">
          Once you start, the timer cannot be paused. Leaving fullscreen or switching tabs is logged,
          not blocked — flags are reviewed by a person before anything happens to your grade.
        </p>
        {openExam ? (
          <Link to={examPath(openExam)}>
            <Button>{openExam.sessionState === 'IN_PROGRESS' ? 'Resume exam' : 'Start exam'}</Button>
          </Link>
        ) : (
          <Button disabled>No exam open</Button>
        )}
      </div>
    </main>
  )
}
