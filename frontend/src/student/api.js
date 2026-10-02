import { USE_MOCKS, delay, http } from '../shared/lib/api.js'
import * as mock from './mock/data.js'

export const getDashboard = () =>
  USE_MOCKS
    ? delay(250, { weak: mock.weakTopics })
    : http('/student/dashboard')

export const getBriefing = () =>
  USE_MOCKS
    ? delay(220, {
        readiness: mock.proctoringReadiness,
        recorded: mock.recordedEvents,
        notRecorded: mock.notRecordedEvents,
      })
    : http('/student/briefing')

export const listOpenExams = () => http('/student/exams')

export const startSession = (examId) => http(`/exams/${examId}/sessions`, { method: 'POST' })

export const getSession = (sessionId) => http(`/sessions/${sessionId}`)

export const saveAnswer = (sessionId, questionId, answer) =>
  http(`/sessions/${sessionId}/answers/${questionId}`, {
    method: 'PUT',
    body: JSON.stringify(answer),
  })

export const submitSession = (sessionId) => http(`/sessions/${sessionId}/submit`, { method: 'POST' })

export const listSittings = () => http('/student/sittings')

export const getReview = (sessionId) => http(`/sessions/${sessionId}/review`)

export const listLanguages = () => http('/code/languages')

export const runCode = (sessionId, questionId, body) =>
  http(`/sessions/${sessionId}/code/${questionId}/run`, {
    method: 'POST',
    body: JSON.stringify(body),
  })

export const submitCode = (sessionId, questionId, body) =>
  http(`/sessions/${sessionId}/code/${questionId}/submit`, {
    method: 'POST',
    body: JSON.stringify(body),
  })

export const prepareConsole = (sessionId, questionId, body) =>
  http(`/sessions/${sessionId}/code/${questionId}/console`, {
    method: 'POST',
    body: JSON.stringify(body),
  })

export const startConsole = (sessionId, runId) =>
  http(`/sessions/${sessionId}/console/${runId}/start`, { method: 'POST' })

export const sendConsoleInput = (sessionId, runId, line) =>
  http(`/sessions/${sessionId}/console/${runId}/stdin`, {
    method: 'POST',
    body: JSON.stringify({ line }),
  })

export const stopConsole = (sessionId, runId) =>
  http(`/sessions/${sessionId}/console/${runId}`, { method: 'DELETE' })

// The same four calls with no exam behind them: the free compiler.
export const preparePracticeConsole = (body) =>
  http('/code/console', { method: 'POST', body: JSON.stringify(body) })

export const startPracticeConsole = (runId) =>
  http(`/code/console/${runId}/start`, { method: 'POST' })

export const sendPracticeConsoleInput = (runId, line) =>
  http(`/code/console/${runId}/stdin`, { method: 'POST', body: JSON.stringify({ line }) })

export const stopPracticeConsole = (runId) =>
  http(`/code/console/${runId}`, { method: 'DELETE' })

export const getPracticeTopics = () =>
  USE_MOCKS ? delay(200, { topics: mock.practiceTopics }) : http('/practice/topics')

const TOPIC_LABELS = Object.fromEntries(mock.practiceTopics.map((t) => [t.key, t.label]))

export const getPracticeSet = (topicKey) =>
  USE_MOCKS
    ? delay(260, { questions: mock.questions.filter((q) => q.topic === TOPIC_LABELS[topicKey]) })
    : http(`/practice/${topicKey}`)
