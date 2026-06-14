import { getAuthHeaders } from './authApi.js'

const BASE_URL = '/api'

export async function sendVoiceCommand(transcript) {
  const response = await fetch(`${BASE_URL}/voice`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeaders()
    },
    body: JSON.stringify({
      transcript,
      canvasWidth: 1000,
      canvasHeight: 600
    })
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  }

  return response.json()
}
