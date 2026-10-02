import { Suspense, lazy } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'

import { AuthProvider } from './auth/AuthContext.jsx'
import RequireAuth    from './auth/RequireAuth.jsx'
import Login          from './auth/Login.jsx'
import Register       from './auth/Register.jsx'

import StudentLayout  from './student/StudentLayout.jsx'
import Dashboard      from './student/screens/Dashboard.jsx'
import Briefing       from './student/screens/Briefing.jsx'
import ObjectiveExam  from './student/screens/ObjectiveExam.jsx'
import Practice       from './student/screens/Practice.jsx'
import PostReview     from './student/screens/PostReview.jsx'

// Split out: CodeMirror roughly doubles the bundle, and only a coding sitting needs it. Everyone
// else -- including the login page -- should not pay for an editor they will never open.
const CodingExam = lazy(() => import('./student/screens/CodingExam.jsx'))
// Faculty get the same free compiler, minus the exam selector -- they do not sit exams.
const PracticeLab = lazy(() => import('./student/screens/PracticeLab.jsx'))

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
    <AuthProvider>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        <Route
          path="/student"
          element={
            <RequireAuth roles={['STUDENT']}>
              <StudentLayout />
            </RequireAuth>
          }
        >
          <Route index            element={<Dashboard />} />
          <Route path="briefing/:examId?" element={<Briefing />} />
          <Route path="exam/:examId?"     element={<ObjectiveExam />} />
          <Route
            path="coding/:examId?"
            element={
              <Suspense fallback={<main className="p-5 text-sm text-ink/42">Loading editor…</main>}>
                <CodingExam />
              </Suspense>
            }
          />
          <Route path="practice"  element={<Practice />} />
          <Route path="results/:sessionId?" element={<PostReview />} />
        </Route>

        <Route
          path="/faculty"
          element={
            <RequireAuth roles={['FACULTY', 'INVIGILATOR']}>
              <FacultyLayout />
            </RequireAuth>
          }
        >
          <Route index               element={<ExamList />} />
          <Route path="monitor/:examId?"   element={<LiveMonitor />} />
          <Route path="session/:id"  element={<SessionDetail />} />
          <Route path="review"       element={<ReviewQueue />} />
          <Route path="collusion/:examId?" element={<CollusionGraph />} />
          <Route path="diff/:pairId"      element={<AnswerDiff />} />
          <Route path="analysis/:examId?" element={<ItemAnalysis />} />
          <Route
            path="lab"
            element={
              <Suspense fallback={<main className="p-5 text-sm text-ink/42">Loading editor…</main>}>
                <PracticeLab />
              </Suspense>
            }
          />
        </Route>

        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </AuthProvider>
  )
}
