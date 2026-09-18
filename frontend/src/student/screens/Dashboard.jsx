import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../../shared/ui/Button.jsx'
import Card from '../../shared/ui/Card.jsx'
import { getDashboard, listOpenExams, listSittings } from '../api.js'
import { describeWindow } from '../lib/openExam.js'
import { useAuth } from '../../auth/context.js'

const PAST_DAY = new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short' })

function sittingLabel(state) {
  if (state == null) return 'Open now'
  if (state === 'IN_PROGRESS') return 'In progress'
  return 'Submitted'
}

export default function Dashboard() {
  const { user } = useAuth()
  const [data, setData] = useState(null)
  const [openExams, setOpenExams] = useState([])
  const [past, setPast] = useState([])

  useEffect(() => {
    let alive = true
    getDashboard().then((d) => alive && setData(d))
    listOpenExams().then((open) => alive && setOpenExams(open)).catch(() => {})
    listSittings().then((list) => alive && setPast(list)).catch(() => {})
    return () => { alive = false }
  }, [])

  if (!data) {
    return <main className="p-5 text-sm text-ink/42">Loading dashboard…</main>
  }

  const { weak } = data

  return (
    <main className="p-5 text-sm">
      <h1 className="m-0 mb-1 text-2xl font-semibold leading-tight tracking-[-0.02em]">
        Welcome back, {user?.fullName?.split(' ')[0] ?? 'there'}
      </h1>
      <p className="m-0 mb-4 text-ink/55">{user?.studentId}</p>

      {openExams.length > 0 ? (
        openExams.map((exam) => (
          <Card key={exam.id} tinted className="p-5 mb-4">
            <div className="flex items-center justify-between gap-4 flex-wrap">
              <div>
                <span className="text-xs font-medium tracking-wide uppercase text-navy-dark">{exam.courseCode}</span>
                <h2 className="m-0 mt-1 text-xl font-semibold tracking-[-0.015em]">{exam.title}</h2>
                <p className="m-0 mt-1 text-ink/66">{describeWindow(exam)}</p>
              </div>
              <div className="flex flex-col items-end gap-2">
                <span className="text-xs font-medium px-2.5 py-[5px] rounded-full bg-navy/16 text-navy-dark border border-navy/18">
                  {sittingLabel(exam.sessionState)}
                </span>
                {exam.sessionState === 'IN_PROGRESS' ? (
                  <Link to={`/student/exam/${exam.id}`}><Button size="sm">Resume exam</Button></Link>
                ) : exam.sessionState == null ? (
                  <Link to={`/student/briefing/${exam.id}`}><Button size="sm">Go to briefing</Button></Link>
                ) : null}
              </div>
            </div>
          </Card>
        ))
      ) : (
        <Card tinted className="p-5 mb-4">
          <h2 className="m-0 text-xl font-semibold tracking-[-0.015em]">No exam is open right now</h2>
          <p className="m-0 mt-1 text-ink/66">Exams your examiner publishes appear here.</p>
        </Card>
      )}

      <div className="grid gap-4" style={{ gridTemplateColumns: 'minmax(0,1.6fr) minmax(240px,1fr)' }}>
        <Card className="overflow-x-auto">
          <div className="grid grid-cols-[minmax(160px,2fr)_120px_100px_100px] gap-3 p-2 px-4
            border-b border-navy/14 bg-paper text-xs font-medium tracking-wide uppercase text-ink/42">
            <span>Exam</span><span>Date</span><span>Score</span><span />
          </div>
          {past.length === 0 && <div className="p-4 text-ink/42">No finished exams yet.</div>}
          {past.map((x) => (
            <div key={x.sessionId} className="grid grid-cols-[minmax(160px,2fr)_120px_100px_100px] gap-3
              items-center min-h-8 py-2.5 px-4 border-b border-navy/7 hover:bg-paper">
              <span className="font-medium">{x.title}</span>
              <span className="text-ink/66">{x.submittedAt ? PAST_DAY.format(new Date(x.submittedAt)) : '—'}</span>
              <span className="tabular-nums text-ink/66">
                {x.totalMarks == null ? '—' : Number(x.totalMarks)} / {Number(x.maxMarks)}
              </span>
              <Link to={`/student/results/${x.sessionId}`} className="font-medium justify-self-end">Review</Link>
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
