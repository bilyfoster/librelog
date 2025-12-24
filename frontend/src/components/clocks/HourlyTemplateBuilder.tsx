import React, { useState, useEffect } from 'react'
import { TRACK_TYPES, getTrackType, getTrackTypeChipColor } from '../../utils/trackTypes'
import {
  Box,
  Typography,
  Alert,
  TextField,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
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
  ArrowUpward,
  ArrowDownward,
  Edit,
  Delete,
  Newspaper,
  Campaign,
  MusicNote,
  AdsClick,
  Info,
  Radio,
} from '@mui/icons-material'

export interface ClockElement {
  id: string
  type: string
  title: string
  count: number
  duration: number
  fallback?: string
}

export interface HourlyTemplate {
  id?: string
  name: string
  description?: string
  hour: string // HH:MM format
  elements: ClockElement[]
}

interface HourlyTemplateBuilderProps {
  template?: HourlyTemplate
  onSave: (template: HourlyTemplate) => Promise<void>
  onCancel: () => void
}

const HourlyTemplateBuilder: React.FC<HourlyTemplateBuilderProps> = ({
  template,
  onSave,
  onCancel,
}) => {
  const [formData, setFormData] = useState<HourlyTemplate>(
    template || {
      name: '',
      description: '',
      hour: '00:00',
      elements: [],
    }
  )
  const [elementDialogOpen, setElementDialogOpen] = useState(false)
  const [elementSelectorOpen, setElementSelectorOpen] = useState(false)
  const [editingElement, setEditingElement] = useState<ClockElement | null>(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  // Ensure dialogs stay closed on mount and when template changes
  useEffect(() => {
    setElementDialogOpen(false)
    setElementSelectorOpen(false)
    setEditingElement(null)
  }, [template])

  // Available clock element types with icons
  const availableElementTypes = [
    { type: 'NEW', label: 'News Block', icon: Newspaper, color: '#ffc107', badge: 'NEW' },
    { type: 'PRO', label: 'Promo', icon: Campaign, color: '#e91e63', badge: 'PRO' },
    { type: 'MUS', label: 'Music', icon: MusicNote, color: '#4caf50', badge: 'MUS' },
    { type: 'ADV', label: 'Advertisement', icon: AdsClick, color: '#f44336', badge: 'ADV' },
    { type: 'PSA', label: 'PSA', icon: Info, color: '#2196f3', badge: 'PSA' },
    { type: 'ID', label: 'Station ID', icon: Radio, color: '#9c27b0', badge: 'ID' },
  ]

  const handleAddElement = () => {
    // Show element selector first
    setElementSelectorOpen(true)
  }

  const handleSelectElementType = (elementType: typeof availableElementTypes[0]) => {
    // Close selector and open editor with selected type
    setElementSelectorOpen(false)
    setEditingElement({
      id: Date.now().toString(),
      type: elementType.type,
      title: elementType.label,
      count: 1,
      duration: elementType.type === 'NEW' ? 180 : elementType.type === 'PRO' ? 30 : 180,
    })
    setElementDialogOpen(true)
  }

  const handleEditElement = (element: ClockElement) => {
    setEditingElement({ ...element })
    setElementDialogOpen(true)
  }

  const handleSaveElement = () => {
    if (!editingElement) return

    const newElements = [...formData.elements]
    const existingIndex = newElements.findIndex((e) => e.id === editingElement.id)

    if (existingIndex >= 0) {
      newElements[existingIndex] = editingElement
    } else {
      newElements.push(editingElement)
    }

    setFormData({
      ...formData,
      elements: newElements,
    })

    setElementDialogOpen(false)
    setEditingElement(null)
  }

  const handleDeleteElement = (elementId: string) => {
    setFormData({
      ...formData,
      elements: formData.elements.filter((e) => e.id !== elementId),
    })
  }

  const handleMoveElement = (elementId: string, direction: 'up' | 'down') => {
    const elements = [...formData.elements]
    const currentIndex = elements.findIndex((e) => e.id === elementId)

    if (currentIndex === -1) return

    const newIndex = direction === 'up' ? currentIndex - 1 : currentIndex + 1

    if (newIndex < 0 || newIndex >= elements.length) return

    ;[elements[currentIndex], elements[newIndex]] = [elements[newIndex], elements[currentIndex]]

    setFormData({
      ...formData,
      elements,
    })
  }

  const handleSave = async () => {
    if (!formData.name.trim()) {
      setError('Template name is required')
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

  const getTotalDuration = () => {
    return formData.elements.reduce((total, element) => {
      return total + element.duration * element.count
    }, 0)
  }

  const formatDuration = (seconds: number) => {
    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = seconds % 60
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" component="h2" sx={{ mb: 3, fontWeight: 500 }}>
        Hourly Template Builder
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Template Name"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            required
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Hour (HH:MM)"
            value={formData.hour}
            onChange={(e) => setFormData({ ...formData, hour: e.target.value })}
            placeholder="00:00"
          />
        </Grid>
      </Grid>

      <TextField
        fullWidth
        label="Description"
        value={formData.description || ''}
        onChange={(e) => setFormData({ ...formData, description: e.target.value })}
        sx={{ mb: 3 }}
      />

      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        mb: 2 
      }}>
        <Typography variant="h6" sx={{ fontWeight: 500 }}>
          Clock Elements ({formData.elements.length})
        </Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={handleAddElement}
        >
          Add Element
        </Button>
      </Box>

      <Stack spacing={1} sx={{ mb: 3 }}>
        {formData.elements.map((element, index) => {
          const typeInfo = getTrackType(element.type)
          const canMoveUp = index > 0
          const canMoveDown = index < formData.elements.length - 1

          return (
            <Card
              key={element.id}
              sx={{
                borderLeft: `4px solid ${typeInfo?.color || '#757575'}`,
                backgroundColor: typeInfo?.backgroundColor || 'transparent',
              }}
            >
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flex: 1 }}>
                    <Chip
                      label={element.type}
                      size="small"
                      sx={{ 
                        backgroundColor: typeInfo?.color || '#757575',
                        color: 'white',
                      }}
                    />
                    <Box>
                      <Typography variant="body1" sx={{ fontWeight: 500 }}>
                        {element.title || `${element.type} ${index + 1}`}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {element.count}x {formatDuration(element.duration)}
                        {element.fallback && ` • Fallback: ${element.fallback}`}
                      </Typography>
                    </Box>
                  </Box>
                  <Stack direction="row" spacing={0.5}>
                    <IconButton
                      size="small"
                      onClick={() => handleMoveElement(element.id, 'up')}
                      disabled={!canMoveUp}
                      title="Move up"
                    >
                      <ArrowUpward fontSize="small" />
                    </IconButton>
                    <IconButton
                      size="small"
                      onClick={() => handleMoveElement(element.id, 'down')}
                      disabled={!canMoveDown}
                      title="Move down"
                    >
                      <ArrowDownward fontSize="small" />
                    </IconButton>
                    <IconButton
                      size="small"
                      onClick={() => handleEditElement(element)}
                      title="Edit"
                    >
                      <Edit fontSize="small" />
                    </IconButton>
                    <IconButton
                      size="small"
                      onClick={() => handleDeleteElement(element.id)}
                      title="Delete"
                      color="error"
                    >
                      <Delete fontSize="small" />
                    </IconButton>
                  </Stack>
                </Box>
              </CardContent>
            </Card>
          )
        })}
      </Stack>

      {formData.elements.length === 0 && (
        <Box sx={{ textAlign: 'center', py: 6 }}>
          <Typography color="text.secondary">
            No elements added yet. Click "Add Element" to get started.
          </Typography>
        </Box>
      )}

      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center',
        pt: 2,
        borderTop: '1px solid',
        borderColor: 'divider',
      }}>
        <Typography variant="body1" sx={{ fontWeight: 500 }}>
          Total Duration: {formatDuration(getTotalDuration())}
        </Typography>
        <Stack direction="row" spacing={1}>
          <Button variant="outlined" onClick={onCancel}>
            Cancel
          </Button>
          <Button variant="contained" onClick={handleSave} disabled={saving}>
            {saving ? 'Saving...' : 'Save Template'}
          </Button>
        </Stack>
      </Box>

      {/* Element Type Selector Dialog */}
      <Dialog
        open={elementSelectorOpen}
        onClose={() => setElementSelectorOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Add Element</DialogTitle>
        <DialogContent>
          <Grid container spacing={1.5} sx={{ mt: 1 }}>
            {availableElementTypes.map((elementType) => {
              const IconComponent = elementType.icon
              return (
                <Grid item xs={12} sm={6} md={4} key={elementType.type}>
                  <Card
                    sx={{
                      cursor: 'pointer',
                      border: '2px solid transparent',
                      transition: 'all 0.2s',
                      '&:hover': {
                        borderColor: elementType.color,
                        boxShadow: `0 2px 8px ${elementType.color}40`,
                        transform: 'translateY(-2px)',
                      },
                    }}
                    onClick={() => handleSelectElementType(elementType)}
                  >
                    <CardContent>
                      <Box sx={{ 
                        display: 'flex', 
                        alignItems: 'center', 
                        justifyContent: 'space-between',
                        mb: 1
                      }}>
                        <Chip
                          label={elementType.badge}
                          size="small"
                          sx={{ 
                            backgroundColor: elementType.color,
                            color: 'white',
                            fontSize: '0.75rem',
                          }}
                        />
                      </Box>
                      <Box sx={{ 
                        display: 'flex', 
                        alignItems: 'center', 
                        gap: 1,
                        mb: 0.5
                      }}>
                        <IconComponent sx={{ color: elementType.color, fontSize: '1.25rem' }} />
                        <Typography variant="body1" sx={{ fontWeight: 500 }}>
                          {elementType.label}
                        </Typography>
                      </Box>
                      <Typography variant="body2" color="text.secondary" sx={{ ml: 4 }}>
                        1x {elementType.type === 'NEW' ? '3:00' : elementType.type === 'PRO' ? '0:30' : '3:00'}
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
              )
            })}
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setElementSelectorOpen(false)}>Cancel</Button>
        </DialogActions>
      </Dialog>

      {/* Element Editor Dialog */}
      <Dialog
        open={elementDialogOpen}
        onClose={() => {
          setElementDialogOpen(false)
          setEditingElement(null)
        }}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>
          {editingElement?.id ? 'Edit Element' : 'Add Element'}
        </DialogTitle>
        <DialogContent>
          {editingElement && (
            <Stack spacing={2} sx={{ mt: 1 }}>
              <FormControl fullWidth>
                <InputLabel>Element Type</InputLabel>
                <Select
                  value={editingElement.type}
                  label="Element Type"
                  onChange={(e) => setEditingElement({ ...editingElement, type: e.target.value })}
                >
                  {TRACK_TYPES.map((type) => (
                    <MenuItem key={type.value} value={type.value}>
                      {type.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <TextField
                fullWidth
                label="Title"
                value={editingElement.title}
                onChange={(e) => setEditingElement({ ...editingElement, title: e.target.value })}
              />

              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <TextField
                    fullWidth
                    label="Count"
                    type="number"
                    value={editingElement.count.toString()}
                    onChange={(e) =>
                      setEditingElement({
                        ...editingElement,
                        count: parseInt(e.target.value) || 1,
                      })
                    }
                  />
                </Grid>
                <Grid item xs={6}>
                  <TextField
                    fullWidth
                    label="Duration (seconds)"
                    type="number"
                    value={editingElement.duration.toString()}
                    onChange={(e) =>
                      setEditingElement({
                        ...editingElement,
                        duration: parseInt(e.target.value) || 30,
                      })
                    }
                  />
                </Grid>
              </Grid>

              <TextField
                fullWidth
                label="Fallback"
                value={editingElement.fallback || ''}
                onChange={(e) => setEditingElement({ ...editingElement, fallback: e.target.value })}
              />
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setElementDialogOpen(false)
            setEditingElement(null)
          }}>
            Cancel
          </Button>
          <Button variant="contained" onClick={handleSaveElement}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default HourlyTemplateBuilder
