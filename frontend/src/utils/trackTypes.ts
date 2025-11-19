/**
 * Track type definitions with distinct colors for visual identification
 */

export interface TrackType {
  value: string
  label: string
  color: string // Hex color code
  backgroundColor: string // Light background color for rows/cards
  textColor: string // Text color for contrast
}

export const TRACK_TYPES: TrackType[] = [
  {
    value: 'MUS',
    label: 'Music',
    color: '#1976d2', // Blue
    backgroundColor: '#e3f2fd',
    textColor: '#0d47a1',
  },
  {
    value: 'ADV',
    label: 'Advertisement',
    color: '#d32f2f', // Red
    backgroundColor: '#ffebee',
    textColor: '#b71c1c',
  },
  {
    value: 'PSA',
    label: 'Public Service Announcement',
    color: '#388e3c', // Green
    backgroundColor: '#e8f5e9',
    textColor: '#1b5e20',
  },
  {
    value: 'LIN',
    label: 'Liner',
    color: '#f57c00', // Orange
    backgroundColor: '#fff3e0',
    textColor: '#e65100',
  },
  {
    value: 'INT',
    label: 'Interview',
    color: '#7b1fa2', // Purple
    backgroundColor: '#f3e5f5',
    textColor: '#4a148c',
  },
  {
    value: 'PRO',
    label: 'Promo',
    color: '#c2185b', // Pink
    backgroundColor: '#fce4ec',
    textColor: '#880e4f',
  },
  {
    value: 'SHO',
    label: 'Show',
    color: '#00796b', // Teal
    backgroundColor: '#e0f2f1',
    textColor: '#004d40',
  },
  {
    value: 'IDS',
    label: 'ID/Station ID',
    color: '#5d4037', // Brown
    backgroundColor: '#efebe9',
    textColor: '#3e2723',
  },
  {
    value: 'COM',
    label: 'Commercial',
    color: '#0288d1', // Light Blue
    backgroundColor: '#e1f5fe',
    textColor: '#01579b',
  },
  {
    value: 'NEW',
    label: 'News',
    color: '#fbc02d', // Yellow/Amber
    backgroundColor: '#fffde7',
    textColor: '#f57f17',
  },
  {
    value: 'VOT',
    label: 'Voice Over Track',
    color: '#616161', // Grey
    backgroundColor: '#f5f5f5',
    textColor: '#212121',
  },
]

/**
 * Get track type information by value
 */
export const getTrackType = (type: string): TrackType | undefined => {
  return TRACK_TYPES.find(t => t.value === type)
}

/**
 * Get color for a track type
 */
export const getTrackTypeColor = (type: string): string => {
  const trackType = getTrackType(type)
  return trackType?.color || '#757575' // Default grey
}

/**
 * Get background color for a track type
 */
export const getTrackTypeBackgroundColor = (type: string): string => {
  const trackType = getTrackType(type)
  return trackType?.backgroundColor || '#fafafa'
}

/**
 * Get text color for a track type
 */
export const getTrackTypeTextColor = (type: string): string => {
  const trackType = getTrackType(type)
  return trackType?.textColor || '#424242'
}

/**
 * Get MUI Chip color prop (for compatibility with existing code)
 */
export const getTrackTypeChipColor = (type: string): 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' => {
  const trackType = getTrackType(type)
  if (!trackType) return 'default'
  
  // Map to MUI colors for Chip component
  const colorMap: Record<string, 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning'> = {
    'MUS': 'primary',
    'ADV': 'error',
    'PSA': 'success',
    'LIN': 'warning',
    'INT': 'secondary',
    'PRO': 'error',
    'SHO': 'info',
    'IDS': 'default',
    'COM': 'info',
    'NEW': 'warning',
    'VOT': 'default',
  }
  
  return colorMap[type] || 'default'
}


