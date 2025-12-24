import React, { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../../utils/api'
import HourlyTemplateBuilder, { HourlyTemplate } from '../../components/clocks/HourlyTemplateBuilder'
import DailyTemplateBuilder, { DailyTemplate } from '../../components/clocks/DailyTemplateBuilder'
import {
  Box,
  Typography,
  Alert,
  Tabs,
  Tab,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  Grid,
  IconButton,
  Stack,
} from '@mui/material'
import {
  Add,
  Edit,
  Delete,
} from '@mui/icons-material'

const ClockBuilder: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'hourly' | 'daily' | 'templates'>(0)
  const [hourlyTemplates, setHourlyTemplates] = useState<HourlyTemplate[]>([])
  const [dailyTemplates, setDailyTemplates] = useState<DailyTemplate[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [editingHourlyTemplate, setEditingHourlyTemplate] = useState<HourlyTemplate | null>(null)
  const [editingDailyTemplate, setEditingDailyTemplate] = useState<DailyTemplate | null>(null)
  const [hourlyBuilderOpen, setHourlyBuilderOpen] = useState(false)
  const [dailyBuilderOpen, setDailyBuilderOpen] = useState(false)
  const queryClient = useQueryClient()

  // Load templates
  const { data: templatesData, isLoading } = useQuery({
    queryKey: ['clock-templates'],
    queryFn: async () => {
      const response = await api.get('/clocks/')
      return response.data
    },
  })

  useEffect(() => {
    if (templatesData) {
      // Separate hourly and daily templates
      // For now, we'll treat all templates as hourly until backend supports daily
      const hourly = (templatesData.templates || []).map((t: any) => ({
        id: t.id,
        name: t.name,
        description: t.description,
        hour: t.json_layout?.hour || '00:00',
        elements: t.json_layout?.elements || [],
      }))
      setHourlyTemplates(hourly)
    }
  }, [templatesData])

  const saveHourlyTemplate = async (template: HourlyTemplate) => {
    setLoading(true)
    setError('')
    setSuccess('')

    try {
      const payload = {
        name: template.name,
        description: template.description,
        json_layout: {
          hour: template.hour,
          elements: template.elements,
        },
      }

      if (template.id) {
        await api.put(`/clocks/${template.id}`, payload)
        setSuccess('Hourly template updated successfully')
      } else {
        await api.post('/clocks/', payload)
        setSuccess('Hourly template created successfully')
      }

      queryClient.invalidateQueries({ queryKey: ['clock-templates'] })
      setHourlyBuilderOpen(false)
      setEditingHourlyTemplate(null)
    } catch (err: any) {
      const message = err?.response?.data?.detail || err?.message || 'Failed to save template'
      setError(message)
      throw new Error(message)
    } finally {
      setLoading(false)
    }
  }

  const saveDailyTemplate = async (template: DailyTemplate) => {
    setLoading(true)
    setError('')
    setSuccess('')

    try {
      // For now, save as a special template type
      // Backend will need to support daily templates
      const payload = {
        name: template.name,
        description: template.description,
        template_type: 'daily',
        json_layout: {
          hourly_templates: template.hourlyTemplates,
        },
      }

      if (template.id) {
        await api.put(`/clocks/${template.id}`, payload)
        setSuccess('Daily template updated successfully')
      } else {
        await api.post('/clocks/', payload)
        setSuccess('Daily template created successfully')
      }

      queryClient.invalidateQueries({ queryKey: ['clock-templates'] })
      setDailyBuilderOpen(false)
      setEditingDailyTemplate(null)
    } catch (err: any) {
      const message = err?.response?.data?.detail || err?.message || 'Failed to save template'
      setError(message)
      throw new Error(message)
    } finally {
      setLoading(false)
    }
  }

  const handleDeleteTemplate = async (id?: string, type: 'hourly' | 'daily') => {
    if (!confirm('Are you sure you want to delete this template?')) return

    try {
      await api.delete(`/clocks/${id}`)
      queryClient.invalidateQueries({ queryKey: ['clock-templates'] })
      setSuccess(`${type === 'hourly' ? 'Hourly' : 'Daily'} template deleted successfully`)
    } catch (err: any) {
      setError(err?.response?.data?.detail || 'Failed to delete template')
    }
  }

  const handleNewHourlyTemplate = () => {
    setEditingHourlyTemplate(null)
    setHourlyBuilderOpen(true)
  }

  const handleEditHourlyTemplate = (template: HourlyTemplate) => {
    setEditingHourlyTemplate(template)
    setHourlyBuilderOpen(true)
  }

  const handleNewDailyTemplate = () => {
    setEditingDailyTemplate(null)
    setDailyBuilderOpen(true)
  }

  const handleEditDailyTemplate = (template: DailyTemplate) => {
    setEditingDailyTemplate(template)
    setDailyBuilderOpen(true)
  }

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue)
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" component="h1" sx={{ mb: 3, fontWeight: 500 }}>
        Clock Template Builder
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess('')}>
          {success}
        </Alert>
      )}

      <Tabs value={activeTab} onChange={handleTabChange} sx={{ mb: 3 }}>
        <Tab label="Hourly Templates" />
        <Tab label="Daily Templates" />
        <Tab label="All Templates" />
      </Tabs>

      {activeTab === 0 && (
        <Box>
          <Box sx={{ mb: 2 }}>
            <Button
              variant="contained"
              startIcon={<Add />}
              onClick={handleNewHourlyTemplate}
            >
              New Hourly Template
            </Button>
          </Box>

          {isLoading ? (
            <Box sx={{ textAlign: 'center', py: 6 }}>
              <CircularProgress />
            </Box>
          ) : hourlyTemplates.length === 0 ? (
            <Card>
              <CardContent sx={{ py: 6, textAlign: 'center' }}>
                <Typography color="text.secondary">
                  No hourly templates yet. Create one to get started.
                </Typography>
              </CardContent>
            </Card>
          ) : (
            <Grid container spacing={2}>
              {hourlyTemplates.map((template) => (
                <Grid item xs={12} sm={6} md={4} key={template.id}>
                  <Card>
                    <CardContent>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', mb: 1.5 }}>
                        <Box>
                          <Typography variant="h6" sx={{ mb: 0.5 }}>
                            {template.name}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            {template.hour} • {template.elements.length} elements
                          </Typography>
                        </Box>
                      </Box>
                      {template.description && (
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                          {template.description}
                        </Typography>
                      )}
                      <Stack direction="row" spacing={1}>
                        <Button
                          variant="outlined"
                          size="small"
                          startIcon={<Edit />}
                          onClick={() => handleEditHourlyTemplate(template)}
                        >
                          Edit
                        </Button>
                        <Button
                          variant="outlined"
                          color="error"
                          size="small"
                          startIcon={<Delete />}
                          onClick={() => template.id && handleDeleteTemplate(template.id, 'hourly')}
                        >
                          Delete
                        </Button>
                      </Stack>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          )}
        </Box>
      )}

      {activeTab === 1 && (
        <Box>
          <Box sx={{ mb: 2 }}>
            <Button
              variant="contained"
              startIcon={<Add />}
              onClick={handleNewDailyTemplate}
            >
              New Daily Template
            </Button>
          </Box>

          {dailyTemplates.length === 0 ? (
            <Card>
              <CardContent sx={{ py: 6, textAlign: 'center' }}>
                <Typography color="text.secondary">
                  No daily templates yet. Create one by selecting hourly templates for each hour of the day.
                </Typography>
              </CardContent>
            </Card>
          ) : (
            <Grid container spacing={2}>
              {dailyTemplates.map((template) => (
                <Grid item xs={12} sm={6} md={4} key={template.id}>
                  <Card>
                    <CardContent>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', mb: 1.5 }}>
                        <Box>
                          <Typography variant="h6" sx={{ mb: 0.5 }}>
                            {template.name}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            {template.hourlyTemplates.length} hours assigned
                          </Typography>
                        </Box>
                      </Box>
                      {template.description && (
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                          {template.description}
                        </Typography>
                      )}
                      <Stack direction="row" spacing={1}>
                        <Button
                          variant="outlined"
                          size="small"
                          startIcon={<Edit />}
                          onClick={() => handleEditDailyTemplate(template)}
                        >
                          Edit
                        </Button>
                        <Button
                          variant="outlined"
                          color="error"
                          size="small"
                          startIcon={<Delete />}
                          onClick={() => template.id && handleDeleteTemplate(template.id, 'daily')}
                        >
                          Delete
                        </Button>
                      </Stack>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          )}
        </Box>
      )}

      {activeTab === 2 && (
        <Grid container spacing={2}>
          {[...hourlyTemplates, ...dailyTemplates].map((template) => (
            <Grid item xs={12} sm={6} md={4} key={template.id}>
              <Card>
                <CardContent>
                  <Typography variant="h6" sx={{ mb: 1 }}>
                    {template.name}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {('hour' in template ? 'Hourly' : 'Daily')} Template
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Hourly Template Builder Dialog */}
      <Dialog
        open={hourlyBuilderOpen}
        onClose={() => {
          setHourlyBuilderOpen(false)
          setEditingHourlyTemplate(null)
        }}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          {editingHourlyTemplate ? 'Edit Hourly Template' : 'New Hourly Template'}
        </DialogTitle>
        <DialogContent>
          <HourlyTemplateBuilder
            template={editingHourlyTemplate || undefined}
            onSave={saveHourlyTemplate}
            onCancel={() => {
              setHourlyBuilderOpen(false)
              setEditingHourlyTemplate(null)
            }}
          />
        </DialogContent>
      </Dialog>

      {/* Daily Template Builder Dialog */}
      <Dialog
        open={dailyBuilderOpen}
        onClose={() => {
          setDailyBuilderOpen(false)
          setEditingDailyTemplate(null)
        }}
        maxWidth="lg"
        fullWidth
      >
        <DialogTitle>
          {editingDailyTemplate ? 'Edit Daily Template' : 'New Daily Template'}
        </DialogTitle>
        <DialogContent>
          <DailyTemplateBuilder
            hourlyTemplates={hourlyTemplates}
            template={editingDailyTemplate || undefined}
            onSave={saveDailyTemplate}
            onCancel={() => {
              setDailyBuilderOpen(false)
              setEditingDailyTemplate(null)
            }}
          />
        </DialogContent>
      </Dialog>
    </Box>
  )
}

export default ClockBuilder
