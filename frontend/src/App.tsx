import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { RoleRoute } from './auth/RoleRoute'
import { AppLayout } from './pages/layout/AppLayout'
import { LoginPage } from './pages/LoginPage'
import { LeadsPage } from './pages/LeadsPage'
import { StudentsPage } from './pages/StudentsPage'
import { StatisticsPage } from './pages/StatisticsPage'
import { AdminPage } from './pages/AdminPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/leads" element={<LeadsPage />} />
        <Route path="/students" element={<StudentsPage />} />
        <Route path="/statistics" element={<StatisticsPage />} />
        <Route
          path="/admin"
          element={
            <RoleRoute role="ADMIN">
              <AdminPage />
            </RoleRoute>
          }
        />
        <Route path="/" element={<Navigate to="/leads" replace />} />
      </Route>
      <Route path="*" element={<Navigate to="/leads" replace />} />
    </Routes>
  )
}
