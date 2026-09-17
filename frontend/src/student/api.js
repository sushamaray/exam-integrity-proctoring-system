import { USE_MOCKS, delay, http } from '../shared/lib/api.js'
import * as mock from './mock/data.js'

export const login = (email, password) =>
  USE_MOCKS
    ? delay(320, { ok: true, student: mock.student })
    : http('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })

export const getDashboard = () =>
  USE_MOCKS
    ? delay(250, { student: mock.student, upcoming: mock.upcomingExam, past: mock.pastExams, weak: mock.weakTopics })
    : http('/student/dashboard')

export const getBriefing = () =>
  USE_MOCKS
    ? delay(220, {
        exam: mock.upcomingExam,
        readiness: mock.proctoringReadiness,
        recorded: mock.recordedEvents,
        notRecorded: mock.notRecordedEvents,
      })
    : http('/student/briefing')

export const getExam = (id) =>
  USE_MOCKS ? delay(300, { questions: mock.questions }) : http(`/exams/${id}`)

export const saveAnswer = (examId, qId, optionId) =>
  USE_MOCKS
    ? delay(180, { ok: true })
    : http(`/exams/${examId}/answers`, {
        method: 'POST',
        body: JSON.stringify({ questionId: qId, optionId }),
      })

export const submitExam = (examId) =>
  USE_MOCKS ? delay(400, { ok: true }) : http(`/exams/${examId}/submit`, { method: 'POST' })

export const getCodingProblem = () =>
  USE_MOCKS ? delay(260, mock.codingProblem) : http('/exams/coding')

export const getPracticeTopics = () =>
  USE_MOCKS ? delay(200, { topics: mock.practiceTopics }) : http('/practice/topics')

const TOPIC_LABELS = Object.fromEntries(mock.practiceTopics.map((t) => [t.key, t.label]))

export const getPracticeSet = (topicKey) =>
  USE_MOCKS
    ? delay(260, { questions: mock.questions.filter((q) => q.topic === TOPIC_LABELS[topicKey]) })
    : http(`/practice/${topicKey}`)

export const getPostReview = (examId) =>
  USE_MOCKS ? delay(300, { questions: mock.reviewQuestions, exam: mock.upcomingExam }) : http(`/exams/${examId}/review`)
