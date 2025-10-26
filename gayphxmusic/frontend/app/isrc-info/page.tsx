'use client'

import { useState } from 'react'
import { Music, Download, FileText, AlertCircle, CheckCircle, ExternalLink } from 'lucide-react'
import Link from 'next/link'

export default function ISRCInfoPage() {
  const [selectedFormat, setSelectedFormat] = useState('mp3')

  return (
    <div className="min-h-screen bg-gradient-to-br from-gayphx-purple to-gayphx-pink">
      {/* Header */}
      <header className="bg-white/10 backdrop-blur-md border-b border-white/20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center space-x-2">
              <Music className="h-8 w-8 text-white" />
              <h1 className="text-2xl font-bold text-white">GayPHX Music</h1>
            </div>
            <Link href="/dashboard" className="text-white hover:text-gray-200 transition-colors">
              Back to Dashboard
            </Link>
          </div>
        </div>
      </header>

      <div className="py-8">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-8">
            <h1 className="text-4xl font-bold text-white mb-4">ISRC Code Information</h1>
            <p className="text-white/90 text-lg">
              Everything you need to know about ISRC codes and how to use them
            </p>
          </div>

          <div className="space-y-8">
            {/* What is ISRC */}
            <div className="card">
              <h2 className="text-2xl font-semibold mb-4 flex items-center">
                <FileText className="h-6 w-6 mr-2" />
                What is an ISRC Code?
              </h2>
              <div className="prose max-w-none">
                <p className="text-gray-700 mb-4">
                  An ISRC (International Standard Recording Code) is a unique identifier for sound recordings and music videos. 
                  It's like a fingerprint for your music that helps track plays, royalties, and distribution across all platforms.
                </p>
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
                  <h3 className="font-semibold text-blue-900 mb-2">ISRC Format</h3>
                  <p className="text-blue-800 font-mono text-lg">US-GPH-25-00001</p>
                  <div className="text-sm text-blue-700 mt-2">
                    <div>• <strong>US</strong> - Country Code</div>
                    <div>• <strong>GPH</strong> - Registrant Code (GayPHX)</div>
                    <div>• <strong>25</strong> - Year (2025)</div>
                    <div>• <strong>00001</strong> - Unique Sequence Number</div>
                  </div>
                </div>
              </div>
            </div>

            {/* How to Use ISRC */}
            <div className="card">
              <h2 className="text-2xl font-semibold mb-4 flex items-center">
                <CheckCircle className="h-6 w-6 mr-2" />
                How to Use Your ISRC Code
              </h2>
              <div className="space-y-4">
                <div className="border-l-4 border-green-500 pl-4">
                  <h3 className="font-semibold text-gray-900">1. Embed in Your Audio Files</h3>
                  <p className="text-gray-700">
                    Add the ISRC code to your MP3, WAV, or other audio file metadata using audio editing software.
                  </p>
                </div>
                
                <div className="border-l-4 border-blue-500 pl-4">
                  <h3 className="font-semibold text-gray-900">2. Provide to Distribution Services</h3>
                  <p className="text-gray-700">
                    When uploading to Spotify, Apple Music, or other platforms, include the ISRC code in the metadata.
                  </p>
                </div>
                
                <div className="border-l-4 border-purple-500 pl-4">
                  <h3 className="font-semibold text-gray-900">3. Register with SoundExchange</h3>
                  <p className="text-gray-700">
                    Register your recordings with <a href="https://soundexchange.com" target="_blank" rel="noopener noreferrer" className="text-gayphx-purple hover:underline">SoundExchange</a> to collect digital performance royalties.
                  </p>
                </div>
              </div>
            </div>

            {/* Software Instructions */}
            <div className="card">
              <h2 className="text-2xl font-semibold mb-4 flex items-center">
                <Download className="h-6 w-6 mr-2" />
                How to Embed ISRC in Your Audio Files
              </h2>
              
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Select Your Audio Format
                </label>
                <select 
                  value={selectedFormat} 
                  onChange={(e) => setSelectedFormat(e.target.value)}
                  className="input-field max-w-xs"
                >
                  <option value="mp3">MP3</option>
                  <option value="wav">WAV</option>
                  <option value="flac">FLAC</option>
                  <option value="m4a">M4A</option>
                </select>
              </div>

              {selectedFormat === 'mp3' && (
                <div className="space-y-4">
                  <h3 className="text-lg font-semibold">MP3 Files (ID3 Tags)</h3>
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <h4 className="font-semibold mb-2">Using Audacity (Free)</h4>
                    <ol className="list-decimal list-inside space-y-1 text-sm">
                      <li>Open your audio file in Audacity</li>
                      <li>Go to <strong>File → Export → Export Audio</strong></li>
                      <li>Choose MP3 format and click <strong>Options</strong></li>
                      <li>In the ID3v2 Tags section, add your ISRC code</li>
                      <li>Save the file</li>
                    </ol>
                  </div>
                  
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <h4 className="font-semibold mb-2">Using Adobe Audition</h4>
                    <ol className="list-decimal list-inside space-y-1 text-sm">
                      <li>Open your audio file in Audition</li>
                      <li>Go to <strong>File → Export → Export Audio</strong></li>
                      <li>In the Format Options, click <strong>Metadata</strong></li>
                      <li>Add your ISRC code in the ISRC field</li>
                      <li>Export the file</li>
                    </ol>
                  </div>
                </div>
              )}

              {selectedFormat === 'wav' && (
                <div className="space-y-4">
                  <h3 className="text-lg font-semibold">WAV Files (BWF Metadata)</h3>
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <h4 className="font-semibold mb-2">Using Reaper (Free Trial)</h4>
                    <ol className="list-decimal list-inside space-y-1 text-sm">
                      <li>Open your project in Reaper</li>
                      <li>Right-click on the audio item and select <strong>Item Properties</strong></li>
                      <li>Go to the <strong>Media</strong> tab</li>
                      <li>In the <strong>Source Properties</strong>, add your ISRC code</li>
                      <li>Render/Export your project</li>
                    </ol>
                  </div>
                </div>
              )}

              {selectedFormat === 'flac' && (
                <div className="space-y-4">
                  <h3 className="text-lg font-semibold">FLAC Files (Vorbis Comments)</h3>
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <h4 className="font-semibold mb-2">Using FLAC Tools (Command Line)</h4>
                    <div className="bg-black text-green-400 p-3 rounded font-mono text-sm">
                      <div># Install flac tools</div>
                      <div># Add ISRC to existing FLAC file</div>
                      <div>metaflac --set-tag="ISRC=US-GPH-25-00001" your-file.flac</div>
                    </div>
                  </div>
                </div>
              )}

              {selectedFormat === 'm4a' && (
                <div className="space-y-4">
                  <h3 className="text-lg font-semibold">M4A Files (iTunes Metadata)</h3>
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <h4 className="font-semibold mb-2">Using iTunes/Music App</h4>
                    <ol className="list-decimal list-inside space-y-1 text-sm">
                      <li>Import your audio file into iTunes/Music</li>
                      <li>Right-click and select <strong>Get Info</strong></li>
                      <li>Go to the <strong>Details</strong> tab</li>
                      <li>Add your ISRC code in the <strong>ISRC</strong> field</li>
                      <li>Click <strong>OK</strong> to save</li>
                    </ol>
                  </div>
                </div>
              )}
            </div>

            {/* Important Notes */}
            <div className="card border-amber-200 bg-amber-50">
              <h2 className="text-2xl font-semibold mb-4 flex items-center text-amber-800">
                <AlertCircle className="h-6 w-6 mr-2" />
                Important Notes
              </h2>
              <div className="space-y-3 text-amber-800">
                <div className="flex items-start">
                  <div className="w-2 h-2 bg-amber-600 rounded-full mt-2 mr-3 flex-shrink-0"></div>
                  <p><strong>One ISRC per recording:</strong> Each unique recording gets its own ISRC code. Remixes, covers, and different versions need separate codes.</p>
                </div>
                <div className="flex items-start">
                  <div className="w-2 h-2 bg-amber-600 rounded-full mt-2 mr-3 flex-shrink-0"></div>
                  <p><strong>Permanent identifier:</strong> Once assigned, the ISRC code stays with the recording forever, even if ownership changes.</p>
                </div>
                <div className="flex items-start">
                  <div className="w-2 h-2 bg-amber-600 rounded-full mt-2 mr-3 flex-shrink-0"></div>
                  <p><strong>Required for distribution:</strong> Most major platforms (Spotify, Apple Music, etc.) require ISRC codes for proper royalty tracking.</p>
                </div>
                <div className="flex items-start">
                  <div className="w-2 h-2 bg-amber-600 rounded-full mt-2 mr-3 flex-shrink-0"></div>
                  <p><strong>Register with SoundExchange:</strong> This is crucial for collecting digital performance royalties from streaming and satellite radio.</p>
                </div>
              </div>
            </div>

            {/* Resources */}
            <div className="card">
              <h2 className="text-2xl font-semibold mb-4">Additional Resources</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <a 
                  href="https://usisrc.org" 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="flex items-center p-4 border rounded-lg hover:bg-gray-50 transition-colors"
                >
                  <ExternalLink className="h-5 w-5 mr-3 text-gayphx-purple" />
                  <div>
                    <div className="font-semibold">US ISRC Agency</div>
                    <div className="text-sm text-gray-600">Official ISRC information and guidelines</div>
                  </div>
                </a>
                
                <a 
                  href="https://soundexchange.com" 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="flex items-center p-4 border rounded-lg hover:bg-gray-50 transition-colors"
                >
                  <ExternalLink className="h-5 w-5 mr-3 text-gayphx-purple" />
                  <div>
                    <div className="font-semibold">SoundExchange</div>
                    <div className="text-sm text-gray-600">Register for digital performance royalties</div>
                  </div>
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

