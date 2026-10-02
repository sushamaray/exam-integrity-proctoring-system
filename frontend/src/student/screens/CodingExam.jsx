import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import ExamSitting from './ExamSitting.jsx'
import PracticeLab from './PracticeLab.jsx'
import { listOpenExams } from '../api.js'

export const SELECT = `h-8 px-2 rounded-[6px] border border-navy/14 bg-surface text-ink/66 font-medium
  max-w-[260px] focus-visible:outline-none focus:border-navy`

/**
 * Picks what you are coding against: nothing in particular, or one of your open coding exams.
 *
 * The compiler is the same either way. Practice is the default landing because it is the one that
 * is always available -- an exam may be finished, or not open yet.
 */
export default function CodingExam() {
  const { examId } = useParams()
  const navigate = useNavigate()
  const [exams, setExams] = useState([])

  useEffect(() => {
    let alive = true
    listOpenExams()
      .then((open) => alive && setExams(open.filter((exam) => exam.coding)))
      .catch(() => {})
    return () => { alive = false }
  }, [])

  const selector = (
    <select
      value={examId ?? ''}
      onChange={(e) => navigate(e.target.value ? `/student/coding/${e.target.value}` : '/student/coding')}
      aria-label="What to code against"
      className={SELECT}
    >
      <option value="">Practice · no exam</option>
      {exams.map((exam) => (
        <option key={exam.id} value={exam.id}>{exam.title}</option>
      ))}
    </select>
  )

  return examId
    ? <ExamSitting key={examId} examId={examId} selector={selector} />
    : <PracticeLab selector={selector} />
}
