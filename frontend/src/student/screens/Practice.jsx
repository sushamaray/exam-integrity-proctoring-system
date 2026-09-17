import { useEffect, useState } from 'react'
import Button from '../../shared/ui/Button.jsx'
import Card from '../../shared/ui/Card.jsx'
import { getPracticeSet, getPracticeTopics } from '../api.js'

export default function Practice() {
  const [topics, setTopics] = useState(null)
  const [topic, setTopic] = useState(null)
  const [prevTopic, setPrevTopic] = useState(null)
  const [questions, setQuestions] = useState(null)
  const [index, setIndex] = useState(0)
  const [picked, setPicked] = useState(null)

  // Adjusting state during render when the topic changes, rather than
  // resetting it with a synchronous setState inside an effect.
  if (topic !== prevTopic) {
    setPrevTopic(topic)
    setQuestions(null)
    setIndex(0)
    setPicked(null)
  }

  useEffect(() => {
    let alive = true
    getPracticeTopics().then((d) => {
      if (!alive) return
      setTopics(d.topics)
      setTopic(d.topics[0]?.key)
    })
    return () => { alive = false }
  }, [])

  useEffect(() => {
    if (!topic) return
    let alive = true
    getPracticeSet(topic).then((d) => {
      if (!alive) return
      setQuestions(d.questions)
    })
    return () => { alive = false }
  }, [topic])

  if (!topics) {
    return <main className="p-5 text-sm text-ink/42">Loading practice…</main>
  }

  const q = questions?.[index]

  return (
    <main className="p-5 text-sm max-w-[720px]">
      <h1 className="m-0 mb-1 text-2xl font-semibold leading-tight tracking-[-0.02em]">Practice</h1>
      <p className="m-0 mb-4 text-ink/55">Untimed. Explanations show immediately after you answer.</p>

      <div className="flex gap-2 flex-wrap mb-5">
        {topics.map((t) => (
          <button
            key={t.key}
            type="button"
            onClick={() => setTopic(t.key)}
            className={`text-sm px-3.5 py-2 rounded-full border cursor-pointer ${
              topic === t.key ? 'border-navy bg-navy/7 text-navy-dark' : 'border-navy/14 bg-surface text-ink/66'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {!questions && <div className="text-ink/42">Loading questions…</div>}

      {q && (
        <Card className="p-5">
          <div className="text-xs font-medium tracking-wide uppercase text-ink/42 mb-3">
            {index + 1} of {questions.length} · {q.bloom}
          </div>
          <p className="text-base leading-relaxed mb-5">{q.text}</p>

          <div className="flex flex-col gap-2.5">
            {q.options.map((opt) => {
              const isPicked = picked === opt.id
              const isCorrect = opt.id === q.correct
              const revealed = picked != null
              const tone = !revealed
                ? 'border-navy/14 bg-surface hover:bg-navy/7'
                : isCorrect
                  ? 'border-navy bg-navy/7'
                  : isPicked
                    ? 'border-navy/40 bg-navy/5'
                    : 'border-navy/14 bg-surface opacity-60'
              return (
                <button
                  key={opt.id}
                  type="button"
                  disabled={revealed}
                  onClick={() => setPicked(opt.id)}
                  className={`flex items-center gap-3 w-full min-h-12 px-3.5 rounded-[6px] text-left border ${tone}`}
                >
                  <span className={`w-6 h-6 rounded-full flex items-center justify-center text-[13px]
                    font-medium shrink-0 border ${revealed && isCorrect ? 'bg-navy border-navy text-white' : 'border-navy/18 text-ink/55'}`}>
                    {opt.id.toUpperCase()}
                  </span>
                  <span>{opt.text}</span>
                </button>
              )
            })}
          </div>

          {picked != null && (
            <div className="mt-4 p-3.5 rounded-[6px] bg-navy/7 border border-navy/18 text-navy-dark">
              {q.explanation}
            </div>
          )}

          <div className="flex justify-end mt-5">
            <Button
              size="sm"
              disabled={index === questions.length - 1}
              onClick={() => { setIndex((i) => i + 1); setPicked(null) }}
            >
              Next question
            </Button>
          </div>
        </Card>
      )}
    </main>
  )
}
