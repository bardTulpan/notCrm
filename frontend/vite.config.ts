import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Proxying /api to the backend keeps frontend+backend on one origin when this is served
// through a single public tunnel — that's what lets the HttpOnly refresh cookie (SameSite=Lax)
// actually work cross-... well, same-origin, and avoids needing CORS at all for real traffic.
const apiProxy = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: apiProxy,
  },
  preview: {
    proxy: apiProxy,
    // Vite's DNS-rebinding guard rejects unrecognized Host headers by default. The Cloudflare
    // quick tunnel forwards the public *.trycloudflare.com hostname (a new random one each time
    // the tunnel restarts), so allow that suffix rather than hardcoding one ephemeral hostname.
    allowedHosts: ['.trycloudflare.com'],
  },
})
