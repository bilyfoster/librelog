'use client'

import { useState } from 'react'
import AudioPlayer from '@/components/AudioPlayer'

export default function TestAudioPage() {
  const [testUrl, setTestUrl] = useState('')
  const [audioUrl, setAudioUrl] = useState('')

  const testAudio = () => {
    if (testUrl) {
      setAudioUrl(testUrl)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Audio Player Test</h1>
        
        <div className="card mb-6">
          <h2 className="text-xl font-semibold mb-4">Test Audio URL</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Audio URL
              </label>
              <input
                type="url"
                value={testUrl}
                onChange={(e) => setTestUrl(e.target.value)}
                placeholder="http://localhost:9002/gayphx-music/..."
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <button
              onClick={testAudio}
              className="bg-indigo-600 text-white px-4 py-2 rounded-md hover:bg-indigo-700 transition-colors"
            >
              Test Audio
            </button>
          </div>
        </div>

        {audioUrl && (
          <AudioPlayer 
            audioUrl={audioUrl} 
            fileName="Test Audio"
            className="mb-6"
          />
        )}

        <div className="card">
          <h2 className="text-xl font-semibold mb-4">Quick Test URLs</h2>
          <div className="space-y-2">
            <button
              onClick={() => setTestUrl('http://localhost:9002/gayphx-music/submissions/45ad70da-5c67-4c2c-8d9a-4250d6ac7041/together-we-thrive.mp3')}
              className="block w-full text-left px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-md transition-colors"
            >
              Test with direct MinIO URL: together-we-thrive.mp3
            </button>
            <button
              onClick={() => setTestUrl('/api/admin/submissions/bbdc8c57-080d-4bd3-8e2f-6cd958d3aa56/audio-stream')}
              className="block w-full text-left px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-md transition-colors"
            >
              Test with streaming endpoint (should work)
            </button>
            <button
              onClick={() => setTestUrl('https://www.soundjay.com/misc/sounds/bell-ringing-05.wav')}
              className="block w-full text-left px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-md transition-colors"
            >
              Test with external file (should work)
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
