import { useQuery } from '@tanstack/react-query'
import api from '../utils/api'

export interface HelpArticle {
  id: string
  title: string
  description?: string
  content: string
  type?: 'workflow' | 'concept'
  category?: string
  role?: string
  steps?: string[]
}

export interface Term {
  id: string
  term: string
  abbreviation?: string
  novice_explanation: string
  veteran_explanation: string
  category?: string
  related_terms?: string[]
}

export interface FieldHelp {
  path: string
  label: string
  help_text: string
  article_id?: string
  term_id?: string
}

export const useHelpArticles = (category?: string, role?: string, search?: string) => {
  return useQuery({
    queryKey: ['help-articles', category, role, search],
    queryFn: async () => {
      const params = new URLSearchParams()
      if (category) params.append('category', category)
      if (role) params.append('role', role)
      if (search) params.append('search', search)
      
      const response = await api.get(`/help/articles?${params.toString()}`)
      return response.data as { articles: HelpArticle[]; total: number }
    },
  })
}

export const useHelpArticle = (articleId: string) => {
  return useQuery({
    queryKey: ['help-article', articleId],
    queryFn: async () => {
      try {
        const response = await api.get(`/help/articles/${articleId}`)
        return response.data as HelpArticle
      } catch (error: any) {
        // Handle 404 gracefully - return null instead of throwing
        if (error?.response?.status === 404) {
          return null
        }
        throw error
      }
    },
    enabled: !!articleId,
    retry: false, // Don't retry on 404s
  })
}

export const useTerminology = (search?: string) => {
  return useQuery({
    queryKey: ['terminology', search],
    queryFn: async () => {
      const params = new URLSearchParams()
      if (search) params.append('search', search)
      
      const response = await api.get(`/help/terminology?${params.toString()}`)
      return response.data as { terms: Term[]; total: number }
    },
  })
}

export const useTerm = (termId: string) => {
  return useQuery({
    queryKey: ['term', termId],
    queryFn: async () => {
      try {
        const response = await api.get(`/help/terminology/${termId}`)
        return response.data as Term
      } catch (error: any) {
        // Handle 404 gracefully - return null instead of throwing
        if (error?.response?.status === 404) {
          return null
        }
        throw error
      }
    },
    enabled: !!termId,
    retry: false, // Don't retry on 404s
  })
}

export const useFieldHelp = (fieldPath: string) => {
  return useQuery({
    queryKey: ['field-help', fieldPath],
    queryFn: async () => {
      const response = await api.get(`/help/field-help/${fieldPath}`)
      return response.data as { help: FieldHelp | null }
    },
    enabled: !!fieldPath,
  })
}

export const useHelpSearch = (query: string) => {
  return useQuery({
    queryKey: ['help-search', query],
    queryFn: async () => {
      const response = await api.get(`/help/search?q=${encodeURIComponent(query)}`)
      return response.data
    },
    enabled: !!query && query.length > 0,
  })
}

