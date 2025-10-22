// Shared types for LibreLog

export interface User {
  id: number
  username: string
  role: 'admin' | 'producer' | 'dj' | 'sales'
  created_at: string
  last_login?: string
}

export interface Track {
  id: number
  title: string
  artist?: string
  album?: string
  type: 'MUS' | 'ADV' | 'PSA' | 'LIN' | 'INT' | 'PRO' | 'BED'
  genre?: string
  duration?: number
  filepath: string
  libretime_id?: string
  last_played?: string
  created_at: string
  updated_at: string
}

export interface Campaign {
  id: number
  advertiser: string
  start_date: string
  end_date: string
  priority: number
  file_url?: string
  active: boolean
  created_at: string
  updated_at: string
}

export interface ClockTemplate {
  id: number
  name: string
  description?: string
  json_layout: ClockLayout
  created_at: string
  updated_at: string
}

export interface ClockLayout {
  hour: string
  elements: ClockElement[]
}

export interface ClockElement {
  type: 'MUS' | 'ADV' | 'PSA' | 'LIN' | 'INT' | 'PRO' | 'BED'
  title?: string
  count?: number
  fallback?: string
  duration?: number
}

export interface DailyLog {
  id: number
  date: string
  generated_by: number
  json_data: LogData
  published: boolean
  created_at: string
  updated_at: string
}

export interface LogData {
  date: string
  hours: HourLog[]
}

export interface HourLog {
  hour: string
  tracks: LogTrack[]
}

export interface LogTrack {
  time: string
  title: string
  artist?: string
  type: string
  duration: number
  track_id?: number
}

export interface VoiceTrack {
  id: number
  show_name?: string
  file_url: string
  scheduled_time?: string
  uploaded_by: number
  created_at: string
}

export interface PlaybackHistory {
  id: number
  track_id: number
  log_id: number
  played_at: string
  duration_played?: number
}

export interface ReconciliationReport {
  date: string
  scheduled: LogTrack[]
  played: PlaybackHistory[]
  discrepancies: Discrepancy[]
}

export interface Discrepancy {
  type: 'missed' | 'wrong_track' | 'timing' | 'duration'
  scheduled: LogTrack
  played?: PlaybackHistory
  description: string
}

// API Response types
export interface ApiResponse<T> {
  data: T
  message?: string
  status: 'success' | 'error'
}

export interface PaginatedResponse<T> {
  items: T[]
  total: number
  page: number
  per_page: number
  pages: number
}

// Form types
export interface LoginForm {
  username: string
  password: string
}

export interface TrackForm {
  title: string
  artist?: string
  album?: string
  type: Track['type']
  genre?: string
  duration?: number
  file: File
}

export interface CampaignForm {
  advertiser: string
  start_date: string
  end_date: string
  priority: number
  file?: File
}

export interface ClockTemplateForm {
  name: string
  description?: string
  json_layout: ClockLayout
}
