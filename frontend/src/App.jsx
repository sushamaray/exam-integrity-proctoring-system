import { Navigate, Route, Routes } from 'react-router-dom'

import ProtectedRoute from './auth/ProtectedRoute'
import FacultyDashboard from './pages/FacultyDashboard'
import InvigilatorDashboard from './pages/InvigilatorDashboard'
import LoginPage from './pages/LoginPage'
import StudentDashboard from './pages/StudentDashboard'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route
        path="/student"
        element={
          <ProtectedRoute roles={['STUDENT']}>
            <StudentDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/faculty"
        element={
          <ProtectedRoute roles={['FACULTY']}>
            <FacultyDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/invigilator"
        element={
          <ProtectedRoute roles={['INVIGILATOR']}>
            <InvigilatorDashboard />
          </ProtectedRoute>
        }
      />

      {/* Anything else, including "/", is decided by whether there is a
          session: ProtectedRoute sends you to your own dashboard or to login. */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
