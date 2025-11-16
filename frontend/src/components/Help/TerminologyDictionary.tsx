import React, { useState } from 'react'
import {
  Box,
  Typography,
  TextField,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  InputAdornment,
  Chip,
  Tabs,
  Tab,
} from '@mui/material'
import { Search as SearchIcon } from '@mui/icons-material'
import { useTerminology } from '../../hooks/useHelp'

interface TerminologyDictionaryProps {
  searchQuery?: string
  onTermClick?: (termId: string) => void
}

const TerminologyDictionary: React.FC<TerminologyDictionaryProps> = ({
  searchQuery: externalSearchQuery,
  onTermClick,
}) => {
  const [internalSearchQuery, setInternalSearchQuery] = useState('')
  const searchQuery = externalSearchQuery || internalSearchQuery
  const { data: termsData, isLoading } = useTerminology(searchQuery)

  const terms = termsData?.terms || []

  const groupedTerms = terms.reduce((acc, term) => {
    const category = term.category || 'other'
    if (!acc[category]) {
      acc[category] = []
    }
    acc[category].push(term)
    return acc
  }, {} as Record<string, typeof terms>)

  const categories = Object.keys(groupedTerms)
  const [selectedCategory, setSelectedCategory] = useState<string>('all')

  const filteredTerms =
    selectedCategory === 'all'
      ? terms
      : groupedTerms[selectedCategory] || []

  if (!externalSearchQuery) {
    return (
      <Box>
        <TextField
          fullWidth
          placeholder="Search terminology..."
          value={internalSearchQuery}
          onChange={(e) => setInternalSearchQuery(e.target.value)}
          sx={{ mb: 2 }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
        />

        {categories.length > 0 && (
          <Box sx={{ mb: 2 }}>
            <Tabs
              value={selectedCategory}
              onChange={(_, newValue) => setSelectedCategory(newValue)}
              variant="scrollable"
              scrollButtons="auto"
            >
              <Tab label="All" value="all" />
              {categories.map((cat) => (
                <Tab key={cat} label={cat} value={cat} />
              ))}
            </Tabs>
          </Box>
        )}

        {isLoading ? (
          <Typography variant="body2" color="text.secondary">
            Loading terminology...
          </Typography>
        ) : filteredTerms.length > 0 ? (
          <List>
            {filteredTerms.map((term) => (
              <ListItem key={term.id} disablePadding>
                <ListItemButton
                  onClick={() => onTermClick?.(term.id)}
                  sx={{ flexDirection: 'column', alignItems: 'flex-start', py: 2 }}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', width: '100%', mb: 1 }}>
                    <Typography variant="subtitle1">{term.term}</Typography>
                    {term.abbreviation && (
                      <Chip
                        label={term.abbreviation}
                        size="small"
                        sx={{ ml: 1 }}
                      />
                    )}
                    {term.category && (
                      <Chip
                        label={term.category}
                        size="small"
                        color="primary"
                        sx={{ ml: 1 }}
                      />
                    )}
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    {term.novice_explanation.substring(0, 150)}
                    {term.novice_explanation.length > 150 ? '...' : ''}
                  </Typography>
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        ) : (
          <Typography variant="body2" color="text.secondary">
            No terms found. {searchQuery && 'Try a different search term.'}
          </Typography>
        )}
      </Box>
    )
  }

  // When used with external search query (from HelpCenter)
  return (
    <Box>
      {isLoading ? (
        <Typography variant="body2" color="text.secondary">
          Loading terminology...
        </Typography>
      ) : terms.length > 0 ? (
        <List>
          {terms.map((term) => (
            <ListItem key={term.id} disablePadding>
              <ListItemButton
                onClick={() => onTermClick?.(term.id)}
                sx={{ flexDirection: 'column', alignItems: 'flex-start', py: 2 }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', width: '100%', mb: 1 }}>
                  <Typography variant="subtitle1">{term.term}</Typography>
                  {term.abbreviation && (
                    <Chip label={term.abbreviation} size="small" sx={{ ml: 1 }} />
                  )}
                </Box>
                <Typography variant="body2" color="text.secondary">
                  {term.novice_explanation.substring(0, 150)}
                  {term.novice_explanation.length > 150 ? '...' : ''}
                </Typography>
              </ListItemButton>
            </ListItem>
          ))}
        </List>
      ) : (
        <Typography variant="body2" color="text.secondary">
          No terms found.
        </Typography>
      )}
    </Box>
  )
}

export default TerminologyDictionary

