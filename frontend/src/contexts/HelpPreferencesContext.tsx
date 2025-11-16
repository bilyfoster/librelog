import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react'

type HelpHintMode = 'hover' | 'always' | 'hidden'

interface HelpPreferences {
  showHelpHints: HelpHintMode
  setShowHelpHints: (mode: HelpHintMode) => void
}

const HelpPreferencesContext = createContext<HelpPreferences | undefined>(undefined)

export const HelpPreferencesProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [showHelpHints, setShowHelpHintsState] = useState<HelpHintMode>(() => {
    const saved = localStorage.getItem('helpHintsMode')
    return (saved as HelpHintMode) || 'hover'
  })

  const setShowHelpHints = (mode: HelpHintMode) => {
    setShowHelpHintsState(mode)
    localStorage.setItem('helpHintsMode', mode)
  }

  return (
    <HelpPreferencesContext.Provider value={{ showHelpHints, setShowHelpHints }}>
      {children}
    </HelpPreferencesContext.Provider>
  )
}

export const useHelpPreferences = (): HelpPreferences => {
  const context = useContext(HelpPreferencesContext)
  if (!context) {
    throw new Error('useHelpPreferences must be used within HelpPreferencesProvider')
  }
  return context
}

