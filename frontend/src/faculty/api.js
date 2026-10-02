import { http } from '../shared/lib/api.js'
import * as adapt from './lib/adapters.js'

export const getExams = async () => ({ rows: (await http('/faculty/exams')).map(adapt.examRow) })

export const listCourses = () => http('/faculty/courses')

export const createExam = async (formData) =>
  adapt.examRow(await http('/faculty/exams', { method: 'POST', body: formData }))

export const listExams = () => http('/faculty/exams')

export const getMonitor = async (examId) => {
  const view = await http(`/exams/${examId}/monitor`)
  return { exam: view, tiles: view.sessions.map(adapt.monitorTile) }
}

export const getSession = async (id) => adapt.sessionDetail(await http(`/sessions/${id}/risk`))

export const getReviewQueue = async () => adapt.reviewQueue(await http('/faculty/review-queue'))

export const setFlagDecision = (sessionId, decision, note) =>
  http(`/faculty/sessions/${sessionId}/decision`, {
    method: 'POST',
    body: JSON.stringify({ decision, note: note || null }),
  })

export const getCollusionGraph = (examId) => http(`/exams/${examId}/similarity`)

export const runSimilarity = (examId) => http(`/exams/${examId}/similarity/run`, { method: 'POST' })

export const getPair = (pairId) => http(`/similarity/pairs/${pairId}`)

export const getItemAnalysis = (examId) => http(`/exams/${examId}/item-analysis`)

export const runItemAnalysis = (examId) =>
  http(`/exams/${examId}/item-analysis/run`, { method: 'POST' })
