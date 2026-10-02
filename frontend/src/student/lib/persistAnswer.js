import { saveAnswer } from '../api.js'

export const TEXT_SAVE_DEBOUNCE_MS = 800

const RETRY_DELAYS_MS = [1000, 3000]

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

/**
 * Autosave with a short backoff. A dropped connection mid-exam is the one failure a student
 * cannot do anything about, so a 5xx or a network error is retried; a 4xx is the server
 * saying no and is handed straight back.
 *
 * Shared by the objective runner and the coding editor so the two cannot drift apart on how
 * forgiving saving is.
 */
export async function saveWithRetry(sessionId, questionId, answer) {
  for (let attempt = 0; ; attempt++) {
    try {
      return await saveAnswer(sessionId, questionId, answer)
    } catch (err) {
      const retryable = !err.status || err.status >= 500
      if (!retryable || attempt >= RETRY_DELAYS_MS.length) throw err
      await wait(RETRY_DELAYS_MS[attempt])
    }
  }
}
