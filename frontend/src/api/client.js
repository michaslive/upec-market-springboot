import axios from 'axios'

export const tokenKey = 'upec-market-token'

export const api = axios.create({
  baseURL: `${(import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/+$/, '')}/api`,
  timeout: 10000,
})

api.interceptors.request.use((config) => {
  const token = window.localStorage.getItem(tokenKey)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      window.localStorage.removeItem(tokenKey)
      window.dispatchEvent(new CustomEvent('upec-session-expired'))
    }
    return Promise.reject(error)
  },
)
