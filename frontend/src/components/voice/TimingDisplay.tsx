import React, { useEffect, useState } from 'react'
import { Box, Card, CardContent, Typography, Chip } from '@mui/material'
import api from '../../utils/api'

interface TimingDisplayProps {
  logId: number
  hour: number
}

interface TimingData {
  hour: number
  total_duration: number
  hour_duration: number
  remaining: number
  remaining_formatted: string
  over_under: number
  status?: string
  severity?: string
  message?: string
  percentage?: number
}

const TimingDisplay: React.FC<TimingDisplayProps> = ({ logId, hour }) => {
  const [timingData, setTimingData] = useState<TimingData | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchTiming = async () => {
      try {
        setLoading(true)
        // Calculate timing from log data
        const logResponse = await api.get(`/logs/${logId}`)
        const log = logResponse.data
        
        if (log && log.json_data) {
          const hourlyLogs = log.json_data.hourly_logs || {}
          const hourStr = `${hour.toString().padStart(2, '0')}:00`
          const hourData = hourlyLogs[hourStr] || {}
          const elements = hourData.elements || []
          
          const totalDuration = elements.reduce((sum: number, elem: any) => sum + (elem.duration || 0), 0)
          const hourDuration = 3600
          const remaining = hourDuration - totalDuration
          
          const timing: TimingData = {
            hour,
            total_duration: totalDuration,
            hour_duration: hourDuration,
            remaining,
            remaining_formatted: `${Math.floor(remaining / 60)}:${Math.floor(remaining % 60).toString().padStart(2, '0')}`,
            over_under: remaining,
          }
          
          // Determine status
          if (remaining < -60) {
            timing.status = 'over'
            timing.severity = 'error'
            timing.message = `Hour is ${Math.abs(remaining).toFixed(0)} seconds over`
          } else if (remaining < 0) {
            timing.status = 'over'
            timing.severity = 'warning'
            timing.message = `Hour is ${Math.abs(remaining).toFixed(0)} seconds over`
          } else if (remaining < 60) {
            timing.status = 'tight'
            timing.severity = 'warning'
            timing.message = `Only ${remaining.toFixed(0)} seconds remaining`
          } else {
            timing.status = 'ok'
            timing.severity = 'info'
            timing.message = `${remaining.toFixed(0)} seconds remaining`
          }
          
          timing.percentage = (totalDuration / hourDuration) * 100
          setTimingData(timing)
        }
      } catch (err) {
        console.error('Timing fetch error:', err)
      } finally {
        setLoading(false)
      }
    }

    if (logId && hour !== undefined) {
      fetchTiming()
    }
  }, [logId, hour])

  if (loading || !timingData) {
    return null
  }

  const isOver = timingData.over_under < 0
  const isTight = timingData.remaining < 60 && timingData.remaining >= 0
  const severity = timingData.severity || (isOver ? 'error' : isTight ? 'warning' : 'info')

  return (
    <Card>
      <CardContent>
        <Typography variant="subtitle1" gutterBottom>
          Hour {hour}:00 Timing
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
          <Typography variant="h6">
            {timingData.remaining_formatted}
          </Typography>
          <Chip
            label={timingData.message || (isOver ? 'Over' : 'OK')}
            color={severity as any}
            size="small"
          />
        </Box>
        <Typography variant="body2" color="text.secondary">
          Total: {Math.floor(timingData.total_duration / 60)}:{(timingData.total_duration % 60).toFixed(0).padStart(2, '0')} / 60:00
        </Typography>
        {timingData.percentage && (
          <Typography variant="caption" color="text.secondary">
            {timingData.percentage.toFixed(1)}% of hour used
          </Typography>
        )}
      </CardContent>
    </Card>
  )
}

export default TimingDisplay

