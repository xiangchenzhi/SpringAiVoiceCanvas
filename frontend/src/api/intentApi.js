import { getAuthHeaders } from './authApi.js'

const BASE_URL = '/api'

export async function sendIntent(transcript, conversationId, parentVersionId) {
  const body = { transcript, conversationId }
  if (parentVersionId) body.parentVersionId = parentVersionId
  const response = await fetch(`${BASE_URL}/intent`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
    body: JSON.stringify(body)
  })

  const data = await response.json()

  if (data.error) {
    throw new Error(data.error)
  }

  return data
}
