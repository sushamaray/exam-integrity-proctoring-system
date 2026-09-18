import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../../shared/ui/Button.jsx'
import { getExams } from '../api.js'
import { STATE_PILL } from '../lib/tone.js'
import ScheduleExamPanel from './ScheduleExamPanel.jsx'

const PILL_BASE = 'inline-flex items-center rounded-full px-2.5 py-[5px] text-xs font-medium whitespace-nowrap'

export default function ExamList() {
  const [rows, setRows] = useState(null)
  const [error, setError] = useState(null)
  const [scheduling, setScheduling] = useState(false)

  useEffect(() => {
    let alive = true
    getExams()
      .then((d) => alive && setRows(d.rows))
      .catch((err) => alive && setError(err.message))
    return () => { alive = false }
  }, [])

  return (
    <main className="p-5 text-sm">
      <div className="flex items-center justify-between gap-4 flex-wrap mb-4">
        <h1 className="m-0 text-2xl font-semibold leading-tight tracking-[-0.02em]">Exams</h1>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => setScheduling(true)}>Import questions (CSV)</Button>
          <Button variant="primary" size="sm" onClick={() => setScheduling(true)}>Schedule exam</Button>
        </div>
      </div>

      {scheduling && (
        <ScheduleExamPanel
          onClose={() => setScheduling(false)}
          onCreated={(row) => {
            setRows((prev) => [row, ...(prev ?? [])])
            setScheduling(false)
          }}
        />
      )}

      <div className="border border-navy/14 rounded-xl bg-surface overflow-x-auto">
        <div className="grid grid-cols-[minmax(180px,2fr)_104px_116px_120px_140px_88px] min-w-[860px]
          gap-3 p-2 px-3 border-b border-navy/14 bg-paper text-xs font-medium tracking-wide
          uppercase text-ink/42">
          <span>Exam</span><span>State</span><span>Pool / variants</span>
          <span>Students</span><span>Flags</span><span />
        </div>

        {!rows && (
          <div className="p-6 text-ink/42">{error ?? 'Loading exams…'}</div>
        )}
        {rows?.length === 0 && (
          <div className="p-6 text-ink/42">No exams in the courses you staff.</div>
        )}

        {rows?.map((x) => (
          <div
            key={x.id}
            className="grid grid-cols-[minmax(180px,2fr)_104px_116px_120px_140px_88px] min-w-[860px]
              gap-3 items-center min-h-8 py-2.5 px-3 border-b border-navy/7 hover:bg-paper"
          >
            <span>
              <span className="block font-medium">{x.name}</span>
              <span className="block text-xs text-ink/42 whitespace-nowrap">{x.when}</span>
            </span>
            <span className={`justify-self-start ${PILL_BASE} ${STATE_PILL[x.state]}`}>{x.state}</span>
            <span className="text-ink/66 tabular-nums">{x.pool}</span>
            <span className="text-ink/66 tabular-nums">{x.students}</span>
            <span className="text-ink/66">{x.flags}</span>
            {x.action
              ? <Link to={x.to} className="font-medium justify-self-end">{x.action}</Link>
              : <span />}
          </div>
        ))}
      </div>
    </main>
  )
}
