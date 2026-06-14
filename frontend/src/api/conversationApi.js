import { getAuthHeaders } from './authApi.js'

const BASE_URL = '/api'

export async function fetchConversations() {
  const res = await fetch(`${BASE_URL}/conversations`, {
    headers: { ...getAuthHeaders() }
  })
  return res.json()
}

export async function fetchConversation(id) {
  const res = await fetch(`${BASE_URL}/conversations/${id}`, {
    headers: { ...getAuthHeaders() }
  })
  return res.json()
}

export async function fetchVersionTree(conversationId) {
  const res = await fetch(`${BASE_URL}/versions/${conversationId}/tree`, {
    headers: { ...getAuthHeaders() }
  })
  return res.json()
}

export async function fetchVersionDetail(versionId) {
  const res = await fetch(`${BASE_URL}/versions/${versionId}`, {
    headers: { ...getAuthHeaders() }
  })
  return res.json()
}

export async function switchVersion(conversationId, versionId) {
  const res = await fetch(`${BASE_URL}/versions/switch`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
    body: JSON.stringify({ conversationId, versionId })
  })
  return res.json()
}

export async function deleteConversation(id) {
  const res = await fetch(`${BASE_URL}/conversations/${id}`, {
    method: 'DELETE',
    headers: { ...getAuthHeaders() }
  })
  if (!res.ok) throw new Error('删除失败')
  return res.json()
}
