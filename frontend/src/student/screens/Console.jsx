import { useCallback, useEffect, useRef, useState } from 'react'
import Button from '../../shared/ui/Button.jsx'
import { useConsoleSocket } from '../hooks/useConsoleSocket.js'

const SEGMENT_TONE = {
  out: 'text-ink',
  err: 'text-navy-dark',
  in: 'text-navy font-medium',
  sys: 'text-ink/42 italic',
}

/** Appends a chunk, merging into the previous segment when it is the same kind. */
function append(segments, kind, text) {
  if (!text) return segments
  const last = segments[segments.length - 1]
  if (last && last.kind === kind) {
    return [...segments.slice(0, -1), { ...last, text: last.text + text }]
  }
  return [...segments, { kind, text, key: `${segments.length}-${kind}` }]
}

/**
 * A terminal: run the program, watch it print, type back at it.
 *
 * The four calls come in as `endpoints` so the same terminal serves an exam sitting and the free
 * scratchpad, which differ only in which URLs they talk to.
 *
 * Starting is two calls because the broker has no replay -- prepare and subscribe first, then
 * launch, so nothing printed at startup is missed. The hook reports readiness only once the
 * broker has acknowledged the subscription.
 */
export default function Console({ endpoints, language, source }) {
  const [runId, setRunId] = useState(null)
  const [segments, setSegments] = useState([])
  const [state, setState] = useState('idle')
  const [line, setLine] = useState('')

  const scrollback = useRef(null)
  const inputBox = useRef(null)
  const launched = useRef(null)

  const onEvent = useCallback((event) => {
    switch (event.type) {
      case 'STARTED':
        setState('running')
        break
      case 'STDOUT':
        setSegments((all) => append(all, 'out', event.text))
        break
      case 'STDERR':
        setSegments((all) => append(all, 'err', event.text))
        break
      case 'EXITED':
        setSegments((all) => append(all, 'sys', `\n[finished with exit code ${event.exitCode}]\n`))
        setState('ended')
        break
      case 'ERROR':
        setSegments((all) => append(all, 'sys', `\n[${event.text}]\n`))
        setState('ended')
        break
      default:
        break
    }
  }, [])

  const ready = useConsoleSocket(runId, onEvent)

  // Launch only once the subscription is live, and only once per run.
  useEffect(() => {
    if (!ready || !runId || launched.current === runId) return
    launched.current = runId
    endpoints.start(runId).catch((err) => {
      setSegments((all) => append(all, 'sys', `\n[${err.message}]\n`))
      setState('ended')
    })
  }, [ready, runId, endpoints])

  useEffect(() => {
    if (scrollback.current) {
      scrollback.current.scrollTop = scrollback.current.scrollHeight
    }
  }, [segments])

  useEffect(() => {
    if (state === 'running' && inputBox.current) {
      inputBox.current.focus()
    }
  }, [state])

  // A program must not outlive the screen that was driving it.
  const live = useRef({ endpoints, runId })
  useEffect(() => {
    live.current = { endpoints, runId }
  }, [endpoints, runId])
  useEffect(() => () => {
    const { endpoints: e, runId: r } = live.current
    if (r) e.stop(r).catch(() => {})
  }, [])

  const run = async () => {
    const previous = runId
    if (previous) endpoints.stop(previous).catch(() => {})

    setRunId(null)
    setSegments([])
    setState('compiling')
    try {
      const prepared = await endpoints.prepare({ language, sourceCode: source })
      if (!prepared.runId) {
        setSegments([{ kind: 'err', text: prepared.compileOutput, key: 'compile' }])
        setState('ended')
        return
      }
      setRunId(prepared.runId)
    } catch (err) {
      setSegments([{ kind: 'sys', text: `[${err.message}]`, key: 'error' }])
      setState('ended')
    }
  }

  const stop = async () => {
    if (!runId) return
    try {
      await endpoints.stop(runId)
    } catch {
      // Already gone; the ERROR or EXITED event has said so.
    }
  }

  const send = async (event) => {
    event.preventDefault()
    if (state !== 'running') return
    const typed = line
    setLine('')
    setSegments((all) => append(all, 'in', `${typed}\n`))
    try {
      await endpoints.send(runId, typed)
    } catch (err) {
      setSegments((all) => append(all, 'sys', `\n[${err.message}]\n`))
      setState('ended')
    }
  }

  const busy = state === 'compiling'

  return (
    <div className="flex flex-col h-full min-h-0 gap-2">
      <div className="flex items-center justify-between gap-2">
        <span className="text-xs font-medium tracking-wide uppercase text-ink/42">
          {state === 'idle' && 'Console'}
          {state === 'compiling' && 'Compiling…'}
          {state === 'running' && 'Running'}
          {state === 'ended' && 'Finished'}
        </span>
        <div className="flex gap-2">
          {state === 'running' ? (
            <Button variant="outline" size="sm" onClick={stop}>Stop</Button>
          ) : (
            <Button size="sm" onClick={run} disabled={busy}>
              {state === 'idle' ? 'Run program' : 'Run again'}
            </Button>
          )}
        </div>
      </div>

      <div
        ref={scrollback}
        className="flex-1 min-h-0 overflow-y-auto rounded-xl border border-navy/14 bg-surface p-3"
      >
        {segments.length === 0 ? (
          <p className="m-0 text-ink/42">
            Run your program here and type input at it, the way you would in a terminal.
          </p>
        ) : (
          <pre className="m-0 font-mono text-xs leading-relaxed whitespace-pre-wrap break-words">
            {segments.map((segment) => (
              <span key={segment.key} className={SEGMENT_TONE[segment.kind]}>{segment.text}</span>
            ))}
          </pre>
        )}
      </div>

      <form onSubmit={send} className="flex gap-2">
        <input
          ref={inputBox}
          value={line}
          onChange={(e) => setLine(e.target.value)}
          disabled={state !== 'running'}
          placeholder={state === 'running' ? 'Type input, then press Enter' : 'Run the program to type at it'}
          aria-label="Input for the running program"
          spellCheck={false}
          autoComplete="off"
          className="flex-1 min-w-0 h-9 px-2.5 rounded-[6px] border border-navy/14 bg-surface
            font-mono text-xs focus:outline-none focus:border-navy disabled:bg-paper
            disabled:text-ink/38"
        />
        <Button size="sm" type="submit" disabled={state !== 'running'}>Send</Button>
      </form>
    </div>
  )
}
