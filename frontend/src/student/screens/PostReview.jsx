import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Card from '../../shared/ui/Card.jsx'
import { getReview, listSittings } from '../api.js'

const DAY = new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })

const marks = (value) => (value == null ? '—' : Number(value).toString())

export default function PostReview() {
  const { sessionId } = useParams()
  const navigate = useNavigate()
  const [sittings, setSittings] = useState(null)
  const [review, setReview] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let alive = true
    listSittings()
      .then((list) => {
        if (!alive) return
        setSittings(list)
        if (!sessionId && list.length > 0) navigate(`/student/results/${list[0].sessionId}`, { replace: true })
      })
      .catch((err) => alive && setError(err.message))
    return () => { alive = false }
  }, [sessionId, navigate])

  useEffect(() => {
    if (!sessionId) return
    let alive = true
    getReview(sessionId)
      .then((d) => alive && setReview(d))
      .catch((err) => alive && setError(err.message))
    return () => { alive = false }
  }, [sessionId])

  if (error) {
    return <main className="p-5 text-sm text-ink/66">Could not load the review: {error}</main>
  }
  if (sittings && sittings.length === 0) {
    return (
      <main className="p-5 text-sm">
        <h1 className="m-0 mb-1 text-2xl font-semibold leading-tight tracking-[-0.02em]">Post-exam review</h1>
        <p className="m-0 text-ink/55">You have not finished any exams yet.</p>
      </main>
    )
  }
  if (!sittings || !review || String(review.sessionId) !== String(sessionId)) {
    return <main className="p-5 text-sm text-ink/42">Loading review…</main>
  }

  const pending = review.questions.some((q) => q.marksAwarded == null)

  return (
    <main className="p-5 text-sm max-w-[760px]">
      <div className="flex items-end justify-between gap-4 flex-wrap mb-4">
        <div>
          <h1 className="m-0 mb-1 text-2xl font-semibold leading-tight tracking-[-0.02em]">Post-exam review</h1>
          <p className="m-0 text-ink/55">{review.title}</p>
        </div>
        <label className="flex flex-col gap-1 text-ink/55">
          Exam
          <select
            value={sessionId}
            onChange={(e) => navigate(`/student/results/${e.target.value}`)}
            className="h-9 px-2.5 rounded-[6px] border border-navy/14 bg-surface text-ink focus:outline-none focus:border-navy"
          >
            {sittings.map((s) => (
              <option key={s.sessionId} value={s.sessionId}>
                {s.title} · {s.submittedAt ? DAY.format(new Date(s.submittedAt)) : ''}
              </option>
            ))}
          </select>
        </label>
      </div>

      <Card tinted className="p-4 mb-5 flex items-center gap-4">
        <span className="text-3xl font-semibold tabular-nums text-navy-dark">
          {marks(review.totalMarks)} / {marks(review.maxMarks)}
        </span>
        <span className="text-ink/66">
          {pending
            ? 'Marks so far. Written answers are still waiting to be graded by a person.'
            : 'Marks awarded. Explanations below show why each option is right or wrong.'}
        </span>
      </Card>

      <div className="flex flex-col gap-3">
        {review.questions.map((q, i) => {
          const isChoice = q.options.length > 0
          const answered = q.yourAnswer != null && q.yourAnswer !== ''
          const isCorrect = isChoice && q.yourAnswer === q.correct
          const status = !answered ? 'Not answered' : !isChoice ? 'Awaiting grading' : isCorrect ? 'Correct' : 'Incorrect'
          return (
            <Card key={q.id} className="p-4">
              <div className="flex items-center justify-between gap-3 mb-2">
                <span className="text-xs font-medium tracking-wide uppercase text-ink/42">
                  Question {i + 1}{q.topic && ` · ${q.topic}`} · {marks(q.marksAwarded)} / {marks(q.maxMarks)}
                </span>
                <span className={`text-xs font-medium px-2.5 py-[5px] rounded-full border ${
                  isCorrect ? 'border-navy/18 bg-navy/7 text-navy-dark' : 'border-navy/18 bg-navy/16 text-navy-dark'
                }`}>
                  {status}
                </span>
              </div>
              <p className="m-0 mb-3 leading-relaxed">{q.text}</p>

              {isChoice ? (
                <div className="flex flex-col gap-1.5 mb-3">
                  {q.options.map((opt) => {
                    const isYours = opt.id === q.yourAnswer
                    const isRight = opt.id === q.correct
                    return (
                      <div
                        key={opt.id}
                        className={`flex items-center gap-3 min-h-9 px-3 rounded-[6px] border text-sm ${
                          isRight
                            ? 'border-navy bg-navy/7'
                            : isYours
                              ? 'border-navy/40 bg-navy/5'
                              : 'border-navy/14'
                        }`}
                      >
                        <span className="w-5 h-5 rounded-full border border-navy/18 flex items-center
                          justify-center text-[11px] font-medium shrink-0">
                          {opt.id.toUpperCase()}
                        </span>
                        <span>{opt.text}</span>
                        {isYours && !isRight && <span className="ml-auto text-xs text-navy-dark">Your answer</span>}
                        {isRight && <span className="ml-auto text-xs text-navy-dark">Correct answer</span>}
                      </div>
                    )
                  })}
                </div>
              ) : (
                <p className="m-0 mb-3 p-3 rounded-[6px] border border-navy/14 bg-paper whitespace-pre-wrap">
                  {answered ? q.yourAnswer : 'No answer written.'}
                </p>
              )}

              {q.explanation && <p className="m-0 text-ink/55">{q.explanation}</p>}
            </Card>
          )
        })}
      </div>
    </main>
  )
}
