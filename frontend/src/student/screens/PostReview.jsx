import { useEffect, useState } from 'react'
import Card from '../../shared/ui/Card.jsx'
import { getPostReview } from '../api.js'

export default function PostReview() {
  const [data, setData] = useState(null)

  useEffect(() => {
    let alive = true
    getPostReview('cse4103-midterm').then((d) => alive && setData(d))
    return () => { alive = false }
  }, [])

  if (!data) {
    return <main className="p-5 text-sm text-ink/42">Loading review…</main>
  }

  const { questions, exam } = data
  const correctCount = questions.filter((q) => q.yourAnswer === q.correct).length

  return (
    <main className="p-5 text-sm max-w-[760px]">
      <h1 className="m-0 mb-1 text-2xl font-semibold leading-tight tracking-[-0.02em]">Post-exam review</h1>
      <p className="m-0 mb-4 text-ink/55">{exam.title}</p>

      <Card tinted className="p-4 mb-5 flex items-center gap-4">
        <span className="text-3xl font-semibold tabular-nums text-navy-dark">
          {correctCount} / {questions.length}
        </span>
        <span className="text-ink/66">
          Correct answers. Explanations below show why each option is right or wrong.
        </span>
      </Card>

      <div className="flex flex-col gap-3">
        {questions.map((q, i) => {
          const isCorrect = q.yourAnswer === q.correct
          return (
            <Card key={q.id} className="p-4">
              <div className="flex items-center justify-between gap-3 mb-2">
                <span className="text-xs font-medium tracking-wide uppercase text-ink/42">
                  Question {i + 1} · {q.topic}
                </span>
                <span className={`text-xs font-medium px-2.5 py-[5px] rounded-full border ${
                  isCorrect ? 'border-navy/18 bg-navy/7 text-navy-dark' : 'border-navy/18 bg-navy/16 text-navy-dark'
                }`}>
                  {isCorrect ? 'Correct' : 'Incorrect'}
                </span>
              </div>
              <p className="m-0 mb-3 leading-relaxed">{q.text}</p>
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
              <p className="m-0 text-ink/55">{q.explanation}</p>
            </Card>
          )
        })}
      </div>
    </main>
  )
}
