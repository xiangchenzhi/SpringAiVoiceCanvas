const BASE_URL = '/api'

export async function generateImage(prompt) {
  const response = await fetch(`${BASE_URL}/image/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ prompt })
  })

  const data = await response.json()

  // 后端返回了 error 字段（即使 HTTP 200）
  if (data.error) {
    throw new Error(data.error)
  }

  return data
}
