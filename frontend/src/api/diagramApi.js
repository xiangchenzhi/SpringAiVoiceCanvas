const BASE_URL = '/api'

export async function sendDiagramCommand(transcript) {
  const response = await fetch(`${BASE_URL}/diagram`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ transcript })
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  }

  return response.json()
}
