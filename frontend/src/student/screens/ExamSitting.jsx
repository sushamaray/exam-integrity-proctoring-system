import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Button from '../../shared/ui/Button.jsx'
import CodeEditor from '../../shared/ui/CodeEditor.jsx'
import Console from './Console.jsx'
import Pill from '../../shared/ui/Pill.jsx'
import { formatClock } from '../../shared/lib/time.js'
import {
  getSession,
  listLanguages,
  prepareConsole,
  runCode,
  sendConsoleInput,
  startConsole,
  startSession,
  stopConsole,
  submitCode,
  submitSession,
} from '../api.js'
import { TEXT_SAVE_DEBOUNCE_MS, saveWithRetry } from '../lib/persistAnswer.js'
import { useProctoring } from '../hooks/useProctoring.js'
import { SELECT } from './CodingExam.jsx'

const STATUS_LABEL = {
  OK: 'Wrong output',
  COMPILE_ERROR: 'Did not compile',
  TIMEOUT: 'Took too long',
  RUNTIME_ERROR: 'Crashed',
  OUTPUT_LIMIT: 'Printed too much',
  INTERNAL_ERROR: 'Could not be run',
}

export default function ExamSitting({ examId, selector }) {
  const navigate = useNavigate()

  const [session, setSession] = useState(null)
  const [error, setError] = useState(null)
  const [index, setIndex] = useState(0)
  const [sources, setSources] = useState({})
  const [languages, setLanguages] = useState([])
  const [language, setLanguage] = useState('C')
  const [saveState, setSaveState] = useState('idle')
  const [secondsLeft, setSecondsLeft] = useState(0)
  const [result, setResult] = useState(null)
  const [busy, setBusy] = useState(null)
  const [runError, setRunError] = useState(null)
  const [panel, setPanel] = useState('tests')
  const [finished, setFinished] = useState(null)

  const [sessionId, setSessionId] = useState(null)
  const finish = useCallback(
    () => navigate(sessionId ? `/student/results/${sessionId}` : '/student/results'),
    [navigate, sessionId],
  )

  // Bound once per sitting so the terminal is not torn down and rebuilt on every render.
  const consoleEndpoints = useMemo(() => ({
    prepare: (body) => prepareConsole(sessionId, questionIdRef.current, body),
    start: (runId) => startConsole(sessionId, runId),
    send: (runId, line) => sendConsoleInput(sessionId, runId, line),
    stop: (runId) => stopConsole(sessionId, runId),
  }), [sessionId])

  const q = session?.questions[index]
  const questionIdRef = useRef(null)
  useEffect(() => {
    questionIdRef.current = q?.id ?? null
  }, [q])
  const { recordAnswer } = useProctoring(session?.id, {
    enabled: session?.state === 'IN_PROGRESS',
    questionId: q?.id,
  })

  useEffect(() => {
    let alive = true
    listLanguages().then((list) => alive && setLanguages(list)).catch(() => {})
    return () => { alive = false }
  }, [])

  useEffect(() => {
    let alive = true

    startSession(examId)
      .then((s) => {
        if (!alive) return
        setSessionId(s.id)
        // Already submitted: say so rather than silently redirecting to the review, which reads
        // like the editor is broken when all you did was click the tab.
        if (s.state !== 'IN_PROGRESS') {
          setFinished({ sessionId: s.id, title: s.title, state: s.state })
          return
        }
        setSession(s)
        // A saved answer is the source the student left behind; otherwise start from the template.
        setSources(Object.fromEntries(
          s.questions.map((question) => [question.id, s.answers[question.id] ?? question.starterCode ?? '']),
        ))
        setSecondsLeft(s.remainingSeconds)
      })
      .catch((err) => alive && setError(err.message))
    return () => { alive = false }
  }, [examId, navigate])

  const running = session?.state === 'IN_PROGRESS'
  useEffect(() => {
    if (!running) return
    const t = setInterval(() => setSecondsLeft((s) => Math.max(0, s - 1)), 1000)
    return () => clearInterval(t)
  }, [running])

  useEffect(() => {
    if (running && secondsLeft === 0) {
      getSession(session.id).then((s) => s.state !== 'IN_PROGRESS' && finish())
    }
  }, [running, secondsLeft, session, finish])

  const savedTimer = useRef(null)
  const persist = useCallback(async (questionId, text) => {
    setSaveState('saving')
    try {
      const saved = await saveWithRetry(session.id, questionId, { textAnswer: text })
      setSecondsLeft(saved.remainingSeconds)
      setSaveState('saved')
      clearTimeout(savedTimer.current)
      savedTimer.current = setTimeout(() => setSaveState('idle'), 2000)
    } catch (err) {
      if (err.status === 409) return finish()
      setSaveState('failed')
    }
  }, [session, finish])

  const typeTimers = useRef({})
  const write = (questionId, text) => {
    setSources((all) => ({ ...all, [questionId]: text }))
    clearTimeout(typeTimers.current[questionId])
    typeTimers.current[questionId] = setTimeout(() => {
      recordAnswer(questionId)
      persist(questionId, text)
    }, TEXT_SAVE_DEBOUNCE_MS)
  }

  const execute = async (mode) => {
    setBusy(mode)
    setRunError(null)
    // Flush the pending autosave first, so what ran and what is stored cannot disagree.
    clearTimeout(typeTimers.current[q.id])
    try {
      const call = mode === 'submit' ? submitCode : runCode
      const view = await call(session.id, q.id, { language, sourceCode: sources[q.id] ?? '' })
      setResult({ questionId: q.id, view })
      if (mode === 'submit') {
        recordAnswer(q.id)
        setSaveState('saved')
      } else {
        persist(q.id, sources[q.id] ?? '')
      }
    } catch (err) {
      if (err.status === 409 && /closed|Time is up/i.test(err.message)) return finish()
      setRunError({ questionId: q.id, message: err.message })
    } finally {
      setBusy(null)
    }
  }

  const submitExam = async () => {
    try {
      await submitSession(session.id)
    } catch (err) {
      if (err.status !== 409) return setError(err.message)
    }
    finish()
  }

  const answeredCount = useMemo(
    () => Object.values(sources).filter((s) => s != null && s.trim() !== '').length,
    [sources],
  )

  if (error) {
    return <main className="p-5 text-sm text-ink/66">Could not open this exam: {error}</main>
  }
  if (finished) {
    return (
      <main className="p-5 text-sm">
        <div className="flex items-center gap-4 mb-4">{selector}</div>
        <div className="max-w-[60ch] rounded-xl border border-navy/14 bg-surface p-5">
          <h1 className="m-0 mb-1 text-xl font-semibold tracking-[-0.015em]">{finished.title}</h1>
          <p className="m-0 text-ink/66">
            You have already {finished.state === 'AUTO_SUBMITTED' ? 'run out of time on' : 'submitted'}{' '}
            this exam, so there is nothing left to sit.
          </p>
          <div className="mt-4 flex gap-2">
            <Link to={`/student/results/${finished.sessionId}`}>
              <Button size="sm">See your result</Button>
            </Link>
            <Link to="/student/coding">
              <Button variant="outline" size="sm">Open the practice compiler</Button>
            </Link>
          </div>
        </div>
      </main>
    )
  }
  if (!session) {
    return <main className="p-5 text-sm text-ink/42">Loading coding exam…</main>
  }
  // The server refuses to start a sitting on an empty pool, so this should be unreachable --
  // but crashing to a white screen mid-exam is the worst way to find out otherwise.
  if (!q) {
    return <main className="p-5 text-sm text-ink/66">This exam has no questions on it.</main>
  }

  const questions = session.questions
  // Tied to the question they came from, so moving on shows that question's own last run rather
  // than the previous one's -- and without an effect that clears state on every navigation.
  const shownResult = result?.questionId === q.id ? result.view : null
  const shownError = runError?.questionId === q.id ? runError.message : null

  const options = languages.length > 0
    ? languages
    : [{ id: 'C', label: 'C (GCC)', available: true }]

  return (
    <div className="flex flex-col h-screen">
      <div className="flex items-center justify-between gap-4 px-5 py-3 border-b border-navy/14 bg-surface">
        <span className="font-semibold tracking-[-0.01em]">{session.title}</span>
        <div className="flex items-center gap-5">
          {selector}
          <select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            aria-label="Language"
            className={SELECT}
          >
            {options.map((option) => (
              <option key={option.id} value={option.id} disabled={!option.available}>
                {option.label}{option.available ? '' : ' · soon'}
              </option>
            ))}
          </select>
          <span className="text-ink/55">
            {saveState === 'saving' && 'Saving…'}
            {saveState === 'saved' && 'Saved'}
            {saveState === 'failed' && 'Not saved: check your connection'}
            {saveState === 'idle' && `${answeredCount} / ${questions.length} attempted`}
          </span>
          <span className={`text-lg font-semibold tabular-nums ${secondsLeft < 300 ? 'text-navy-dark' : 'text-ink'}`}>
            {formatClock(secondsLeft)}
          </span>
          <Button size="sm" onClick={submitExam}>Submit exam</Button>
        </div>
      </div>

      <main className="flex-1 min-h-0 flex flex-col p-5 gap-3">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <span className="text-xs font-medium tracking-wide uppercase text-ink/42">
              Question {index + 1} of {questions.length}
              {q.topic && ` · ${q.topic}`}
            </span>
            <p className="m-0 mt-1 text-base leading-relaxed max-w-[80ch]">{q.text}</p>
          </div>
          <div className="flex gap-2 shrink-0">
            <Button
              variant="outline"
              size="sm"
              disabled={busy != null}
              onClick={() => execute('run')}
            >
              {busy === 'run' ? 'Running…' : 'Run'}
            </Button>
            <Button size="sm" disabled={busy != null} onClick={() => execute('submit')}>
              {busy === 'submit' ? 'Checking…' : 'Submit answer'}
            </Button>
          </div>
        </div>

        <div
          className="grid gap-3 flex-1 min-h-0"
          style={{ gridTemplateColumns: 'minmax(0,1.5fr) minmax(300px,1fr)' }}
        >
          <CodeEditor
            value={sources[q.id] ?? ''}
            onChange={(text) => write(q.id, text)}
            language={language}
            className="min-h-0"
          />
          <div className="flex flex-col min-h-0 gap-2">
            <div className="flex gap-1.5">
              {[['tests', 'Tests'], ['console', 'Console']].map(([id, label]) => (
                <button
                  key={id}
                  type="button"
                  onClick={() => setPanel(id)}
                  className={`h-7 px-3 rounded-full text-xs font-medium border ${
                    panel === id
                      ? 'bg-navy/16 border-navy/18 text-navy-dark'
                      : 'border-navy/18 text-ink/55 hover:bg-navy/7'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
            <div className="flex-1 min-h-0">
              {panel === 'tests' ? (
                <ResultPanel question={q} result={shownResult} error={shownError} busy={busy != null} />
              ) : (
                <Console
                  key={q.id}
                  endpoints={consoleEndpoints}
                  language={language}
                  source={sources[q.id] ?? ''}
                />
              )}
            </div>
          </div>
        </div>

        <div className="flex justify-between">
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
    </div>
  )
}

function ResultPanel({ question, result, error, busy }) {
  // Keyed by name, not index: a Run reports the visible cases in order, but a Submit reports
  // every case including hidden ones, so positions no longer line up.
  const sampleByName = new Map((question.tests ?? []).map((test) => [test.name, test]))

  return (
    <div className="border border-navy/14 rounded-xl bg-surface p-4 overflow-y-auto min-h-0">
      {error && (
        <div className="mb-3 rounded-[6px] border border-navy/18 bg-navy/7 p-2.5 text-navy-dark">
          {error}
        </div>
      )}

      {!result && !error && (
        <>
          <div className="text-xs font-medium tracking-wide uppercase text-ink/42 mb-3">Sample cases</div>
          {question.tests.length === 0 && <p className="m-0 text-ink/42">No sample cases for this question.</p>}
          <div className="flex flex-col gap-3">
            {question.tests.map((test) => (
              <div key={test.name}>
                <div className="font-medium mb-1">{test.name}</div>
                <Stream label="Input" text={test.input} />
                <Stream label="Expected" text={test.expected} />
              </div>
            ))}
          </div>
          <p className="m-0 mt-4 text-ink/42">
            {busy ? 'Compiling…' : 'Run checks the sample cases. Submit answer also runs the hidden ones and is what gets marked.'}
          </p>
        </>
      )}

      {result && (
        <>
          <div className="flex items-center justify-between gap-2 mb-3">
            <span className="text-xs font-medium tracking-wide uppercase text-ink/42">
              {result.isFinal ? 'Submitted answer' : 'Sample run'}
            </span>
            <Pill tone={result.passed === result.total && result.total > 0 ? 'escalate' : 'clear'}>
              {result.status === 'OK'
                ? `${result.passed} / ${result.total} passed`
                : STATUS_LABEL[result.status] ?? result.status}
            </Pill>
          </div>

          {result.compileOutput && (
            <div className="mb-3">
              <div className="text-xs font-medium text-ink/55 mb-1">Compiler output</div>
              <pre className="m-0 font-mono text-xs whitespace-pre-wrap break-words rounded-[6px]
                border border-navy/14 bg-paper p-2.5 text-ink/66">{result.compileOutput}</pre>
            </div>
          )}

          <div className="flex flex-col gap-3">
            {result.tests.map((test) => (
              <div key={test.name} className="border-b border-navy/7 pb-3 last:border-b-0 last:pb-0">
                <div className="flex items-center justify-between gap-2">
                  <span className="font-medium">
                    {test.name}
                    {test.hidden && <span className="ml-2 text-xs font-normal text-ink/42">hidden</span>}
                  </span>
                  <span className={test.passed ? 'font-medium text-navy-dark' : 'text-ink/55'}>
                    {test.passed ? 'Passed' : STATUS_LABEL[test.status] ?? 'Failed'}
                  </span>
                </div>
                {!test.hidden && (
                  <>
                    <Stream label="Input" text={sampleByName.get(test.name)?.input ?? ''} />
                    <Stream label="Expected" text={sampleByName.get(test.name)?.expected ?? ''} />
                    <Stream label="Your output" text={test.stdout ?? ''} />
                    {test.stderr && <Stream label="Errors" text={test.stderr} />}
                  </>
                )}
              </div>
            ))}
          </div>

          {result.isFinal && (
            <p className="m-0 mt-3 text-ink/42">
              Marked out of the cases above. Submitting again replaces this.
            </p>
          )}
        </>
      )}
    </div>
  )
}

function Stream({ label, text }) {
  return (
    <div className="mt-1">
      <div className="text-xs text-ink/42">{label}</div>
      <pre className="m-0 font-mono text-xs whitespace-pre-wrap break-words rounded-[6px]
        border border-navy/10 bg-paper px-2 py-1.5">{text === '' ? '(empty)' : text}</pre>
    </div>
  )
}
