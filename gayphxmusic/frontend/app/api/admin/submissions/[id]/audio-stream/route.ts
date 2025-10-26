import { NextRequest, NextResponse } from 'next/server'

const API_BASE_URL = 'http://backend:8000'

export async function GET(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/submissions/${params.id}/audio-stream`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    })

    if (!response.ok) {
      const errorText = await response.text()
      return NextResponse.json(
        { error: 'Failed to stream audio', details: errorText },
        { status: response.status }
      )
    }

    // Stream the audio file directly
    const audioStream = response.body
    if (!audioStream) {
      return NextResponse.json(
        { error: 'No audio stream available' },
        { status: 500 }
      )
    }

    return new NextResponse(audioStream, {
      headers: {
        'Content-Type': response.headers.get('content-type') || 'audio/mpeg',
        'Content-Disposition': response.headers.get('content-disposition') || 'inline',
        'Accept-Ranges': response.headers.get('accept-ranges') || 'bytes',
        'Cache-Control': 'public, max-age=3600',
      },
    })
  } catch (error) {
    console.error('Audio stream API Error:', error)
    return NextResponse.json(
      { error: 'Internal server error', details: error instanceof Error ? error.message : String(error) },
      { status: 500 }
    )
  }
}

