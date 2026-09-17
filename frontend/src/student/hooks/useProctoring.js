import { useEffect, useRef } from 'react'

export function useProctoring(sessionId, { enabled = true } = {}) {
  const queue = useRef([])

  useEffect(() => {
    if (!enabled) return

    const emit = (type, payload = {}) => {
      const event = { sessionId, type, occurredAt: new Date().toISOString(), payload }
      queue.current.push(event)
      console.log('[proctoring]', event) // becomes a POST later
    }

    const onVisibility = () => emit(document.hidden ? 'TAB_HIDDEN' : 'TAB_VISIBLE')
    const onBlur = () => emit('WINDOW_BLUR')
    const onFocus = () => emit('WINDOW_FOCUS')
    const onPaste = (e) =>
      emit('PASTE', { charCount: (e.clipboardData?.getData('text') || '').length })
    const onCopy = () => emit('COPY')
    const onFs = () => emit(document.fullscreenElement ? 'FS_ENTER' : 'FS_EXIT')

    document.addEventListener('visibilitychange', onVisibility)
    window.addEventListener('blur', onBlur)
    window.addEventListener('focus', onFocus)
    document.addEventListener('paste', onPaste)
    document.addEventListener('copy', onCopy)
    document.addEventListener('fullscreenchange', onFs)

    const heartbeat = setInterval(() => emit('HEARTBEAT'), 5000)

    return () => {
      document.removeEventListener('visibilitychange', onVisibility)
      window.removeEventListener('blur', onBlur)
      window.removeEventListener('focus', onFocus)
      document.removeEventListener('paste', onPaste)
      document.removeEventListener('copy', onCopy)
      document.removeEventListener('fullscreenchange', onFs)
      clearInterval(heartbeat)
    }
  }, [sessionId, enabled])
}
