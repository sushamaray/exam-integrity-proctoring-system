import { useCallback, useEffect, useRef } from 'react'
import { http } from '../../shared/lib/api.js'

const FLUSH_MS = 3000
const HEARTBEAT_MS = 5000

const MAX_BATCH = 200

const MAX_BACKLOG = 2000

export function useProctoring(sessionId, { enabled = true, questionId = null } = {}) {
  const buffer = useRef([])
  const currentQuestion = useRef(questionId)
  const stopped = useRef(false)

  useEffect(() => {
    currentQuestion.current = questionId
  }, [questionId])

  const emit = useCallback((type, payload = {}, forQuestion = currentQuestion.current) => {
    if (stopped.current) return
    buffer.current.push({
      clientEventId: crypto.randomUUID(),
      type,
      occurredAt: new Date().toISOString(),
      questionId: forQuestion,
      payload,
    })
    if (buffer.current.length > MAX_BACKLOG) buffer.current.splice(0, buffer.current.length - MAX_BACKLOG)
  }, [])

  useEffect(() => {
    if (!enabled || sessionId == null) return
    stopped.current = false

    const path = `/sessions/${sessionId}/events`

    const flush = async ({ keepalive = false } = {}) => {
      if (buffer.current.length === 0) return
      const batch = buffer.current.splice(0, MAX_BATCH)
      try {
        await http(path, { method: 'POST', body: JSON.stringify(batch), keepalive })
      } catch (err) {
        if (err.status === 409 || err.status === 404) {
          stopped.current = true
          buffer.current = []
          return
        }

        buffer.current.unshift(...batch)
      }
    }

    const onVisibility = () => emit(document.hidden ? 'TAB_HIDDEN' : 'TAB_VISIBLE')
    const onBlur = () => emit('WINDOW_BLUR')
    const onFocus = () => emit('WINDOW_FOCUS')
    const onPaste = (e) =>
      emit('PASTE', { charCount: (e.clipboardData?.getData('text') || '').length })
    const onCopy = () => emit('COPY')
    const onFs = () => emit(document.fullscreenElement ? 'FS_ENTER' : 'FS_EXIT')

    const onPageHide = () => flush({ keepalive: true })

    document.addEventListener('visibilitychange', onVisibility)
    window.addEventListener('blur', onBlur)
    window.addEventListener('focus', onFocus)
    document.addEventListener('paste', onPaste)
    document.addEventListener('copy', onCopy)
    document.addEventListener('fullscreenchange', onFs)
    window.addEventListener('pagehide', onPageHide)

    emit('HEARTBEAT')
    const heartbeat = setInterval(() => emit('HEARTBEAT'), HEARTBEAT_MS)
    const flusher = setInterval(flush, FLUSH_MS)

    return () => {
      document.removeEventListener('visibilitychange', onVisibility)
      window.removeEventListener('blur', onBlur)
      window.removeEventListener('focus', onFocus)
      document.removeEventListener('paste', onPaste)
      document.removeEventListener('copy', onCopy)
      document.removeEventListener('fullscreenchange', onFs)
      window.removeEventListener('pagehide', onPageHide)
      clearInterval(heartbeat)
      clearInterval(flusher)
      flush()
    }
  }, [sessionId, enabled, emit])

  const recordAnswer = useCallback((forQuestion) => emit('ANSWER', {}, forQuestion), [emit])

  return { recordAnswer }
}
