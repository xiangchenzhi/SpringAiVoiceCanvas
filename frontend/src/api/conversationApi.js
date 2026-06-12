const BASE_URL = '/api'

export async function fetchConversations() {
  const res = await fetch(`${BASE_URL}/conversations`)
  return res.json()
}

export async function fetchConversation(id) {
  const res = await fetch(`${BASE_URL}/conversations/${id}`)
  return res.json()
}
