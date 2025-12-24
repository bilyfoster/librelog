import React, { useState } from 'react'
import { HourlyTemplate } from './HourlyTemplateBuilder'
import {
  Box,
  Typography,
  Alert,
  TextField,
  Button,
  Card,
  CardContent,
  Chip,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Grid,
  IconButton,
  Stack,
} from '@mui/material'
import {
  Add,
  Close,
  Delete,
} from '@mui/icons-material'

export interface DailyTemplate {
  id?: string
  name: string
  description?: string
  hourlyTemplates: {
    hour: string // HH:MM format
    templateId: number
    templateName?: string
  }[]
}

interface DailyTemplateBuilderProps {
  hourlyTemplates: HourlyTemplate[]
  template?: DailyTemplate
  onSave: (template: DailyTemplate) => Promise<void>
  onCancel: () => void
}

const DailyTemplateBuilder: React.FC<DailyTemplateBuilderProps> = ({
  hourlyTemplates,
  template,
  onSave,
  onCancel,
}) => {
  const [formData, setFormData] = useState<DailyTemplate>(
    template || {
      name: '',
      description: '',
      hourlyTemplates: [],
    }
  )
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [selectedHour, setSelectedHour] = useState('00:00')
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | ''>('')

  // Generate hours for the day (00:00 to 23:00)
  const hours = Array.from({ length: 24 }, (_, i) => {
    const hour = i.toString().padStart(2, '0')
    return `${hour}:00`
  })

  const handleAddHourlyTemplate = () => {
    if (!selectedTemplateId) {
      setError('Please select a template')
      return
    }

    // Check if hour is already assigned
    if (formData.hourlyTemplates.some((ht) => ht.hour === selectedHour)) {
      setError(`Hour ${selectedHour} is already assigned`)
      return
    }

    const selectedTemplate = hourlyTemplates.find((t) => t.id === selectedTemplateId)
    setFormData({
      ...formData,
      hourlyTemplates: [
        ...formData.hourlyTemplates,
        {
          hour: selectedHour,
          templateId: selectedTemplateId as number,
          templateName: selectedTemplate?.name,
        },
      ].sort((a, b) => a.hour.localeCompare(b.hour)),
    })

    setError('')
    setSelectedHour('00:00')
    setSelectedTemplateId('')
  }

  const handleRemoveHourlyTemplate = (hour: string) => {
    setFormData({
      ...formData,
      hourlyTemplates: formData.hourlyTemplates.filter((ht) => ht.hour !== hour),
    })
  }

  const handleSave = async () => {
    if (!formData.name.trim()) {
      setError('Template name is required')
      return
    }

    if (formData.hourlyTemplates.length === 0) {
      setError('Please add at least one hourly template')
      return
    }

    setSaving(true)
    setError('')

    try {
      await onSave(formData)
    } catch (err: any) {
      setError(err.message || 'Failed to save template')
    } finally {
      setSaving(false)
    }
  }

  const getTemplateForHour = (hour: string) => {
    return formData.hourlyTemplates.find((ht) => ht.hour === hour)
  }

  const availableHours = hours.filter(
    (hour) => !formData.hourlyTemplates.some((ht) => ht.hour === hour)
  )

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" component="h2" sx={{ mb: 3, fontWeight: 500 }}>
        Daily Template Builder
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      <Stack spacing={2} sx={{ mb: 3 }}>
        <TextField
          fullWidth
          label="Template Name"
          value={formData.name}
          onChange={(e) => setFormData({ ...formData, name: e.target.value })}
          required
        />
        <TextField
          fullWidth
          label="Description"
          value={formData.description || ''}
          onChange={(e) => setFormData({ ...formData, description: e.target.value })}
        />
      </Stack>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2, fontWeight: 500 }}>
            Add Hourly Template
          </Typography>
          <Grid container spacing={2} alignItems="flex-end">
            <Grid item xs={12} sm={4}>
              <FormControl fullWidth>
                <InputLabel>Hour</InputLabel>
                <Select
                  value={selectedHour}
                  label="Hour"
                  onChange={(e) => setSelectedHour(e.target.value)}
                >
                  {availableHours.map((hour) => (
                    <MenuItem key={hour} value={hour}>
                      {hour}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={5}>
              <FormControl fullWidth>
                <InputLabel>Hourly Template</InputLabel>
                <Select
                  value={selectedTemplateId.toString()}
                  label="Hourly Template"
                  onChange={(e) => setSelectedTemplateId(parseInt(e.target.value) || '')}
                >
                  <MenuItem value="">Select a template</MenuItem>
                  {hourlyTemplates.map((template) => (
                    <MenuItem key={template.id} value={template.id?.toString() || ''}>
                      {template.name} ({template.hour})
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={3}>
              <Button
                variant="contained"
                startIcon={<Add />}
                onClick={handleAddHourlyTemplate}
                fullWidth
              >
                Add
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Typography variant="h6" sx={{ mb: 2, fontWeight: 500 }}>
        Daily Schedule ({formData.hourlyTemplates.length} hours assigned)
      </Typography>

      <Grid container spacing={1.5} sx={{ mb: 3 }}>
        {hours.map((hour) => {
          const assignedTemplate = getTemplateForHour(hour)
          return (
            <Grid item xs={6} sm={4} md={3} lg={2} key={hour}>
              <Card
                sx={{
                  border: assignedTemplate 
                    ? '2px solid' 
                    : '1px solid',
                  borderColor: assignedTemplate 
                    ? 'primary.main' 
                    : 'divider',
                  backgroundColor: assignedTemplate 
                    ? 'action.hover' 
                    : 'background.paper',
                  position: 'relative',
                }}
              >
                <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
                  <Box sx={{ 
                    display: 'flex', 
                    justifyContent: 'space-between', 
                    alignItems: 'center',
                    mb: assignedTemplate ? 1 : 0
                  }}>
                    <Typography variant="body1" sx={{ fontWeight: 'bold' }}>
                      {hour}
                    </Typography>
                    {assignedTemplate && (
                      <IconButton
                        size="small"
                        onClick={() => handleRemoveHourlyTemplate(hour)}
                        title="Remove"
                        sx={{ p: 0.5 }}
                      >
                        <Close fontSize="small" />
                      </IconButton>
                    )}
                  </Box>
                  {assignedTemplate ? (
                    <Chip
                      label={assignedTemplate.templateName || `Template ${assignedTemplate.templateId}`}
                      size="small"
                      sx={{ fontSize: '0.75rem' }}
                    />
                  ) : (
                    <Typography variant="body2" color="text.secondary" sx={{ fontSize: '0.875rem' }}>
                      Not assigned
                    </Typography>
                  )}
                </CardContent>
              </Card>
            </Grid>
          )
        })}
      </Grid>

      {formData.hourlyTemplates.length > 0 && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 2, fontWeight: 500 }}>
              Schedule Preview
            </Typography>
            <Stack spacing={1}>
              {formData.hourlyTemplates.map((ht) => (
                <Box
                  key={ht.hour}
                  sx={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    p: 1,
                    backgroundColor: 'action.hover',
                    borderRadius: 1,
                  }}
                >
                  <Box>
                    <Typography component="span" sx={{ fontWeight: 'bold', mr: 1.5 }}>
                      {ht.hour}
                    </Typography>
                    <Typography component="span">
                      {ht.templateName || `Template ${ht.templateId}`}
                    </Typography>
                  </Box>
                  <IconButton
                    size="small"
                    onClick={() => handleRemoveHourlyTemplate(ht.hour)}
                    color="error"
                  >
                    <Delete fontSize="small" />
                  </IconButton>
                </Box>
              ))}
            </Stack>
          </CardContent>
        </Card>
      )}

      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'flex-end', 
        gap: 1,
        pt: 2,
        borderTop: '1px solid',
        borderColor: 'divider',
      }}>
        <Button variant="outlined" onClick={onCancel}>
          Cancel
        </Button>
        <Button variant="contained" onClick={handleSave} disabled={saving}>
          {saving ? 'Saving...' : 'Save Daily Template'}
        </Button>
      </Box>
    </Box>
  )
}

export default DailyTemplateBuilder
