import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Button from '../../shared/ui/Button.jsx'
import { formatClock } from '../../shared/lib/time.js'
import { getExam, saveAnswer, submitExam } from '../api.js'
import { useProctoring } from '../hooks/useProctoring.js'

const EXAM_ID = 'cse4103-midterm'
const DURATION_S = 60 * 60

export default function ObjectiveExam() {
  const [questions, setQuestions] = useState(null)
  const [index, setIndex] = useState(0)
  const [answers, setAnswers] = useState({})
  const [flagged, setFlagged] = useState(() => new Set())
  const [saveState, setSaveState] = useState('idle') // idle | saving | saved
  const [secondsLeft, setSecondsLeft] = useState(DURATION_S)
  const navigate = useNavigate()

  useProctoring(EXAM_ID)

  useEffect(() => {
    let alive = true
    getExam(EXAM_ID).then((d) => alive && setQuestions(d.questions))
    return () => { alive = false }
  }, [])

  useEffect(() => {
    const t = setInterval(() => setSecondsLeft((s) => Math.max(0, s - 1)), 1000)
    return () => clearInterval(t)
  }, [])

  const savedTimer = useRef(null)
  const pick = async (qId, optionId) => {
    setAnswers((a) => ({ ...a, [qId]: optionId }))
    setSaveState('saving')
    await saveAnswer(EXAM_ID, qId, optionId)
    setSaveState('saved')
    clearTimeout(savedTimer.current)
    savedTimer.current = setTimeout(() => setSaveState('idle'), 2000)
  }

  const toggleFlag = (qId) => {
    setFlagged((prev) => {
      const next = new Set(prev)
      next.has(qId) ? next.delete(qId) : next.add(qId)
      return next
    })
  }

  const submit = async () => {
    await submitExam(EXAM_ID)
    navigate('/student/results')
  }

  const q = questions?.[index]
  const answeredCount = useMemo(() => Object.keys(answers).length, [answers])

  if (!questions) {
    return <main className="p-5 text-sm text-ink/42">Loading exam…</main>
  }

  return (
    <div className="flex flex-col h-screen">
      <div className="flex items-center justify-between gap-4 px-5 py-3 border-b border-navy/14 bg-surface">
        <span className="font-semibold tracking-[-0.01em]">CSE 4103 · Advanced OOP Midterm</span>
        <div className="flex items-center gap-5">
          <span className="text-ink/55">
            {saveState === 'saving' && 'Saving…'}
            {saveState === 'saved' && 'Saved'}
            {saveState === 'idle' && `${answeredCount} / ${questions.length} answered`}
          </span>
          <span className={`text-lg font-semibold tabular-nums ${secondsLeft < 300 ? 'text-navy-dark' : 'text-ink'}`}>
            {formatClock(secondsLeft)}
          </span>
          <Button size="sm" onClick={submit}>Submit exam</Button>
        </div>
      </div>

      <div className="flex flex-1 min-h-0">
        <main className="flex-1 overflow-y-auto p-6 max-w-[720px]">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-medium tracking-wide uppercase text-ink/42">
              Question {index + 1} of {questions.length} · {q.bloom} · {q.topic}
            </span>
            <button
              type="button"
              onClick={() => toggleFlag(q.id)}
              className={`text-xs font-medium px-2.5 py-[5px] rounded-full border ${
                flagged.has(q.id) ? 'bg-navy/16 border-navy/18 text-navy-dark' : 'border-navy/18 text-ink/55'
              }`}
            >
              {flagged.has(q.id) ? 'Marked for review' : 'Mark for review'}
            </button>
          </div>

          <p className="text-base leading-relaxed mb-5">{q.text}</p>

          <div className="flex flex-col gap-2.5">
            {q.options.map((opt) => {
              const selected = answers[q.id] === opt.id
              return (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() => pick(q.id, opt.id)}
                  className={`flex items-center gap-3 w-full min-h-12 px-3.5 rounded-[6px] text-left
                    border ${selected ? 'border-navy bg-navy/7' : 'border-navy/14 bg-surface hover:bg-navy/7'}`}
                >
                  <span className={`w-6 h-6 rounded-full flex items-center justify-center text-[13px]
                    font-medium shrink-0 border ${selected ? 'bg-navy border-navy text-white' : 'border-navy/18 text-ink/55'}`}>
                    {opt.id.toUpperCase()}
                  </span>
                  <span>{opt.text}</span>
                </button>
              )
            })}
          </div>

          <div className="flex justify-between mt-6">
            <Button variant="outline" size="sm" disabled={index === 0} onClick={() => setIndex((i) => i - 1)}>
              Previous
            </Button>
            <Button
              size="sm"
              disabled={index === questions.length - 1}
              onClick={() => setIndex((i) => i + 1)}
            >
              Next
            </Button>
          </div>
        </main>

        <aside className="w-[220px] shrink-0 border-l border-navy/14 bg-surface p-4 overflow-y-auto">
          <div className="text-xs font-medium tracking-wide uppercase text-ink/42 mb-3">Palette</div>
          <div className="grid grid-cols-5 gap-1.5">
            {questions.map((qq, i) => {
              const isCurrent = i === index
              const isAnswered = answers[qq.id] != null
              const isFlagged = flagged.has(qq.id)
              return (
                <button
                  key={qq.id}
                  type="button"
                  onClick={() => setIndex(i)}
                  className={`h-7 rounded-[3px] text-xs tabular-nums border ${
                    isCurrent
                      ? 'bg-navy border-navy text-white'
                      : isFlagged
                        ? 'bg-navy/10 border-navy/28 text-navy-dark'
                        : isAnswered
                          ? 'bg-navy/7 border-navy/18 text-navy-dark'
                          : 'bg-surface border-navy/14 text-ink/42'
                  }`}
                >
                  {i + 1}
                </button>
              )
            })}
          </div>
          <div className="mt-4 flex flex-col gap-1.5 text-xs text-ink/55">
            <span className="flex items-center gap-2"><span className="w-2.5 h-2.5 rounded-sm bg-navy" />Current</span>
            <span className="flex items-center gap-2"><span className="w-2.5 h-2.5 rounded-sm bg-navy/7 border border-navy/18" />Answered</span>
            <span className="flex items-center gap-2"><span className="w-2.5 h-2.5 rounded-sm bg-navy/10 border border-navy/28" />Marked</span>
            <span className="flex items-center gap-2"><span className="w-2.5 h-2.5 rounded-sm bg-surface border border-navy/14" />Unanswered</span>
          </div>
        </aside>
      </div>
    </div>
  )
}
