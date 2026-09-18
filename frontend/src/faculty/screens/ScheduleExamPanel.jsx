import { useEffect, useState } from 'react'
import Button from '../../shared/ui/Button.jsx'
import { createExam, listCourses } from '../api.js'

const INPUT = 'w-full h-9 px-2.5 rounded-[6px] border border-navy/14 bg-paper focus:outline-none focus:border-navy'

function localNow() {
  const d = new Date()
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset())
  return d.toISOString().slice(0, 16)
}

export default function ScheduleExamPanel({ onCreated, onClose }) {
  const [courses, setCourses] = useState([])
  const [courseId, setCourseId] = useState('')
  const [title, setTitle] = useState('')
  const [startsAt, setStartsAt] = useState(localNow)
  const [duration, setDuration] = useState(60)
  const [hours, setHours] = useState(24)
  const [file, setFile] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [fileErrors, setFileErrors] = useState([])
  const [dragging, setDragging] = useState(false)

  const pickFile = (picked) => {
    if (!picked) return
    if (!picked.name.toLowerCase().endsWith('.csv')) {
      setError('That is not a .csv file.')
      return
    }
    setFile(picked)
    setError(null)
    setFileErrors([])
  }

  useEffect(() => {
    let alive = true
    listCourses()
      .then((list) => {
        if (!alive) return
        setCourses(list)
        if (list.length > 0) setCourseId(String(list[0].id))
      })
      .catch((err) => alive && setError(err.message))
    return () => { alive = false }
  }, [])

  const submit = async (e) => {
    e.preventDefault()
    if (!file) {
      setError('Choose a CSV file of questions.')
      return
    }
    const form = new FormData()
    form.append('courseId', courseId)
    form.append('title', title)
    form.append('startsAt', new Date(startsAt).toISOString())
    form.append('durationMinutes', duration)
    form.append('availableHours', hours)
    form.append('file', file)

    setBusy(true)
    setError(null)
    setFileErrors([])
    try {
      onCreated(await createExam(form))
    } catch (err) {
      setError(err.message)
      setFileErrors(err.fieldErrors?.file ? err.fieldErrors.file.split('\n') : [])
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="border border-navy/14 rounded-xl bg-surface p-4 mb-4">
      <div className="flex items-center justify-between gap-3 mb-3">
        <span className="text-xs font-medium tracking-wide uppercase text-ink/42">Schedule exam from CSV</span>
        <a href="/sample-questions.csv" download className="text-xs font-medium">Download sample CSV</a>
      </div>

      <div className="grid gap-3 mb-3" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))' }}>
        <label className="flex flex-col gap-1">
          <span className="text-ink/55">Course</span>
          <select value={courseId} onChange={(e) => setCourseId(e.target.value)} className={INPUT} required>
            {courses.map((c) => <option key={c.id} value={c.id}>{c.code} · {c.title}</option>)}
          </select>
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-ink/55">Title</span>
          <input value={title} onChange={(e) => setTitle(e.target.value)} className={INPUT}
            required maxLength={200} placeholder="Quiz 3 · Design patterns" />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-ink/55">Opens at</span>
          <input type="datetime-local" value={startsAt} onChange={(e) => setStartsAt(e.target.value)}
            className={INPUT} required />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-ink/55">Duration per student (minutes)</span>
          <input type="number" min={1} max={300} value={duration}
            onChange={(e) => setDuration(e.target.value)} className={INPUT} required />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-ink/55">Open for (hours)</span>
          <input type="number" min={1} max={168} value={hours}
            onChange={(e) => setHours(e.target.value)} className={INPUT} required />
        </label>
      </div>

      <label
        onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault()
          setDragging(false)
          pickFile(e.dataTransfer.files?.[0])
        }}
        className={`flex flex-col items-center justify-center gap-1 mb-3 p-6 rounded-xl border-2 border-dashed
          cursor-pointer text-center transition-colors ${
            dragging ? 'border-navy bg-navy/7' : 'border-navy/18 bg-paper hover:bg-navy/5'
          }`}
      >
        <input type="file" accept=".csv,text/csv" className="sr-only"
          onChange={(e) => pickFile(e.target.files?.[0])} />
        {file ? (
          <>
            <span className="font-medium text-navy-dark">{file.name}</span>
            <span className="text-xs text-ink/42">{(file.size / 1024).toFixed(1)} KB · click or drop to replace</span>
          </>
        ) : (
          <>
            <span className="font-medium">Drop the questions CSV here</span>
            <span className="text-xs text-ink/42">or click to choose a file</span>
          </>
        )}
      </label>

      <p className="m-0 mb-3 text-xs text-ink/42">
        Header: topic,question,option_a,option_b,option_c,option_d,correct,marks. The correct column is
        a, b, c or d; marks may be left blank for 1. Leave the options and correct blank for a written
        question.
      </p>

      {error && (
        <div className="mb-3 text-navy-dark">
          <p className="m-0">{error}</p>
          {fileErrors.length > 0 && (
            <ul className="m-0 mt-1 pl-5 text-ink/66">
              {fileErrors.map((line) => <li key={line}>{line}</li>)}
            </ul>
          )}
        </div>
      )}

      <div className="flex justify-end gap-2">
        <Button variant="outline" size="sm" onClick={onClose}>Cancel</Button>
        <Button size="sm" type="submit" disabled={busy || courses.length === 0}>
          {busy ? 'Creating…' : 'Create exam'}
        </Button>
      </div>
    </form>
  )
}
