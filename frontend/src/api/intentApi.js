const BASE_URL = '/api'

export async function sendIntent(transcript, conversationId) {
  const response = await fetch(`${BASE_URL}/intent`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ transcript, conversationId })
  })

  const data = await response.json()

  if (data.error) {
    throw new Error(data.error)
  }

  return data
}
