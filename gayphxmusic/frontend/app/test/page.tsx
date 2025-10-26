'use client'

import { useState } from 'react'

export default function TestPage() {
  const [result, setResult] = useState('')
  const [loading, setLoading] = useState(false)

  const testAdminLogin = async () => {
    setLoading(true)
    try {
      const response = await fetch('/api/admin/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email: 'admin@gayphx.com',
          password: 'admin123'
        })
      })
      
      const data = await response.text()
      setResult(`Status: ${response.status}\nResponse: ${data}`)
    } catch (error) {
      setResult(`Error: ${error}`)
    } finally {
      setLoading(false)
    }
  }

  const testArtistSignup = async () => {
    setLoading(true)
    try {
      const response = await fetch('http://backend:8000/api/auth/signup', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: 'Test Artist',
          email: 'test@example.com'
        })
      })
      
      const data = await response.text()
      setResult(`Status: ${response.status}\nResponse: ${data}`)
    } catch (error) {
      setResult(`Error: ${error}`)
    } finally {
      setLoading(false)
    }
  }

  const testBackendDirect = async () => {
    setLoading(true)
    try {
      const response = await fetch('http://localhost:8000/api/admin/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email: 'admin@gayphx.com',
          password: 'admin123'
        })
      })
      
      const data = await response.text()
      setResult(`Status: ${response.status}\nResponse: ${data}`)
    } catch (error) {
      setResult(`Error: ${error}`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-100 p-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold mb-8">API Test Page</h1>
        
        <div className="space-y-4 mb-8">
          <button
            onClick={testAdminLogin}
            disabled={loading}
            className="bg-blue-500 text-white px-4 py-2 rounded mr-4"
          >
            Test Admin Login (Proxy)
          </button>
          
          <button
            onClick={testArtistSignup}
            disabled={loading}
            className="bg-green-500 text-white px-4 py-2 rounded mr-4"
          >
            Test Artist Signup (Proxy)
          </button>
          
          <button
            onClick={testBackendDirect}
            disabled={loading}
            className="bg-red-500 text-white px-4 py-2 rounded"
          >
            Test Backend Direct
          </button>
        </div>

        {loading && <div className="text-blue-600">Loading...</div>}
        
        <div className="bg-white p-4 rounded border">
          <h3 className="font-bold mb-2">Result:</h3>
          <pre className="whitespace-pre-wrap text-sm">{result}</pre>
        </div>
      </div>
    </div>
  )
}