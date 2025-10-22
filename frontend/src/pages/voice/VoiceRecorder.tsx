import React from 'react'
import { Box, Typography, Card, CardContent } from '@mui/material'

const VoiceRecorder: React.FC = () => {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Voice Tracking
      </Typography>
      <Card>
        <CardContent>
          <Typography variant="body1">
            Voice recording and tracking will be implemented here.
          </Typography>
        </CardContent>
      </Card>
    </Box>
  )
}

export default VoiceRecorder
