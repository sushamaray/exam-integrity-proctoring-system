import { USE_MOCKS, delay, http } from '../shared/lib/api.js'
import * as mock from './mock/data.js'

export const getExams = () =>
  USE_MOCKS ? delay(250, { rows: mock.examRows }) : http('/faculty/exams')

export const getMonitor = () =>
  USE_MOCKS
    ? delay(250, { header: mock.examHeader, filters: mock.monitorFilters, cohort: mock.cohortNote, tiles: mock.tiles })
    : http('/faculty/monitor')

export const getSession = (id) =>
  USE_MOCKS ? delay(280, mock.sessionDetail) : http(`/faculty/sessions/${id}`)

export const getReviewQueue = () =>
  USE_MOCKS ? delay(250, { stats: mock.queueStats, rows: mock.queueRows }) : http('/faculty/review-queue')

export const getCollusionGraph = () =>
  USE_MOCKS ? delay(300, mock.collusionGraph) : http('/faculty/collusion')

export const getAnswerDiff = (a, b) =>
  USE_MOCKS ? delay(250, mock.answerDiff) : http(`/faculty/diff/${a}/${b}`)

export const getItemAnalysis = () =>
  USE_MOCKS ? delay(280, { items: mock.items }) : http('/faculty/item-analysis')

export const setFlagDecision = (sessionId, decision) =>
  USE_MOCKS
    ? delay(200, { ok: true })
    : http(`/faculty/sessions/${sessionId}/decision`, {
        method: 'POST',
        body: JSON.stringify({ decision }),
      })
