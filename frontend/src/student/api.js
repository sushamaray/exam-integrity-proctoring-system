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

export const getCodingProblem = () =>
  USE_MOCKS ? delay(260, mock.codingProblem) : http('/exams/coding')

export const getPracticeTopics = () =>
  USE_MOCKS ? delay(200, { topics: mock.practiceTopics }) : http('/practice/topics')

const TOPIC_LABELS = Object.fromEntries(mock.practiceTopics.map((t) => [t.key, t.label]))

export const getPracticeSet = (topicKey) =>
  USE_MOCKS
    ? delay(260, { questions: mock.questions.filter((q) => q.topic === TOPIC_LABELS[topicKey]) })
    : http(`/practice/${topicKey}`)
