import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../../shared/ui/Button.jsx'
import Card from '../../shared/ui/Card.jsx'
import { getDashboard } from '../api.js'

export default function Dashboard() {
  const [data, setData] = useState(null)

  useEffect(() => {
    let alive = true
    getDashboard().then((d) => alive && setData(d))
    return () => { alive = false }
  }, [])

  if (!data) {
    return <main className="p-5 text-sm text-ink/42">Loading dashboard…</main>
  }

  const { student, upcoming, past, weak } = data

  return (
    <main className="p-5 text-sm">
      <h1 className="m-0 mb-1 text-2xl font-semibold leading-tight tracking-[-0.02em]">
        Welcome back, {student.name.split(' ')[0]}
      </h1>
      <p className="m-0 mb-4 text-ink/55">{student.id}</p>

      <Card tinted className="p-5 mb-4">
        <div className="flex items-center justify-between gap-4 flex-wrap">
          <div>
            <span className="text-xs font-medium tracking-wide uppercase text-navy-dark">{upcoming.code}</span>
            <h2 className="m-0 mt-1 text-xl font-semibold tracking-[-0.015em]">{upcoming.title}</h2>
            <p className="m-0 mt-1 text-ink/66">{upcoming.when}</p>
          </div>
          <div className="flex flex-col items-end gap-2">
            <span className="text-xs font-medium px-2.5 py-[5px] rounded-full bg-navy/16 text-navy-dark border border-navy/18">
              {upcoming.status}
            </span>
            <Link to="/student/briefing"><Button size="sm">Go to briefing</Button></Link>
          </div>
        </div>
      </Card>

      <div className="grid gap-4" style={{ gridTemplateColumns: 'minmax(0,1.6fr) minmax(240px,1fr)' }}>
        <Card className="overflow-x-auto">
          <div className="grid grid-cols-[minmax(160px,2fr)_120px_100px_100px] gap-3 p-2 px-4
            border-b border-navy/14 bg-paper text-xs font-medium tracking-wide uppercase text-ink/42">
            <span>Exam</span><span>Date</span><span>Score</span><span />
          </div>
          {past.map((x) => (
            <div key={x.id} className="grid grid-cols-[minmax(160px,2fr)_120px_100px_100px] gap-3
              items-center min-h-8 py-2.5 px-4 border-b border-navy/7 hover:bg-paper">
              <span className="font-medium">{x.name}</span>
              <span className="text-ink/66">{x.date}</span>
              <span className="tabular-nums text-ink/66">{x.score}</span>
              <Link to="/student/results" className="font-medium justify-self-end">{x.action}</Link>
            </div>
          ))}
        </Card>

        <Card className="p-4">
          <div className="text-xs font-medium tracking-wide uppercase text-ink/42 mb-3">
            Weak topics · practice suggested
          </div>
          <div className="flex flex-col gap-3">
            {weak.map((w) => (
              <div key={w.label}>
                <div className="flex justify-between text-xs mb-1">
                  <span className="font-medium">{w.label}</span>
                  <span className="text-ink/55 tabular-nums">{w.pct}% correct</span>
                </div>
                <div className="h-1.5 rounded-full bg-navy/7">
                  <span
                    className={`block h-full rounded-full ${w.pct < 50 ? 'bg-navy/50' : w.pct < 70 ? 'bg-navy' : 'bg-ink/38'}`}
                    style={{ width: `${w.pct}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
          <Link to="/student/practice">
            <Button variant="outline" size="sm" className="w-full mt-4">Practice now</Button>
          </Link>
        </Card>
      </div>
    </main>
  )
}
