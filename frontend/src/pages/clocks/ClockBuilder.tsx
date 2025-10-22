import React from 'react'
import { Box, Typography, Card, CardContent } from '@mui/material'

const ClockBuilder: React.FC = () => {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Clock Template Builder
      </Typography>
      <Card>
        <CardContent>
          <Typography variant="body1">
            Clock template builder will be implemented here.
          </Typography>
        </CardContent>
      </Card>
    </Box>
  )
}

export default ClockBuilder
