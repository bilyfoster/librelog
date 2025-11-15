import React from 'react'
import {
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TableContainer,
} from '@mui/material'

interface InventoryHeatmapProps {
  data: any
}

const InventoryHeatmap: React.FC<InventoryHeatmapProps> = ({ data }) => {
  // Handle different data structures
  if (!data) {
    return (
      <Paper sx={{ p: 2 }}>
        <Typography variant="h6" gutterBottom>
          Inventory Heatmap
        </Typography>
        <Typography variant="body2" color="text.secondary">
          No heatmap data available
        </Typography>
      </Paper>
    )
  }

  // If data is an array, render as table
  if (Array.isArray(data)) {
    if (data.length === 0) {
      return (
        <Paper sx={{ p: 2 }}>
          <Typography variant="h6" gutterBottom>
            Inventory Heatmap
          </Typography>
          <Typography variant="body2" color="text.secondary">
            No data available
          </Typography>
        </Paper>
      )
    }

    // Group by date and hour
    const grouped: Record<string, any[]> = {}
    data.forEach((item: any) => {
      const key = `${item.date || item.date_key || 'unknown'}-${item.hour || 'unknown'}`
      if (!grouped[key]) {
        grouped[key] = []
      }
      grouped[key].push(item)
    })

    return (
      <Paper sx={{ p: 2 }}>
        <Typography variant="h6" gutterBottom>
          Inventory Heatmap
        </Typography>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Date</TableCell>
                <TableCell>Hour</TableCell>
                <TableCell>Daypart</TableCell>
                <TableCell>Available</TableCell>
                <TableCell>Booked</TableCell>
                <TableCell>Sellout %</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {Object.entries(grouped).slice(0, 20).map(([key, items]) => {
                const firstItem = items[0]
                const totalAvailable = items.reduce((sum, item) => sum + (item.available || 0), 0)
                const totalBooked = items.reduce((sum, item) => sum + (item.booked || 0), 0)
                const selloutPercent = totalAvailable > 0 ? (totalBooked / totalAvailable) * 100 : 0
                
                return (
                  <TableRow key={key}>
                    <TableCell>
                      {firstItem.date ? new Date(firstItem.date).toLocaleDateString() : '-'}
                    </TableCell>
                    <TableCell>{firstItem.hour || '-'}</TableCell>
                    <TableCell>{firstItem.daypart || '-'}</TableCell>
                    <TableCell>{totalAvailable}</TableCell>
                    <TableCell>{totalBooked}</TableCell>
                    <TableCell>
                      <Box
                        sx={{
                          width: '100%',
                          height: 20,
                          bgcolor: 'grey.200',
                          borderRadius: 1,
                          position: 'relative',
                          overflow: 'hidden',
                        }}
                      >
                        <Box
                          sx={{
                            width: `${Math.min(selloutPercent, 100)}%`,
                            height: '100%',
                            bgcolor:
                              selloutPercent >= 90
                                ? 'error.main'
                                : selloutPercent >= 70
                                ? 'warning.main'
                                : 'success.main',
                          }}
                        />
                        <Typography
                          variant="caption"
                          sx={{
                            position: 'absolute',
                            top: '50%',
                            left: '50%',
                            transform: 'translate(-50%, -50%)',
                            fontWeight: 'bold',
                            color: selloutPercent > 50 ? 'white' : 'text.primary',
                          }}
                        >
                          {selloutPercent.toFixed(0)}%
                        </Typography>
                      </Box>
                    </TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    )
  }

  // If data is an object with heatmap structure
  if (typeof data === 'object') {
    return (
      <Paper sx={{ p: 2 }}>
        <Typography variant="h6" gutterBottom>
          Inventory Heatmap
        </Typography>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
          {Object.entries(data).map(([key, value]: [string, any]) => (
            <Box
              key={key}
              sx={{
                p: 1,
                bgcolor: 'primary.light',
                borderRadius: 1,
                minWidth: 100,
                textAlign: 'center',
              }}
            >
              <Typography variant="caption" display="block">
                {key}
              </Typography>
              <Typography variant="h6">
                {typeof value === 'number' ? value : JSON.stringify(value)}
              </Typography>
            </Box>
          ))}
        </Box>
      </Paper>
    )
  }

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Inventory Heatmap
      </Typography>
      <Typography variant="body2" color="text.secondary">
        Unable to render heatmap data
      </Typography>
    </Paper>
  )
}

export default InventoryHeatmap

