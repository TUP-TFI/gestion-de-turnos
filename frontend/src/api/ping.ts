const API_URL = import.meta.env.VITE_API_URL

export async function getPing(): Promise<string> {
  const response = await fetch(`${API_URL}/ping`)

  if (!response.ok) {
    throw new Error(`El backend respondió con estado ${response.status}`)
  }

  return response.text()
}
