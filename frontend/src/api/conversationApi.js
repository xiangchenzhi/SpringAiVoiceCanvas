const BASE_URL = '/api'

export async function fetchConversations() {
  const res = await fetch(`${BASE_URL}/conversations`)
  return res.json()
}

export async function fetchConversation(id) {
  const res = await fetch(`${BASE_URL}/conversations/${id}`)
  return res.json()
}

export async function fetchVersionTree(conversationId) {
  const res = await fetch(`${BASE_URL}/versions/${conversationId}/tree`)
  return res.json()
}

export async function fetchVersionDetail(versionId) {
  const res = await fetch(`${BASE_URL}/versions/${versionId}`)
  return res.json()
}

export async function switchVersion(conversationId, versionId) {
  const res = await fetch(`${BASE_URL}/versions/switch`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ conversationId, versionId })
  })
  return res.json()
}
