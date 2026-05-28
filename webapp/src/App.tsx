import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import { ProfileProvider } from './contexts/ProfileContext'
import { EntriesProvider } from './contexts/EntriesContext'
import LoginPage from './pages/LoginPage'
import HomePage from './pages/HomePage'
import HistoryPage from './pages/HistoryPage'
import EntryDetailPage from './pages/EntryDetailPage'
import SettingsPage from './pages/SettingsPage'

function ProtectedLayout() {
  const { user, loading } = useAuth()
  if (loading) return <div className="h-[100dvh] bg-bg" />
  if (!user) return <Navigate to="/login" replace />
  return (
    <ProfileProvider>
      <EntriesProvider>
        <Outlet />
      </EntriesProvider>
    </ProfileProvider>
  )
}

function AppRoutes() {
  const { user, loading } = useAuth()
  if (loading) return <div className="h-[100dvh] bg-bg" />

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route element={<ProtectedLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/history" element={<HistoryPage />} />
        <Route path="/entry/:kind/:id" element={<EntryDetailPage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  )
}
