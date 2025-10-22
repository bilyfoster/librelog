import React, { useState, useEffect } from 'react'
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  TextField,
  Grid,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  Divider,
  Alert,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Preview as PreviewIcon,
  Publish as PublishIcon,
  Save as SaveIcon,
} from '@mui/icons-material'
import api from '../../utils/api'

interface ClockElement {
  id: string
  type: string
  title: string
  count: number
  duration: number
  fallback?: string
}

interface ClockTemplate {
  id?: number
  name: string
  description?: string
  json_layout: {
    hour: string
    elements: ClockElement[]
  }
}

const ClockBuilder: React.FC = () => {
  const [templates, setTemplates] = useState<ClockTemplate[]>([])
  const [currentTemplate, setCurrentTemplate] = useState<ClockTemplate>({
    name: '',
    description: '',
    json_layout: {
      hour: '00:00',
      elements: []
    }
  })
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [previewOpen, setPreviewOpen] = useState(false)
  const [previewData, setPreviewData] = useState<any>(null)
  const [elementDialogOpen, setElementDialogOpen] = useState(false)
  const [editingElement, setEditingElement] = useState<ClockElement | null>(null)

  const elementTypes = [
    { value: 'MUS', label: 'Music', color: 'primary' },
    { value: 'ADV', label: 'Advertisement', color: 'secondary' },
    { value: 'PSA', label: 'PSA', color: 'success' },
    { value: 'LIN', label: 'Liner', color: 'info' },
    { value: 'INT', label: 'Interview', color: 'warning' },
    { value: 'PRO', label: 'Promo', color: 'error' },
    { value: 'BED', label: 'Bed', color: 'default' }
  ]

  useEffect(() => {
    loadTemplates()
  }, [])

  const loadTemplates = async () => {
    try {
      const response = await api.get('/clocks/')
      setTemplates(response.data.templates)
    } catch (err) {
      console.error('Failed to load templates:', err)
    }
  }

  const handleSaveTemplate = async () => {
    if (!currentTemplate.name.trim()) {
      setError('Template name is required')
      return
    }

    setSaving(true)
    setError('')

    try {
      if (currentTemplate.id) {
        await api.put(`/clocks/${currentTemplate.id}`, currentTemplate)
        setSuccess('Template updated successfully')
      } else {
        await api.post('/clocks/', currentTemplate)
        setSuccess('Template created successfully')
      }
      
      await loadTemplates()
      setCurrentTemplate({
        name: '',
        description: '',
        json_layout: {
          hour: '00:00',
          elements: []
        }
      })
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to save template')
    } finally {
      setSaving(false)
    }
  }

  const handleLoadTemplate = (template: ClockTemplate) => {
    setCurrentTemplate(template)
    setError('')
    setSuccess('')
  }

  const handleAddElement = () => {
    setEditingElement({
      id: Date.now().toString(),
      type: 'MUS',
      title: '',
      count: 1,
      duration: 180
    })
    setElementDialogOpen(true)
  }

  const handleEditElement = (element: ClockElement) => {
    setEditingElement(element)
    setElementDialogOpen(true)
  }

  const handleSaveElement = () => {
    if (!editingElement) return

    const newElements = [...currentTemplate.json_layout.elements]
    const existingIndex = newElements.findIndex(e => e.id === editingElement.id)

    if (existingIndex >= 0) {
      newElements[existingIndex] = editingElement
    } else {
      newElements.push(editingElement)
    }

    setCurrentTemplate({
      ...currentTemplate,
      json_layout: {
        ...currentTemplate.json_layout,
        elements: newElements
      }
    })

    setElementDialogOpen(false)
    setEditingElement(null)
  }

  const handleDeleteElement = (elementId: string) => {
    const newElements = currentTemplate.json_layout.elements.filter(e => e.id !== elementId)
    setCurrentTemplate({
      ...currentTemplate,
      json_layout: {
        ...currentTemplate.json_layout,
        elements: newElements
      }
    })
  }

  const handlePreview = async () => {
    if (!currentTemplate.id) {
      setError('Please save the template before previewing')
      return
    }

    try {
      const response = await api.get(`/clocks/${currentTemplate.id}/preview`)
      setPreviewData(response.data)
      setPreviewOpen(true)
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to generate preview')
    }
  }

  const handleExport = async () => {
    if (!currentTemplate.id) {
      setError('Please save the template before exporting')
      return
    }

    try {
      await api.post(`/clocks/${currentTemplate.id}/export`)
      setSuccess('Template exported to LibreTime successfully')
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to export template')
    }
  }

  const getTotalDuration = () => {
    return currentTemplate.json_layout.elements.reduce((total, element) => {
      return total + (element.duration * element.count)
    }, 0)
  }

  const formatDuration = (seconds: number) => {
    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = seconds % 60
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Clock Template Builder
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 2 }}>
          {success}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* Template List */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Saved Templates
              </Typography>
              <List>
                {templates.map((template) => (
                  <ListItem
                    key={template.id}
                    button
                    onClick={() => handleLoadTemplate(template)}
                    selected={currentTemplate.id === template.id}
                  >
                    <ListItemText
                      primary={template.name}
                      secondary={`${template.json_layout.elements.length} elements`}
                    />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>

        {/* Template Editor */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Template Editor
              </Typography>

              <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Template Name"
                    value={currentTemplate.name}
                    onChange={(e) => setCurrentTemplate({
                      ...currentTemplate,
                      name: e.target.value
                    })}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Hour (HH:MM)"
                    value={currentTemplate.json_layout.hour}
                    onChange={(e) => setCurrentTemplate({
                      ...currentTemplate,
                      json_layout: {
                        ...currentTemplate.json_layout,
                        hour: e.target.value
                      }
                    })}
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Description"
                    multiline
                    rows={2}
                    value={currentTemplate.description || ''}
                    onChange={(e) => setCurrentTemplate({
                      ...currentTemplate,
                      description: e.target.value
                    })}
                  />
                </Grid>
              </Grid>

              <Divider sx={{ my: 2 }} />

              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">
                  Clock Elements ({currentTemplate.json_layout.elements.length})
                </Typography>
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={handleAddElement}
                >
                  Add Element
                </Button>
              </Box>

              <List>
                {currentTemplate.json_layout.elements.map((element, index) => (
                  <ListItem key={element.id}>
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Chip
                            label={element.type}
                            color={elementTypes.find(t => t.value === element.type)?.color as any}
                            size="small"
                          />
                          <Typography variant="body1">
                            {element.title || `${element.type} ${index + 1}`}
                          </Typography>
                          <Typography variant="body2" color="textSecondary">
                            ({element.count}x {formatDuration(element.duration)})
                          </Typography>
                        </Box>
                      }
                      secondary={element.fallback ? `Fallback: ${element.fallback}` : undefined}
                    />
                    <ListItemSecondaryAction>
                      <IconButton onClick={() => handleEditElement(element)}>
                        <EditIcon />
                      </IconButton>
                      <IconButton onClick={() => handleDeleteElement(element.id)}>
                        <DeleteIcon />
                      </IconButton>
                    </ListItemSecondaryAction>
                  </ListItem>
                ))}
              </List>

              {currentTemplate.json_layout.elements.length === 0 && (
                <Typography variant="body2" color="textSecondary" sx={{ textAlign: 'center', py: 4 }}>
                  No elements added yet. Click "Add Element" to get started.
                </Typography>
              )}

              <Divider sx={{ my: 2 }} />

              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="body1">
                  Total Duration: {formatDuration(getTotalDuration())}
                </Typography>
                <Box sx={{ display: 'flex', gap: 1 }}>
                  <Button
                    variant="outlined"
                    startIcon={<PreviewIcon />}
                    onClick={handlePreview}
                    disabled={!currentTemplate.id}
                  >
                    Preview
                  </Button>
                  <Button
                    variant="outlined"
                    startIcon={<PublishIcon />}
                    onClick={handleExport}
                    disabled={!currentTemplate.id}
                  >
                    Export to LibreTime
                  </Button>
                  <Button
                    variant="contained"
                    startIcon={saving ? <CircularProgress size={20} /> : <SaveIcon />}
                    onClick={handleSaveTemplate}
                    disabled={saving}
                  >
                    {saving ? 'Saving...' : 'Save Template'}
                  </Button>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Element Editor Dialog */}
      <Dialog open={elementDialogOpen} onClose={() => setElementDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editingElement?.id ? 'Edit Element' : 'Add Element'}
        </DialogTitle>
        <DialogContent>
          {editingElement && (
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12}>
                <FormControl fullWidth>
                  <InputLabel>Element Type</InputLabel>
                  <Select
                    value={editingElement.type}
                    onChange={(e) => setEditingElement({
                      ...editingElement,
                      type: e.target.value
                    })}
                  >
                    {elementTypes.map((type) => (
                      <MenuItem key={type.value} value={type.value}>
                        {type.label}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Title"
                  value={editingElement.title}
                  onChange={(e) => setEditingElement({
                    ...editingElement,
                    title: e.target.value
                  })}
                />
              </Grid>
              <Grid item xs={6}>
                <TextField
                  fullWidth
                  label="Count"
                  type="number"
                  value={editingElement.count}
                  onChange={(e) => setEditingElement({
                    ...editingElement,
                    count: parseInt(e.target.value) || 1
                  })}
                />
              </Grid>
              <Grid item xs={6}>
                <TextField
                  fullWidth
                  label="Duration (seconds)"
                  type="number"
                  value={editingElement.duration}
                  onChange={(e) => setEditingElement({
                    ...editingElement,
                    duration: parseInt(e.target.value) || 30
                  })}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Fallback"
                  value={editingElement.fallback || ''}
                  onChange={(e) => setEditingElement({
                    ...editingElement,
                    fallback: e.target.value
                  })}
                />
              </Grid>
            </Grid>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setElementDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleSaveElement} variant="contained">Save</Button>
        </DialogActions>
      </Dialog>

      {/* Preview Dialog */}
      <Dialog open={previewOpen} onClose={() => setPreviewOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Clock Template Preview</DialogTitle>
        <DialogContent>
          {previewData && (
            <Box>
              <Typography variant="h6" gutterBottom>
                {previewData.template_name} - {previewData.hour}
              </Typography>
              <Typography variant="body2" color="textSecondary" gutterBottom>
                Total Duration: {formatDuration(previewData.total_duration)}
              </Typography>
              <List>
                {previewData.timeline.map((item: any, index: number) => (
                  <ListItem key={index}>
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Typography variant="body2" sx={{ minWidth: 60 }}>
                            {item.time}
                          </Typography>
                          <Chip
                            label={item.element.type}
                            color={elementTypes.find(t => t.value === item.element.type)?.color as any}
                            size="small"
                          />
                          <Typography variant="body1">
                            {item.element.title}
                          </Typography>
                        </Box>
                      }
                      secondary={`${item.element.count}x ${formatDuration(item.element.duration)}`}
                    />
                  </ListItem>
                ))}
              </List>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPreviewOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default ClockBuilder
