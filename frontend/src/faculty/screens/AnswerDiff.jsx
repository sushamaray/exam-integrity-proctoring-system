import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getPair } from '../api.js'
import { hhmm } from '../lib/adapters.js'
import { METHOD_LABEL, segments } from '../lib/collusion.js'

function Segments({ segments }) {
  return (
    <p className="m-0 text-base leading-[1.7]">
      {segments.map((seg, i) =>
        seg.hot ? (
          <span key={i} className="bg-navy/12 border-b-2 border-navy/45">{seg.text}</span>
        ) : (
          <span key={i}>{seg.text}</span>
        )
      )}
    </p>
  )
}

export default function AnswerDiff() {
  const { pairId } = useParams()
  const [d, setD] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let alive = true
    getPair(pairId)
      .then((v) => alive && setD(v))
      .catch((err) => alive && setError(err.message))
    return () => { alive = false }
  }, [pairId])

  if (!d) {
    return <main className="p-5 text-sm text-ink/42">{error ?? 'Loading answer diff…'}</main>
  }

  const isText = d.method === 'TFIDF_COSINE'
  const sides = [['a', d.a], ['b', d.b]]

  return (
    <main className="p-5 text-sm">
      <Link to={`/faculty/collusion/${d.examId}`} className="text-xs font-medium">← Collusion graph</Link>

      <div className="my-2.5 mb-4">
        <h1 className="m-0 mb-1 text-2xl font-semibold leading-tight tracking-[-0.02em]">
          {d.a.student} ↔ {d.b.student}
        </h1>
        <div className="text-ink/55">
          {METHOD_LABEL[d.method]} · score {d.score.toFixed(isText ? 2 : 3)} · {d.detail}
          {isText && ' · matching spans underlined'}
        </div>
      </div>

      {isText ? (
        <>
          <p className="m-0 mb-3 text-ink/66 max-w-[88ch]">{d.questionText}</p>
          <div className="grid gap-3 mb-3" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))' }}>
            {sides.map(([key, side]) => (
              <div key={key} className="border border-navy/14 rounded-xl bg-surface p-4">
                <div className="flex items-center justify-between gap-3 mb-3">
                  <span className="font-medium">{side.student}</span>
                  <span className="text-xs text-ink/42">
                    Submitted {hhmm(side.submittedAt)} · {side.text ? side.text.split(/\s+/).length : 0} words
                  </span>
                </div>
                <Segments segments={segments(side.text, d.spans, key)} />
              </div>
            ))}
          </div>
          <div className="border border-navy/18 rounded-xl bg-navy/5 p-3 px-4">
            <span className="font-medium text-navy">How to read this: </span>
            <span className="text-ink/70">
              Underlined runs are five or more words the two answers share. Phrases every student used
              carry no weight in the score; rare shared phrasing is what raises it. A high score is a
              reason to look, not a finding.
            </span>
          </div>
        </>
      ) : (
        <>
          <div className="border border-navy/14 rounded-xl bg-surface overflow-x-auto mb-3">
            <div className="grid grid-cols-[minmax(240px,2fr)_minmax(160px,1fr)_minmax(160px,1fr)] min-w-[640px]
              gap-3 p-2 px-4 border-b border-navy/14 bg-paper text-xs font-medium tracking-wide uppercase text-ink/42">
              <span>Question</span><span>Both chose</span><span>Correct answer</span>
            </div>
            {d.sharedMistakes.map((m) => (
              <div key={m.questionId}
                className="grid grid-cols-[minmax(240px,2fr)_minmax(160px,1fr)_minmax(160px,1fr)] min-w-[640px]
                  gap-3 items-center min-h-8 py-2.5 px-4 border-b border-navy/7">
                <span>{m.question}</span>
                <span className="text-navy-dark font-medium">{m.option.toUpperCase()} · {m.optionText}</span>
                <span className="text-ink/66">{m.correctOption?.toUpperCase()} · {m.correctText}</span>
              </div>
            ))}
          </div>
          <div className="border border-navy/18 rounded-xl bg-navy/5 p-3 px-4">
            <span className="font-medium text-navy">Why matching mistakes: </span>
            <span className="text-ink/70">
              There is one right answer, so strong students agree on it; there are three wrong ones.
              The score is how unlikely this much agreement on wrong options is by chance, given how
              often the whole cohort picked each distractor.
            </span>
          </div>
        </>
      )}
    </main>
  )
}
