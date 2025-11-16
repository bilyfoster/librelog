import React, { useState } from 'react'
import {
  IconButton,
  Tooltip,
  Popover,
  Typography,
  Box,
  Link,
  Paper,
} from '@mui/material'
import { Info as InfoIcon } from '@mui/icons-material'
import { useHelpPreferences } from '../../contexts/HelpPreferencesContext'
import { useFieldHelp } from '../../hooks/useHelp'
import HelpModal from './HelpModal'

interface InfoButtonProps {
  fieldPath: string
  label?: string
  size?: 'small' | 'medium'
  placement?: 'top' | 'bottom' | 'left' | 'right'
}

const InfoButton: React.FC<InfoButtonProps> = ({
  fieldPath,
  label,
  size = 'small',
  placement = 'top',
}) => {
  const { showHelpHints } = useHelpPreferences()
  const { data: fieldHelpData, isLoading } = useFieldHelp(fieldPath)
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null)
  const [modalOpen, setModalOpen] = useState(false)

  const fieldHelp = fieldHelpData?.help

  // Don't show if hidden or no help available
  if (showHelpHints === 'hidden' || (!fieldHelp && !isLoading)) {
    return null
  }

  const handleClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget)
  }

  const handleClose = () => {
    setAnchorEl(null)
  }

  const handleLearnMore = () => {
    setAnchorEl(null)
    setModalOpen(true)
  }

  const open = Boolean(anchorEl)
  const id = open ? `help-popover-${fieldPath}` : undefined

  // Show button based on preference - always show the button, but control visibility
  const alwaysVisible = showHelpHints === 'always'

  return (
    <>
      <Box
        component="span"
        sx={{
          display: 'inline-flex',
          alignItems: 'center',
          ml: 0.5,
          opacity: alwaysVisible ? 1 : 0,
          transition: 'opacity 0.2s',
          '&:hover': {
            opacity: 1,
          },
        }}
        onMouseEnter={(e) => {
          if (showHelpHints === 'hover') {
            // Show button on hover
          }
        }}
      >
        <Tooltip title={label || 'Help'}>
          <IconButton
            size={size}
            onClick={handleClick}
            sx={{
              padding: '2px',
              color: 'text.secondary',
              '&:hover': {
                color: 'primary.main',
                backgroundColor: 'action.hover',
              },
            }}
            aria-describedby={id}
            aria-label={`Help for ${label || fieldPath}`}
          >
            <InfoIcon fontSize="inherit" />
          </IconButton>
        </Tooltip>
      </Box>

      <Popover
        id={id}
        open={open}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{
          vertical: placement === 'top' ? 'top' : placement === 'bottom' ? 'bottom' : 'center',
          horizontal: placement === 'left' ? 'right' : placement === 'right' ? 'left' : 'center',
        }}
        transformOrigin={{
          vertical: placement === 'top' ? 'bottom' : placement === 'bottom' ? 'top' : 'center',
          horizontal: 'center',
        }}
        sx={{
          pointerEvents: 'none',
        }}
        PaperProps={{
          onMouseEnter: () => {},
          onMouseLeave: handleClose,
          sx: {
            pointerEvents: 'auto',
            maxWidth: 300,
            p: 2,
          },
        }}
      >
        {isLoading ? (
          <Typography variant="body2">Loading help...</Typography>
        ) : fieldHelp ? (
          <Box>
            <Typography variant="subtitle2" gutterBottom>
              {fieldHelp.label}
            </Typography>
            <Typography variant="body2" sx={{ mb: 1 }}>
              {fieldHelp.help_text}
            </Typography>
            {(fieldHelp.article_id || fieldHelp.term_id) && (
              <Link
                component="button"
                variant="body2"
                onClick={handleLearnMore}
                sx={{ cursor: 'pointer' }}
              >
                Learn more
              </Link>
            )}
          </Box>
        ) : (
          <Typography variant="body2">No help available for this field.</Typography>
        )}
      </Popover>

      {fieldHelp && (
        <HelpModal
          open={modalOpen}
          onClose={() => setModalOpen(false)}
          articleId={fieldHelp.article_id}
          termId={fieldHelp.term_id}
        />
      )}
    </>
  )
}

export default InfoButton

