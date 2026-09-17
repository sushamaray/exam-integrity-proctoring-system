import { Routes, Route, Navigate } from 'react-router-dom'

import Login          from './student/screens/Login.jsx'
import StudentLayout  from './student/StudentLayout.jsx'
import Dashboard      from './student/screens/Dashboard.jsx'
import Briefing       from './student/screens/Briefing.jsx'
import ObjectiveExam  from './student/screens/ObjectiveExam.jsx'
import CodingExam     from './student/screens/CodingExam.jsx'
import Practice       from './student/screens/Practice.jsx'
import PostReview     from './student/screens/PostReview.jsx'

import FacultyLayout   from './faculty/FacultyLayout.jsx'
import ExamList        from './faculty/screens/ExamList.jsx'
import LiveMonitor      from './faculty/screens/LiveMonitor.jsx'
import SessionDetail    from './faculty/screens/SessionDetail.jsx'
import ReviewQueue      from './faculty/screens/ReviewQueue.jsx'
import CollusionGraph   from './faculty/screens/CollusionGraph.jsx'
import AnswerDiff        from './faculty/screens/AnswerDiff.jsx'
import ItemAnalysis      from './faculty/screens/ItemAnalysis.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<Login />} />

      <Route path="/student" element={<StudentLayout />}>
        <Route index            element={<Dashboard />} />
        <Route path="briefing"  element={<Briefing />} />
        <Route path="exam"      element={<ObjectiveExam />} />
        <Route path="coding"    element={<CodingExam />} />
        <Route path="practice"  element={<Practice />} />
        <Route path="results"   element={<PostReview />} />
      </Route>

      <Route path="/faculty" element={<FacultyLayout />}>
        <Route index               element={<ExamList />} />
        <Route path="monitor"      element={<LiveMonitor />} />
        <Route path="session/:id"  element={<SessionDetail />} />
        <Route path="review"       element={<ReviewQueue />} />
        <Route path="collusion"    element={<CollusionGraph />} />
        <Route path="diff/:a/:b"   element={<AnswerDiff />} />
        <Route path="analysis"     element={<ItemAnalysis />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
