import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { Box } from '@mui/material'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import ErrorBoundary from './components/ErrorBoundary'
import Login from './pages/auth/Login'
import Setup from './pages/Setup'
import Dashboard from './pages/Dashboard'
import LibraryList from './pages/library/LibraryList'
import SpotsLibrary from './pages/library/SpotsLibrary'
import ClockBuilder from './pages/clocks/ClockBuilder'
import TrafficManager from './pages/traffic/TrafficManager'
import Advertisers from './pages/traffic/Advertisers'
import Agencies from './pages/traffic/Agencies'
import Orders from './pages/traffic/Orders'
import SalesReps from './pages/traffic/SalesReps'
import LogGenerator from './pages/logs/LogGenerator'
import LogEditor from './pages/logs/LogEditor'
import SpotScheduler from './components/scheduling/SpotScheduler'
import Dayparts from './pages/traffic/Dayparts'
import DaypartCategories from './pages/traffic/DaypartCategories'
import RotationRules from './pages/traffic/RotationRules'
import TrafficLogs from './pages/traffic/TrafficLogs'
import CopyLibrary from './pages/traffic/CopyLibrary'
import VoiceRecorder from './pages/voice/VoiceRecorder'
import ReportsHub from './pages/reports/ReportsHub'
import Invoices from './pages/billing/Invoices'
import Payments from './pages/billing/Payments'
import Makegoods from './pages/billing/Makegoods'
import AuditLogs from './pages/admin/AuditLogs'
import Webhooks from './pages/admin/Webhooks'
import Notifications from './pages/admin/Notifications'
import Settings from './pages/admin/Settings'
import Backups from './pages/admin/Backups'
import Users from './pages/admin/Users'
import InventoryDashboard from './pages/analytics/InventoryDashboard'
import RevenueDashboard from './pages/analytics/RevenueDashboard'
import SalesGoals from './pages/analytics/SalesGoals'
import HelpCenter from './components/Help/HelpCenter'
import { HelpPreferencesProvider } from './contexts/HelpPreferencesContext'

function App() {
  return (
    <ErrorBoundary>
      <HelpPreferencesProvider>
        <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
          <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/setup" element={<Setup />} />
        <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="library" element={<LibraryList />} />
          <Route path="library/spots" element={<Navigate to="/library" replace />} />
          <Route path="clocks" element={<ClockBuilder />} />
          <Route path="traffic" element={<TrafficManager />} />
          <Route path="traffic/advertisers" element={<Advertisers />} />
          <Route path="traffic/agencies" element={<Agencies />} />
          <Route path="traffic/orders" element={<Orders />} />
          <Route path="traffic/sales-reps" element={<SalesReps />} />
          <Route path="traffic/spot-scheduler" element={<SpotScheduler />} />
          <Route path="traffic/dayparts" element={<Dayparts />} />
          <Route path="traffic/daypart-categories" element={<DaypartCategories />} />
          <Route path="traffic/rotation-rules" element={<RotationRules />} />
          <Route path="traffic/traffic-logs" element={<TrafficLogs />} />
          <Route path="traffic/copy" element={<CopyLibrary />} />
          <Route path="logs" element={<LogGenerator />} />
          <Route path="logs/:logId/edit" element={<LogEditor />} />
          <Route path="voice" element={<VoiceRecorder />} />
          <Route path="reports" element={<ReportsHub />} />
          <Route path="billing/invoices" element={<Invoices />} />
          <Route path="billing/payments" element={<Payments />} />
          <Route path="billing/makegoods" element={<Makegoods />} />
          <Route path="admin/users" element={<Users />} />
          <Route path="admin/settings" element={<Settings />} />
          <Route path="admin/backups" element={<Backups />} />
          <Route path="admin/audit-logs" element={<AuditLogs />} />
          <Route path="admin/webhooks" element={<Webhooks />} />
          <Route path="admin/notifications" element={<Notifications />} />
          <Route path="analytics/inventory" element={<InventoryDashboard />} />
          <Route path="analytics/revenue" element={<RevenueDashboard />} />
          <Route path="analytics/sales-goals" element={<SalesGoals />} />
          <Route path="help" element={<HelpCenter />} />
        </Route>
        <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        </Box>
      </HelpPreferencesProvider>
    </ErrorBoundary>
  )
}

export default App
