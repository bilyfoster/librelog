import React, { useState, useCallback } from 'react'
import { useDropzone } from 'react-dropzone'
import {
  Box,
  TextField,
  Button,
  Typography,
  LinearProgress,
  Alert,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Paper,
} from '@mui/material'
import {
  CloudUpload as CloudUploadIcon,
  AudioFile as AudioFileIcon,
} from '@mui/icons-material'
import { uploadCopy, createCopy, getOrdersProxy, getAdvertisersProxy } from '../../utils/api'
import api from '../../utils/api'

interface CopyUploadProps {
  onSuccess?: () => void
  onCancel?: () => void
}

const CopyUpload: React.FC<CopyUploadProps> = ({ onSuccess, onCancel }) => {
  const [file, setFile] = useState<File | null>(null)
  const [title, setTitle] = useState('')
  const [orderId, setOrderId] = useState<number | ''>('')
  const [advertiserId, setAdvertiserId] = useState<number | ''>('')
  const [scriptText, setScriptText] = useState('')
  const [expirationDate, setExpirationDate] = useState('')
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [audioPreview, setAudioPreview] = useState<string | null>(null)

  // Fetch orders and advertisers for dropdowns
  const [orders, setOrders] = useState<any[]>([])
  const [advertisers, setAdvertisers] = useState<any[]>([])

  React.useEffect(() => {
    const loadData = async () => {
      try {
        // Use server-side proxy endpoints - all processing happens on backend
        const [ordersData, advertisersData] = await Promise.all([
          getOrdersProxy({ limit: 100 }),
          getAdvertisersProxy({ limit: 100 }),
        ])
        setOrders(Array.isArray(ordersData) ? ordersData : [])
        setAdvertisers(Array.isArray(advertisersData) ? advertisersData : [])
      } catch (err) {
        console.error('Failed to load orders/advertisers:', err)
      }
    }
    loadData()
  }, [])

  const onDrop = useCallback((acceptedFiles: File[]) => {
    const selectedFile = acceptedFiles[0]
    if (selectedFile) {
      // Check if it's an audio file
      if (!selectedFile.type.startsWith('audio/')) {
        setError('Please select an audio file')
        return
      }
      setFile(selectedFile)
      setError(null)
      
      // Create preview URL
      const previewUrl = URL.createObjectURL(selectedFile)
      setAudioPreview(previewUrl)
      
      // Auto-fill title if empty
      if (!title) {
        setTitle(selectedFile.name.replace(/\.[^/.]+$/, ''))
      }
    }
  }, [title])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'audio/*': ['.mp3', '.wav', '.ogg', '.m4a', '.aac'],
    },
    maxFiles: 1,
  })

  const handleSubmit = async () => {
    if (!title.trim()) {
      setError('Title is required')
      return
    }

    setError(null)
    setUploading(true)
    setUploadProgress(0)

    try {
      if (file) {
        // Upload with file
        await uploadCopy(
          file,
          title,
          orderId ? Number(orderId) : undefined,
          advertiserId ? Number(advertiserId) : undefined,
          (progress) => setUploadProgress(progress)
        )
      } else {
        // Create copy without file (script only)
        await createCopy({
          title,
          order_id: orderId ? Number(orderId) : undefined,
          advertiser_id: advertiserId ? Number(advertiserId) : undefined,
          script_text: scriptText || undefined,
          expires_at: expirationDate || undefined,
        })
      }

      // Clean up preview URL
      if (audioPreview) {
        URL.revokeObjectURL(audioPreview)
      }

      // Reset form
      setFile(null)
      setTitle('')
      setOrderId('')
      setAdvertiserId('')
      setScriptText('')
      setExpirationDate('')
      setAudioPreview(null)

      if (onSuccess) {
        onSuccess()
      }
    } catch (err: any) {
      console.error('Copy upload error:', err)
      setError(err.response?.data?.detail || err.message || 'Failed to upload copy')
    } finally {
      setUploading(false)
      setUploadProgress(0)
    }
  }

  const handleCancel = () => {
    if (audioPreview) {
      URL.revokeObjectURL(audioPreview)
    }
    if (onCancel) {
      onCancel()
    }
  }

  return (
    <Box sx={{ p: 2 }}>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* File Upload Area */}
      <Paper
        {...getRootProps()}
        sx={{
          p: 4,
          border: '2px dashed',
          borderColor: isDragActive ? 'primary.main' : 'grey.300',
          bgcolor: isDragActive ? 'action.hover' : 'background.paper',
          cursor: 'pointer',
          textAlign: 'center',
          mb: 3,
        }}
      >
        <input {...getInputProps()} />
        <CloudUploadIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
        {file ? (
          <Box>
            <AudioFileIcon sx={{ fontSize: 32, mb: 1 }} />
            <Typography variant="body1" fontWeight="medium">
              {file.name}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {(file.size / 1024 / 1024).toFixed(2)} MB
            </Typography>
            <Button
              size="small"
              onClick={(e) => {
                e.stopPropagation()
                setFile(null)
                if (audioPreview) {
                  URL.revokeObjectURL(audioPreview)
                  setAudioPreview(null)
                }
              }}
              sx={{ mt: 1 }}
            >
              Remove File
            </Button>
          </Box>
        ) : (
          <Box>
            <Typography variant="body1" gutterBottom>
              {isDragActive ? 'Drop audio file here' : 'Drag & drop audio file here, or click to select'}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Supports: MP3, WAV, OGG, M4A, AAC
            </Typography>
          </Box>
        )}
      </Paper>

      {/* Audio Preview */}
      {audioPreview && file && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom>
            Audio Preview
          </Typography>
          <audio controls src={audioPreview} style={{ width: '100%' }} />
        </Box>
      )}

      {/* Upload Progress */}
      {uploading && (
        <Box sx={{ mb: 3 }}>
          <LinearProgress variant="determinate" value={uploadProgress} />
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Uploading... {uploadProgress}%
          </Typography>
        </Box>
      )}

      {/* Form Fields */}
      <TextField
        fullWidth
        label="Title *"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        required
        sx={{ mb: 2 }}
      />

      <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
        <FormControl fullWidth>
          <InputLabel>Order (Optional)</InputLabel>
          <Select
            value={orderId}
            onChange={(e) => setOrderId(e.target.value as number | '')}
            label="Order (Optional)"
          >
            <MenuItem value="">None</MenuItem>
            {orders.map((order) => (
              <MenuItem key={order.id} value={order.id}>
                {order.order_number}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <FormControl fullWidth>
          <InputLabel>Advertiser (Optional)</InputLabel>
          <Select
            value={advertiserId}
            onChange={(e) => setAdvertiserId(e.target.value as number | '')}
            label="Advertiser (Optional)"
          >
            <MenuItem value="">None</MenuItem>
            {advertisers.map((advertiser) => (
              <MenuItem key={advertiser.id} value={advertiser.id}>
                {advertiser.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      <TextField
        fullWidth
        label="Script Text (Optional)"
        value={scriptText}
        onChange={(e) => setScriptText(e.target.value)}
        multiline
        rows={4}
        sx={{ mb: 2 }}
      />

      <TextField
        fullWidth
        label="Expiration Date (Optional)"
        type="date"
        value={expirationDate}
        onChange={(e) => setExpirationDate(e.target.value)}
        InputLabelProps={{ shrink: true }}
        sx={{ mb: 3 }}
      />

      {/* Actions */}
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
        {onCancel && (
          <Button onClick={handleCancel} disabled={uploading}>
            Cancel
          </Button>
        )}
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={uploading || !title.trim()}
          startIcon={<CloudUploadIcon />}
        >
          {uploading ? 'Uploading...' : file ? 'Upload Copy' : 'Create Copy'}
        </Button>
      </Box>
    </Box>
  )
}

export default CopyUpload

