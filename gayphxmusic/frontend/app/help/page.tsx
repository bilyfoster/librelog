'use client'

import { useState } from 'react'
import { Music, Users, Award, Shield, FileText, HelpCircle, ArrowRight, CheckCircle, XCircle, Info } from 'lucide-react'
import Link from 'next/link'

export default function HelpPage() {
  const [activeSection, setActiveSection] = useState('what-we-are')

  const sections = [
    { id: 'what-we-are', title: 'What We Are', icon: Music },
    { id: 'vs-label', title: 'GayPHX vs. Record Label', icon: Users },
    { id: 'who-owns', title: 'Who Owns Your Music', icon: Shield },
    { id: 'what-we-provide', title: 'What We Provide', icon: Award },
    { id: 'what-we-dont', title: "What We Don't Do", icon: XCircle },
    { id: 'faq', title: 'Frequently Asked Questions', icon: HelpCircle }
  ]

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-500 to-blue-700 relative overflow-hidden">
      {/* Wavy Lines Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `repeating-linear-gradient(45deg, white 0px, white 1px, transparent 1px, transparent 10px)`,
          backgroundSize: '20px 20px'
        }}></div>
      </div>
      {/* Header */}
      <header className="bg-white/10 backdrop-blur-md border-b border-white/20 relative z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center space-x-2">
              <Music className="h-8 w-8 text-white" />
              <h1 className="text-2xl font-bold text-white">GayPHX Music</h1>
            </div>
            <Link href="/" className="text-white hover:text-gray-200 transition-colors">
              Back to Home
            </Link>
          </div>
        </div>
      </header>

      <div className="py-8 relative z-10">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-8">
            <h1 className="text-4xl font-bold text-white mb-4">Help & Education Center</h1>
            <p className="text-white/90 text-lg">
              Learn about GayPHX Music, how we support artists, and what makes us different
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
            {/* Sidebar Navigation */}
            <div className="lg:col-span-1">
              <div className="bg-white/10 backdrop-blur-md rounded-lg p-4 sticky top-8">
                <h3 className="text-white font-semibold mb-4">Topics</h3>
                <nav className="space-y-2">
                  {sections.map((section) => {
                    const Icon = section.icon
                    return (
                      <button
                        key={section.id}
                        onClick={() => setActiveSection(section.id)}
                        className={`w-full flex items-center px-3 py-2 text-sm rounded-md transition-colors ${
                          activeSection === section.id
                            ? 'bg-white/20 text-white'
                            : 'text-white/80 hover:bg-white/10 hover:text-white'
                        }`}
                      >
                        <Icon className="h-4 w-4 mr-2" />
                        {section.title}
                      </button>
                    )
                  })}
                </nav>
              </div>
            </div>

            {/* Main Content */}
            <div className="lg:col-span-3">
              <div className="card">
                {activeSection === 'what-we-are' && (
                  <div>
                    <h2 className="text-3xl font-bold text-gray-900 mb-6 flex items-center">
                      <Music className="h-8 w-8 mr-3 text-gayphx-purple" />
                      What is GayPHX Music?
                    </h2>
                    
                    <div className="prose max-w-none">
                      <p className="text-lg text-gray-700 mb-6">
                        GayPHX Music is a <strong>music support platform</strong> specifically designed for LGBTQ+ artists. 
                        We're not a record label, but rather a specialized service that helps independent artists 
                        navigate the music industry with professional tools and resources.
                      </p>

                      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 mb-6">
                        <h3 className="text-xl font-semibold text-blue-900 mb-3">Our Mission</h3>
                        <p className="text-blue-800">
                          To empower LGBTQ+ artists by providing access to professional music industry tools, 
                          education, and community support, helping them build sustainable careers while 
                          maintaining full creative and financial control of their music.
                        </p>
                      </div>

                      <h3 className="text-2xl font-semibold text-gray-900 mb-4">What We Do</h3>
                      <ul className="space-y-3 mb-6">
                        <li className="flex items-start">
                          <CheckCircle className="h-5 w-5 text-green-500 mr-3 mt-0.5 flex-shrink-0" />
                          <span><strong>ISRC Code Assignment:</strong> Provide official ISRC codes for your recordings</span>
                        </li>
                        <li className="flex items-start">
                          <CheckCircle className="h-5 w-5 text-green-500 mr-3 mt-0.5 flex-shrink-0" />
                          <span><strong>Music Submission Platform:</strong> Streamlined system for submitting and tracking your music</span>
                        </li>
                        <li className="flex items-start">
                          <CheckCircle className="h-5 w-5 text-green-500 mr-3 mt-0.5 flex-shrink-0" />
                          <span><strong>Industry Education:</strong> Resources and guidance on music industry best practices</span>
                        </li>
                        <li className="flex items-start">
                          <CheckCircle className="h-5 w-5 text-green-500 mr-3 mt-0.5 flex-shrink-0" />
                          <span><strong>Community Support:</strong> Connect with other LGBTQ+ artists and industry professionals</span>
                        </li>
                        <li className="flex items-start">
                          <CheckCircle className="h-5 w-5 text-green-500 mr-3 mt-0.5 flex-shrink-0" />
                          <span><strong>Radio Opportunities:</strong> Potential for airplay on GayPHX Radio (with permission)</span>
                        </li>
                      </ul>

                      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                        <div className="flex items-start">
                          <Info className="h-5 w-5 text-yellow-600 mr-3 mt-0.5 flex-shrink-0" />
                          <div>
                            <p className="text-yellow-800 font-semibold">Important to Understand:</p>
                            <p className="text-yellow-700 text-sm mt-1">
                              We are a <strong>service provider</strong>, not a record label. You maintain full ownership 
                              and control of your music. We help you with tools and resources, but we don't sign artists 
                              or take ownership of your creative work.
                            </p>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {activeSection === 'vs-label' && (
                  <div>
                    <h2 className="text-3xl font-bold text-gray-900 mb-6 flex items-center">
                      <Users className="h-8 w-8 mr-3 text-gayphx-purple" />
                      GayPHX Music vs. Record Label
                    </h2>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">
                      <div className="bg-green-50 border border-green-200 rounded-lg p-6">
                        <h3 className="text-xl font-semibold text-green-900 mb-4 flex items-center">
                          <CheckCircle className="h-6 w-6 mr-2" />
                          GayPHX Music (Service Provider)
                        </h3>
                        <ul className="space-y-2 text-green-800">
                          <li>• You own 100% of your music</li>
                          <li>• You keep all royalties and revenue</li>
                          <li>• No long-term contracts</li>
                          <li>• You control distribution and marketing</li>
                          <li>• Pay-per-service model</li>
                          <li>• Focus on LGBTQ+ community support</li>
                          <li>• Educational resources and tools</li>
                          <li>• You maintain creative freedom</li>
                        </ul>
                      </div>

                      <div className="bg-red-50 border border-red-200 rounded-lg p-6">
                        <h3 className="text-xl font-semibold text-red-900 mb-4 flex items-center">
                          <XCircle className="h-6 w-6 mr-2" />
                          Traditional Record Label
                        </h3>
                        <ul className="space-y-2 text-red-800">
                          <li>• Label owns master recordings</li>
                          <li>• Revenue split (often 10-15% to artist)</li>
                          <li>• Multi-year exclusive contracts</li>
                          <li>• Label controls distribution</li>
                          <li>• Upfront investment, recoupable</li>
                          <li>• Focus on commercial success</li>
                          <li>• Limited creative control</li>
                          <li>• May require specific sound/image</li>
                        </ul>
                      </div>
                    </div>

                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
                      <h3 className="text-xl font-semibold text-blue-900 mb-3">Why Choose GayPHX Music?</h3>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-blue-800">
                        <div>
                          <h4 className="font-semibold mb-2">For Independent Artists:</h4>
                          <ul className="space-y-1 text-sm">
                            <li>• Maintain full ownership</li>
                            <li>• Keep all your earnings</li>
                            <li>• No pressure to change your sound</li>
                            <li>• LGBTQ+ community focus</li>
                          </ul>
                        </div>
                        <div>
                          <h4 className="font-semibold mb-2">Professional Benefits:</h4>
                          <ul className="space-y-1 text-sm">
                            <li>• Official ISRC codes</li>
                            <li>• Industry-standard tools</li>
                            <li>• Educational resources</li>
                            <li>• Community support</li>
                          </ul>
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {activeSection === 'who-owns' && (
                  <div>
                    <h2 className="text-3xl font-bold text-gray-900 mb-6 flex items-center">
                      <Shield className="h-8 w-8 mr-3 text-gayphx-purple" />
                      Who Owns Your Music?
                    </h2>

                    <div className="bg-green-50 border border-green-200 rounded-lg p-6 mb-6">
                      <h3 className="text-xl font-semibold text-green-900 mb-3">You Own Everything!</h3>
                      <p className="text-green-800 mb-4">
                        When you work with GayPHX Music, you maintain <strong>100% ownership</strong> of your music. 
                        This includes master recordings, publishing rights, and all associated revenue streams.
                      </p>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
                      <div>
                        <h3 className="text-lg font-semibold text-gray-900 mb-3">What You Own:</h3>
                        <ul className="space-y-2 text-gray-700">
                          <li className="flex items-start">
                            <CheckCircle className="h-4 w-4 text-green-500 mr-2 mt-1 flex-shrink-0" />
                            <span><strong>Master Recordings:</strong> The actual audio files</span>
                          </li>
                          <li className="flex items-start">
                            <CheckCircle className="h-4 w-4 text-green-500 mr-2 mt-1 flex-shrink-0" />
                            <span><strong>Publishing Rights:</strong> Songwriting and composition rights</span>
                          </li>
                          <li className="flex items-start">
                            <CheckCircle className="h-4 w-4 text-green-500 mr-2 mt-1 flex-shrink-0" />
                            <span><strong>Performance Rights:</strong> Rights to perform the music</span>
                          </li>
                          <li className="flex items-start">
                            <CheckCircle className="h-4 w-4 text-green-500 mr-2 mt-1 flex-shrink-0" />
                            <span><strong>Mechanical Rights:</strong> Rights to reproduce the music</span>
                          </li>
                          <li className="flex items-start">
                            <CheckCircle className="h-4 w-4 text-green-500 mr-2 mt-1 flex-shrink-0" />
                            <span><strong>All Royalties:</strong> Streaming, radio, sync, etc.</span>
                          </li>
                        </ul>
                      </div>

                      <div>
                        <h3 className="text-lg font-semibold text-gray-900 mb-3">What We Provide:</h3>
                        <ul className="space-y-2 text-gray-700">
                          <li className="flex items-start">
                            <Award className="h-4 w-4 text-blue-500 mr-2 mt-1 flex-shrink-0" />
                            <span><strong>ISRC Codes:</strong> Official identification codes</span>
                          </li>
                          <li className="flex items-start">
                            <Award className="h-4 w-4 text-blue-500 mr-2 mt-1 flex-shrink-0" />
                            <span><strong>Platform Access:</strong> Submission and tracking tools</span>
                          </li>
                          <li className="flex items-start">
                            <Award className="h-4 w-4 text-blue-500 mr-2 mt-1 flex-shrink-0" />
                            <span><strong>Education:</strong> Industry knowledge and resources</span>
                          </li>
                          <li className="flex items-start">
                            <Award className="h-4 w-4 text-blue-500 mr-2 mt-1 flex-shrink-0" />
                            <span><strong>Community:</strong> LGBTQ+ artist network</span>
                          </li>
                          <li className="flex items-start">
                            <Award className="h-4 w-4 text-blue-500 mr-2 mt-1 flex-shrink-0" />
                            <span><strong>Radio Opportunity:</strong> Potential airplay (with permission)</span>
                          </li>
                        </ul>
                      </div>
                    </div>

                    <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                      <h3 className="text-lg font-semibold text-yellow-900 mb-2">Important Legal Note:</h3>
                      <p className="text-yellow-800 text-sm">
                        GayPHX Music does not claim any ownership rights to your music. We are a service provider 
                        that helps you with industry tools and resources. You are responsible for ensuring you have 
                        the legal rights to submit your music and that all samples, covers, or collaborations are 
                        properly cleared.
                      </p>
                    </div>
                  </div>
                )}

                {activeSection === 'what-we-provide' && (
                  <div>
                    <h2 className="text-3xl font-bold text-gray-900 mb-6 flex items-center">
                      <Award className="h-8 w-8 mr-3 text-gayphx-purple" />
                      What We Provide
                    </h2>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div className="space-y-6">
                        <div className="bg-white border border-gray-200 rounded-lg p-6">
                          <h3 className="text-lg font-semibold text-gray-900 mb-3 flex items-center">
                            <Award className="h-5 w-5 mr-2 text-gayphx-purple" />
                            ISRC Code Services
                          </h3>
                          <ul className="space-y-2 text-gray-700 text-sm">
                            <li>• Official ISRC code assignment</li>
                            <li>• Industry-standard identification</li>
                            <li>• Royalty tracking support</li>
                            <li>• Distribution compatibility</li>
                          </ul>
                        </div>

                        <div className="bg-white border border-gray-200 rounded-lg p-6">
                          <h3 className="text-lg font-semibold text-gray-900 mb-3 flex items-center">
                            <FileText className="h-5 w-5 mr-2 text-gayphx-purple" />
                            Submission Platform
                          </h3>
                          <ul className="space-y-2 text-gray-700 text-sm">
                            <li>• Easy music submission process</li>
                            <li>• Real-time tracking system</li>
                            <li>• Status updates and notifications</li>
                            <li>• File management and storage</li>
                          </ul>
                        </div>

                        <div className="bg-white border border-gray-200 rounded-lg p-6">
                          <h3 className="text-lg font-semibold text-gray-900 mb-3 flex items-center">
                            <Music className="h-5 w-5 mr-2 text-gayphx-purple" />
                            Radio Opportunities
                          </h3>
                          <ul className="space-y-2 text-gray-700 text-sm">
                            <li>• Potential airplay on GayPHX Radio</li>
                            <li>• LGBTQ+ community exposure</li>
                            <li>• Professional radio consideration</li>
                            <li>• Performance royalty potential</li>
                          </ul>
                        </div>

                        <div className="bg-white border border-gray-200 rounded-lg p-6">
                          <h3 className="text-lg font-semibold text-gray-900 mb-3 flex items-center">
                            <Award className="h-5 w-5 mr-2 text-gayphx-purple" />
                            Commercial Use & Compensation
                          </h3>
                          <ul className="space-y-2 text-gray-700 text-sm">
                            <li>• Podcast and commercial use opportunities</li>
                            <li>• Fair compensation for commercial use</li>
                            <li>• Transparent tracking of all uses</li>
                            <li>• Rights change notifications</li>
                          </ul>
                        </div>
                      </div>

                      <div className="space-y-6">
                        <div className="bg-white border border-gray-200 rounded-lg p-6">
                          <h3 className="text-lg font-semibold text-gray-900 mb-3 flex items-center">
                            <Users className="h-5 w-5 mr-2 text-gayphx-purple" />
                            Educational Resources
                          </h3>
                          <ul className="space-y-2 text-gray-700 text-sm">
                            <li>• ISRC usage guides</li>
                            <li>• Music industry best practices</li>
                            <li>• Distribution strategies</li>
                            <li>• Royalty collection information</li>
                          </ul>
                        </div>

                        <div className="bg-white border border-gray-200 rounded-lg p-6">
                          <h3 className="text-lg font-semibold text-gray-900 mb-3 flex items-center">
                            <Shield className="h-5 w-5 mr-2 text-gayphx-purple" />
                            Community Support
                          </h3>
                          <ul className="space-y-2 text-gray-700 text-sm">
                            <li>• LGBTQ+ artist network</li>
                            <li>• Peer support and collaboration</li>
                            <li>• Industry connections</li>
                            <li>• Safe, inclusive environment</li>
                          </ul>
                        </div>

                        <div className="bg-white border border-gray-200 rounded-lg p-6">
                          <h3 className="text-lg font-semibold text-gray-900 mb-3 flex items-center">
                            <HelpCircle className="h-5 w-5 mr-2 text-gayphx-purple" />
                            Technical Support
                          </h3>
                          <ul className="space-y-2 text-gray-700 text-sm">
                            <li>• Platform assistance</li>
                            <li>• ISRC implementation help</li>
                            <li>• File format guidance</li>
                            <li>• Troubleshooting support</li>
                          </ul>
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {activeSection === 'what-we-dont' && (
                  <div>
                    <h2 className="text-3xl font-bold text-gray-900 mb-6 flex items-center">
                      <XCircle className="h-8 w-8 mr-3 text-gayphx-purple" />
                      What We Don't Do
                    </h2>

                    <div className="space-y-6">
                      <div className="bg-red-50 border border-red-200 rounded-lg p-6">
                        <h3 className="text-xl font-semibold text-red-900 mb-3">We Are NOT a Record Label</h3>
                        <ul className="space-y-2 text-red-800">
                          <li className="flex items-start">
                            <XCircle className="h-4 w-4 mr-2 mt-1 flex-shrink-0" />
                            <span>We don't sign artists to exclusive contracts</span>
                          </li>
                          <li className="flex items-start">
                            <XCircle className="h-4 w-4 mr-2 mt-1 flex-shrink-0" />
                            <span>We don't own your master recordings</span>
                          </li>
                          <li className="flex items-start">
                            <XCircle className="h-4 w-4 mr-2 mt-1 flex-shrink-0" />
                            <span>We don't take a percentage of your royalties</span>
                          </li>
                          <li className="flex items-start">
                            <XCircle className="h-4 w-4 mr-2 mt-1 flex-shrink-0" />
                            <span>We don't control your distribution or marketing</span>
                          </li>
                        </ul>
                      </div>

                      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
                        <h3 className="text-xl font-semibold text-yellow-900 mb-3">We Don't Provide These Services</h3>
                        <ul className="space-y-2 text-yellow-800">
                          <li className="flex items-start">
                            <XCircle className="h-4 w-4 mr-2 mt-1 flex-shrink-0" />
                            <span>Music production or recording services</span>
                          </li>
                          <li className="flex items-start">
                            <XCircle className="h-4 w-4 mr-2 mt-1 flex-shrink-0" />
                            <span>Distribution to streaming platforms (Spotify, Apple Music, etc.)</span>
                          </li>
                          <li className="flex items-start">
                            <XCircle className="h-4 w-4 mr-2 mt-1 flex-shrink-0" />
                            <span>Marketing or promotion campaigns</span>
                          </li>
                          <li className="flex items-start">
                            <XCircle className="h-4 w-4 mr-2 mt-1 flex-shrink-0" />
                            <span>Booking or tour management</span>
                          </li>
                          <li className="flex items-start">
                            <XCircle className="h-4 w-4 mr-2 mt-1 flex-shrink-0" />
                            <span>Legal services or contract review</span>
                          </li>
                          <li className="flex items-start">
                            <XCircle className="h-4 w-4 mr-2 mt-1 flex-shrink-0" />
                            <span>Financial advances or funding</span>
                          </li>
                        </ul>
                      </div>

                      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
                        <h3 className="text-xl font-semibold text-blue-900 mb-3">What You Need to Do Yourself</h3>
                        <ul className="space-y-2 text-blue-800">
                          <li className="flex items-start">
                            <ArrowRight className="h-4 w-4 mr-2 mt-1 flex-shrink-0" />
                            <span>Set up your own distribution (DistroKid, CD Baby, etc.)</span>
                          </li>
                          <li className="flex items-start">
                            <ArrowRight className="h-4 w-4 mr-2 mt-1 flex-shrink-0" />
                            <span>Register with SoundExchange for performance royalties</span>
                          </li>
                          <li className="flex items-start">
                            <ArrowRight className="h-4 w-4 mr-2 mt-1 flex-shrink-0" />
                            <span>Handle your own marketing and promotion</span>
                          </li>
                          <li className="flex items-start">
                            <ArrowRight className="h-4 w-4 mr-2 mt-1 flex-shrink-0" />
                            <span>Manage your own social media and fan engagement</span>
                          </li>
                          <li className="flex items-start">
                            <ArrowRight className="h-4 w-4 mr-2 mt-1 flex-shrink-0" />
                            <span>Handle legal matters and copyright registration</span>
                          </li>
                        </ul>
                      </div>
                    </div>
                  </div>
                )}

                {activeSection === 'faq' && (
                  <div>
                    <h2 className="text-3xl font-bold text-gray-900 mb-6 flex items-center">
                      <HelpCircle className="h-8 w-8 mr-3 text-gayphx-purple" />
                      Frequently Asked Questions
                    </h2>

                    <div className="space-y-6">
                      <div className="bg-white border border-gray-200 rounded-lg p-6">
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">Do I need to sign a contract with GayPHX Music?</h3>
                        <p className="text-gray-700">
                          No, we don't require long-term contracts. Our services are provided on a per-use basis. 
                          You can use our platform whenever you need ISRC codes or want to submit music for radio consideration.
                        </p>
                      </div>

                      <div className="bg-white border border-gray-200 rounded-lg p-6">
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">How much does it cost to get an ISRC code?</h3>
                        <p className="text-gray-700">
                          ISRC codes are provided free of charge as part of our service to the LGBTQ+ music community. 
                          We believe in removing barriers for independent artists.
                        </p>
                      </div>

                      <div className="bg-white border border-gray-200 rounded-lg p-6">
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">Can I use GayPHX Music if I'm not LGBTQ+?</h3>
                        <p className="text-gray-700">
                          While our primary focus is supporting LGBTQ+ artists, we welcome allies and supporters of the community. 
                          However, our resources and community features are designed with LGBTQ+ artists in mind.
                        </p>
                      </div>

                      <div className="bg-white border border-gray-200 rounded-lg p-6">
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">What happens if I want to work with a record label later?</h3>
                        <p className="text-gray-700">
                          Since you own all your music, you're free to work with any record label, distributor, or other 
                          industry partners. Our ISRC codes remain valid and your ownership is never compromised.
                        </p>
                      </div>

                      <div className="bg-white border border-gray-200 rounded-lg p-6">
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">Do you help with music production or recording?</h3>
                        <p className="text-gray-700">
                          No, we don't provide production or recording services. We focus on the business and administrative 
                          side of music (ISRC codes, industry education, community support). You'll need to handle 
                          production separately.
                        </p>
                      </div>

                      <div className="bg-white border border-gray-200 rounded-lg p-6">
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">How do I get my music on streaming platforms?</h3>
                        <p className="text-gray-700">
                          You'll need to use a distribution service like DistroKid, CD Baby, or TuneCore. These services 
                          will upload your music to Spotify, Apple Music, etc. We can provide ISRC codes to use with 
                          these distributors.
                        </p>
                      </div>

                      <div className="bg-white border border-gray-200 rounded-lg p-6">
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">How does commercial use and compensation work?</h3>
                        <p className="text-gray-700">
                          When you grant commercial use permission, GayPHX may use your music for commercial purposes 
                          (ads, podcasts, events, etc.). We track every use and ensure you receive fair compensation 
                          based on agreed rates. You'll be notified of any commercial use and can change your 
                          permissions at any time.
                        </p>
                      </div>

                      <div className="bg-white border border-gray-200 rounded-lg p-6">
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">Can I change my rights permissions after submission?</h3>
                        <p className="text-gray-700">
                          Yes! You can modify your rights permissions at any time through your artist dashboard. 
                          When you make changes, our admin team is automatically notified to ensure all systems 
                          are updated accordingly. This includes revoking radio play, podcast, or commercial use permissions.
                        </p>
                      </div>

                      <div className="bg-white border border-gray-200 rounded-lg p-6">
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">What if I have a question not answered here?</h3>
                        <p className="text-gray-700">
                          Feel free to contact us through our support channels. We're here to help and want to make 
                          sure you have all the information you need to succeed as an independent artist.
                        </p>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
