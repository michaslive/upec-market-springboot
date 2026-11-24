import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig, loadEnv } from 'vite'

// Vite no acepta hosts públicos por defecto. Para usar un túnel de Cloudflare,
// define VITE_ALLOWED_HOSTS con el host público del frontend.
// Si no se define, la app sigue funcionando en local normalmente.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  const allowedHosts = (env.VITE_ALLOWED_HOSTS ?? '')
    .split(',')
    .map((host) => host.trim())
    .filter(Boolean)

  return {
    plugins: [react(), tailwindcss()],
    server: {
      host: '0.0.0.0',
      allowedHosts,
    },
  }
})
