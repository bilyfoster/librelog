import { useState, useRef, useCallback } from 'react'

interface UseVoiceRecorderOptions {
  context?: 'voice_track' | 'production'
  breakId?: number
  requestId?: number
  onRecordingComplete?: (blob: Blob) => void
}

export const useVoiceRecorder = (options: UseVoiceRecorderOptions = {}) => {
  const [isRecording, setIsRecording] = useState(false)
  const [isPaused, setIsPaused] = useState(false)
  const [recordingTime, setRecordingTime] = useState(0)
  const [audioBlob, setAudioBlob] = useState<Blob | null>(null)
  const [takes, setTakes] = useState<any[]>([])
  const [selectedTake, setSelectedTake] = useState<number | null>(null)
  
  const mediaRecorderRef = useRef<MediaRecorder | null>(null)
  const chunksRef = useRef<Blob[]>([])
  const timerRef = useRef<NodeJS.Timeout | null>(null)
  const streamRef = useRef<MediaStream | null>(null)

  const startRecording = useCallback(async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      streamRef.current = stream
      
      const mediaRecorder = new MediaRecorder(stream, {
        mimeType: 'audio/webm;codecs=opus'
      })
      mediaRecorderRef.current = mediaRecorder
      chunksRef.current = []
      
      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          chunksRef.current.push(event.data)
        }
      }
      
      mediaRecorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: 'audio/webm' })
        setAudioBlob(blob)
        if (options.onRecordingComplete) {
          options.onRecordingComplete(blob)
        }
        
        // Stop all tracks
        if (streamRef.current) {
          streamRef.current.getTracks().forEach(track => track.stop())
          streamRef.current = null
        }
      }
      
      mediaRecorder.start()
      setIsRecording(true)
      setRecordingTime(0)
      
      // Start timer
      timerRef.current = setInterval(() => {
        setRecordingTime(prev => prev + 1)
      }, 1000)
      
    } catch (error) {
      console.error('Error starting recording:', error)
      throw error
    }
  }, [options])

  const stopRecording = useCallback(() => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop()
      setIsRecording(false)
      setIsPaused(false)
      
      if (timerRef.current) {
        clearInterval(timerRef.current)
        timerRef.current = null
      }
    }
  }, [isRecording])

  const pauseRecording = useCallback(() => {
    if (mediaRecorderRef.current && isRecording && !isPaused) {
      mediaRecorderRef.current.pause()
      setIsPaused(true)
      
      if (timerRef.current) {
        clearInterval(timerRef.current)
        timerRef.current = null
      }
    }
  }, [isRecording, isPaused])

  const resumeRecording = useCallback(() => {
    if (mediaRecorderRef.current && isRecording && isPaused) {
      mediaRecorderRef.current.resume()
      setIsPaused(false)
      
      timerRef.current = setInterval(() => {
        setRecordingTime(prev => prev + 1)
      }, 1000)
    }
  }, [isRecording, isPaused])

  const addTake = useCallback((take: any) => {
    setTakes(prev => [...prev, take])
  }, [])

  const selectTake = useCallback((takeId: number) => {
    setSelectedTake(takeId)
  }, [])

  const reset = useCallback(() => {
    stopRecording()
    setAudioBlob(null)
    setRecordingTime(0)
    chunksRef.current = []
  }, [stopRecording])

  return {
    isRecording,
    isPaused,
    recordingTime,
    audioBlob,
    takes,
    selectedTake,
    startRecording,
    stopRecording,
    pauseRecording,
    resumeRecording,
    addTake,
    selectTake,
    reset
  }
}

