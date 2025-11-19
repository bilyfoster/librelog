import React, { useState } from 'react'
import {
  Box,
  Typography,
  TextField,
  Tabs,
  Tab,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Paper,
  InputAdornment,
  Chip,
  Divider,
} from '@mui/material'
import {
  Search as SearchIcon,
  Work as WorkflowIcon,
  Lightbulb as ConceptIcon,
  Book as BookIcon,
} from '@mui/icons-material'
import { useHelpArticles, useTerminology, useHelpSearch } from '../../hooks/useHelp'
import HelpModal from './HelpModal'
import TerminologyDictionary from './TerminologyDictionary'

interface TabPanelProps {
  children?: React.ReactNode
  index: number
  value: number
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`help-tabpanel-${index}`}
      aria-labelledby={`help-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  )
}

const HelpCenter: React.FC = () => {
  const [tabValue, setTabValue] = useState(0)
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedArticle, setSelectedArticle] = useState<string | null>(null)
  const [selectedTerm, setSelectedTerm] = useState<string | null>(null)
  const [modalOpen, setModalOpen] = useState(false)

  const { data: workflowsData } = useHelpArticles(undefined, undefined, tabValue === 0 ? searchQuery : undefined)
  const { data: conceptsData } = useHelpArticles(undefined, undefined, tabValue === 1 ? searchQuery : undefined)
  const { data: documentationData } = useHelpArticles(undefined, undefined, tabValue === 3 ? searchQuery : undefined)
  const { data: searchResults } = useHelpSearch(searchQuery)

  const workflows = workflowsData?.articles.filter(a => a.type === 'workflow') || []
  const concepts = conceptsData?.articles.filter(a => a.type === 'concept') || []
  const documentation = documentationData?.articles.filter(a => a.type === 'documentation') || []

  const handleTabChange = (_: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue)
    setSearchQuery('')
  }

  const handleArticleClick = (articleId: string) => {
    setSelectedArticle(articleId)
    setSelectedTerm(null)
    setModalOpen(true)
  }

  const handleTermClick = (termId: string) => {
    setSelectedTerm(termId)
    setSelectedArticle(null)
    setModalOpen(true)
  }

  const showSearchResults = searchQuery.length > 0 && tabValue === 4

  return (
    <Box sx={{ width: '100%', maxWidth: 1200, mx: 'auto', p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Help Center
      </Typography>
      <Typography variant="body2" color="text.secondary" paragraph>
        Find answers to your questions and learn how to use the system
      </Typography>

      <TextField
        fullWidth
        placeholder="Search help articles, concepts, and terminology..."
        value={searchQuery}
        onChange={(e) => setSearchQuery(e.target.value)}
        sx={{ mb: 3 }}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <SearchIcon />
            </InputAdornment>
          ),
        }}
      />

      <Paper sx={{ mb: 3 }}>
        <Tabs value={tabValue} onChange={handleTabChange}>
          <Tab icon={<WorkflowIcon />} label="Workflows" iconPosition="start" />
          <Tab icon={<ConceptIcon />} label="Concepts" iconPosition="start" />
          <Tab icon={<BookIcon />} label="Terminology" iconPosition="start" />
          <Tab icon={<BookIcon />} label="Documentation" iconPosition="start" />
          <Tab icon={<SearchIcon />} label="Search" iconPosition="start" />
        </Tabs>

        <TabPanel value={tabValue} index={0}>
          <Typography variant="h6" gutterBottom>
            Workflows by Role
          </Typography>
          <List>
            {workflows.map((workflow) => (
              <ListItem key={workflow.id} disablePadding>
                <ListItemButton onClick={() => handleArticleClick(workflow.id)}>
                  <ListItemText
                    primary={workflow.title}
                    secondary={workflow.description}
                  />
                  {workflow.role && (
                    <Chip label={workflow.role} size="small" sx={{ ml: 2 }} />
                  )}
                </ListItemButton>
              </ListItem>
            ))}
            {workflows.length === 0 && (
              <Typography variant="body2" color="text.secondary">
                No workflows found. {searchQuery && 'Try a different search term.'}
              </Typography>
            )}
          </List>
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          <Typography variant="h6" gutterBottom>
            Key Concepts
          </Typography>
          <List>
            {concepts.map((concept) => (
              <ListItem key={concept.id} disablePadding>
                <ListItemButton onClick={() => handleArticleClick(concept.id)}>
                  <ListItemText
                    primary={concept.title}
                    secondary={concept.description}
                  />
                  {concept.category && (
                    <Chip label={concept.category} size="small" sx={{ ml: 2 }} />
                  )}
                </ListItemButton>
              </ListItem>
            ))}
            {concepts.length === 0 && (
              <Typography variant="body2" color="text.secondary">
                No concepts found. {searchQuery && 'Try a different search term.'}
              </Typography>
            )}
          </List>
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          <TerminologyDictionary
            searchQuery={searchQuery}
            onTermClick={handleTermClick}
          />
        </TabPanel>

        <TabPanel value={tabValue} index={3}>
          <Typography variant="h6" gutterBottom>
            Documentation
          </Typography>
          <Typography variant="body2" color="text.secondary" paragraph>
            Comprehensive guides and reference documentation for LibreLog.
          </Typography>
          <List>
            {documentation.map((doc) => (
              <ListItem key={doc.id} disablePadding>
                <ListItemButton onClick={() => handleArticleClick(doc.id)}>
                  <ListItemText
                    primary={doc.title}
                    secondary={doc.description}
                  />
                  {doc.category && (
                    <Chip label={doc.category} size="small" sx={{ ml: 2 }} />
                  )}
                </ListItemButton>
              </ListItem>
            ))}
            {documentation.length === 0 && (
              <Typography variant="body2" color="text.secondary">
                No documentation found. {searchQuery && 'Try a different search term.'}
              </Typography>
            )}
          </List>
        </TabPanel>

        <TabPanel value={tabValue} index={4}>
          {showSearchResults && searchResults ? (
            <Box>
              {searchResults.results.articles.length > 0 && (
                <Box sx={{ mb: 3 }}>
                  <Typography variant="h6" gutterBottom>
                    Articles ({searchResults.results.articles.length})
                  </Typography>
                  <List>
                    {searchResults.results.articles.map((article: any) => (
                      <ListItem key={article.id} disablePadding>
                        <ListItemButton onClick={() => handleArticleClick(article.id)}>
                          <ListItemText
                            primary={article.title}
                            secondary={article.description}
                          />
                          <Chip
                            label={article.type}
                            size="small"
                            sx={{ ml: 2 }}
                          />
                        </ListItemButton>
                      </ListItem>
                    ))}
                  </List>
                </Box>
              )}

              {searchResults.results.terms.length > 0 && (
                <Box sx={{ mb: 3 }}>
                  <Divider sx={{ my: 2 }} />
                  <Typography variant="h6" gutterBottom>
                    Terms ({searchResults.results.terms.length})
                  </Typography>
                  <List>
                    {searchResults.results.terms.map((term: any) => (
                      <ListItem key={term.id} disablePadding>
                        <ListItemButton onClick={() => handleTermClick(term.id)}>
                          <ListItemText
                            primary={term.term}
                            secondary={term.abbreviation || term.novice_explanation.substring(0, 100) + '...'}
                          />
                        </ListItemButton>
                      </ListItem>
                    ))}
                  </List>
                </Box>
              )}

              {searchResults.total === 0 && (
                <Typography variant="body2" color="text.secondary">
                  No results found for "{searchQuery}". Try different keywords.
                </Typography>
              )}
            </Box>
          ) : (
            <Typography variant="body2" color="text.secondary">
              Enter a search query to find help articles and terminology.
            </Typography>
          )}
        </TabPanel>
      </Paper>

      <HelpModal
        open={modalOpen}
        onClose={() => {
          setModalOpen(false)
          setSelectedArticle(null)
          setSelectedTerm(null)
        }}
        articleId={selectedArticle || undefined}
        termId={selectedTerm || undefined}
      />
    </Box>
  )
}

export default HelpCenter

