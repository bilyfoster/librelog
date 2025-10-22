import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { Box } from '@mui/material'
import Layout from './components/Layout'
import Login from './pages/auth/Login'
import Setup from './pages/Setup'
import Dashboard from './pages/Dashboard'
import LibraryList from './pages/library/LibraryList'
import ClockBuilder from './pages/clocks/ClockBuilder'
import TrafficManager from './pages/traffic/TrafficManager'
import LogGenerator from './pages/logs/LogGenerator'
import VoiceRecorder from './pages/voice/VoiceRecorder'
import ReportsHub from './pages/reports/ReportsHub'

function App() {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/setup" element={<Setup />} />
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="library" element={<LibraryList />} />
          <Route path="clocks" element={<ClockBuilder />} />
          <Route path="traffic" element={<TrafficManager />} />
          <Route path="logs" element={<LogGenerator />} />
          <Route path="voice" element={<VoiceRecorder />} />
          <Route path="reports" element={<ReportsHub />} />
        </Route>
      </Routes>
    </Box>
  )
}

export default App
