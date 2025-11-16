import React from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Tabs,
  Tab,
  Divider,
  Paper,
} from '@mui/material'
import { useHelpArticle, useTerm } from '../../hooks/useHelp'

interface HelpModalProps {
  open: boolean
  onClose: () => void
  articleId?: string
  termId?: string
}

const HelpModal: React.FC<HelpModalProps> = ({ open, onClose, articleId, termId }) => {
  const { data: article, isLoading: articleLoading } = useHelpArticle(articleId || '')
  const { data: term, isLoading: termLoading } = useTerm(termId || '')
  const [tabValue, setTabValue] = React.useState(0)

  const isLoading = articleLoading || termLoading
  const hasTerm = !!term
  const hasArticle = !!article

  React.useEffect(() => {
    if (hasTerm) {
      setTabValue(0) // Show novice explanation first
    }
  }, [hasTerm])

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        {hasTerm ? term.term : hasArticle ? article.title : 'Help'}
      </DialogTitle>
      <DialogContent>
        {isLoading ? (
          <Typography>Loading...</Typography>
        ) : hasTerm ? (
          <Box>
            {term.abbreviation && (
              <Typography variant="body2" color="text.secondary" gutterBottom>
                {term.abbreviation}
              </Typography>
            )}
            <Tabs value={tabValue} onChange={(_, newValue) => setTabValue(newValue)}>
              <Tab label="Novice Explanation" />
              <Tab label="Veteran Explanation" />
            </Tabs>
            <Box sx={{ mt: 2 }}>
              {tabValue === 0 ? (
                <Typography variant="body1">{term.novice_explanation}</Typography>
              ) : (
                <Typography variant="body1">{term.veteran_explanation}</Typography>
              )}
            </Box>
            {term.related_terms && term.related_terms.length > 0 && (
              <Box sx={{ mt: 3 }}>
                <Typography variant="subtitle2" gutterBottom>
                  Related Terms:
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {term.related_terms.join(', ')}
                </Typography>
              </Box>
            )}
          </Box>
        ) : hasArticle ? (
          <Box>
            {article.description && (
              <Typography variant="body2" color="text.secondary" paragraph>
                {article.description}
              </Typography>
            )}
            <Divider sx={{ my: 2 }} />
            <Paper
              sx={{
                p: 2,
                backgroundColor: 'background.default',
                '& h1': { fontSize: '1.5rem', fontWeight: 'bold', mb: 1, mt: 2 },
                '& h2': { fontSize: '1.25rem', fontWeight: 'bold', mb: 1, mt: 2 },
                '& h3': { fontSize: '1.1rem', fontWeight: 'bold', mb: 1, mt: 1 },
                '& p': { mb: 1.5, whiteSpace: 'pre-wrap' },
                '& ul, & ol': { pl: 3, mb: 1.5 },
                '& li': { mb: 0.5 },
                '& strong': { fontWeight: 'bold' },
                '& em': { fontStyle: 'italic' },
              }}
            >
              <Typography
                variant="body1"
                component="div"
                sx={{ whiteSpace: 'pre-wrap' }}
              >
                {article.content.split('\n').map((line, idx) => {
                  // Handle headers
                  if (line.startsWith('### ')) {
                    return <Typography key={idx} variant="h3" component="h3" sx={{ mt: 2, mb: 1 }}>{line.substring(4)}</Typography>
                  }
                  if (line.startsWith('## ')) {
                    return <Typography key={idx} variant="h2" component="h2" sx={{ mt: 2, mb: 1 }}>{line.substring(3)}</Typography>
                  }
                  if (line.startsWith('# ')) {
                    return <Typography key={idx} variant="h1" component="h1" sx={{ mt: 2, mb: 1 }}>{line.substring(2)}</Typography>
                  }
                  // Handle list items
                  if (line.match(/^(\d+)\. /)) {
                    const match = line.match(/^(\d+)\. (.+)$/)
                    return match ? <Typography key={idx} component="li" sx={{ ml: 3, mb: 0.5 }}>{match[2]}</Typography> : <Typography key={idx} component="p">{line}</Typography>
                  }
                  if (line.startsWith('- ')) {
                    return <Typography key={idx} component="li" sx={{ ml: 3, mb: 0.5 }}>{line.substring(2)}</Typography>
                  }
                  // Handle bold and italic
                  let processedLine = line
                    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
                    .replace(/\*(.+?)\*/g, '<em>$1</em>')
                  
                  // Regular paragraph
                  if (line.trim()) {
                    return <Typography key={idx} component="p" dangerouslySetInnerHTML={{ __html: processedLine }} />
                  }
                  return <br key={idx} />
                })}
              </Typography>
            </Paper>
            {article.steps && article.steps.length > 0 && (
              <Box sx={{ mt: 3 }}>
                <Typography variant="subtitle2" gutterBottom>
                  Quick Steps:
                </Typography>
                <Box component="ol" sx={{ pl: 2 }}>
                  {article.steps.map((step, index) => (
                    <Typography key={index} component="li" variant="body2" sx={{ mb: 1 }}>
                      {step}
                    </Typography>
                  ))}
                </Box>
              </Box>
            )}
          </Box>
        ) : (
          <Typography>No help content available.</Typography>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  )
}

export default HelpModal

