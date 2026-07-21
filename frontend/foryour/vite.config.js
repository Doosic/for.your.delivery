import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { fileURLToPath } from 'url'
import { defineConfig } from 'vite'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig(({ mode }) => {
  const isProd = mode === 'production'
  const timestamp = new Date()
    .toISOString()
    .replace(/[-:TZ.]/g, '')
    .slice(0, 14)

  return {
    plugins: [
      react(),
      tailwindcss(),
      {
        name: 'append-build-timestamp',
        enforce: 'post',
        transformIndexHtml(html) {
          const query = `?t=${timestamp}`

          if (isProd) {
            html = html
              .replace(/(<script[^>]+src=")(?!https?:|\/\/)(?:\.\/)?js\//g, '$1../app/js/')
              .replace(/(<link[^>]+rel="modulepreload"[^>]+href=")(?!https?:|\/\/)(?:\.\/)?js\//g, '$1../app/js/')
              .replace(/(<link[^>]+href=")(?!https?:|\/\/)(?:\.\/)?css\//g, '$1../app/css/')
          }

          return html
            .replace(/(<script[^>]+src=")([^"]+\.js)"/g, (_match, prefix, file) => `${prefix}${file}${query}"`)
            .replace(/(<link[^>]+href=")([^"]+\.css)"/g, (_match, prefix, file) => `${prefix}${file}${query}"`)
        },
      },
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
        '@/assets': path.resolve(__dirname, './src/assets'),
        '@/components': path.resolve(__dirname, './src/components'),
        '@/layouts': path.resolve(__dirname, './src/layouts'),
        '@/pages': path.resolve(__dirname, './src/pages'),
        '@/services': path.resolve(__dirname, './src/services'),
        '@/shared': path.resolve(__dirname, './src/shared'),
      },
    },
    base: isProd ? '/delivery/resources/app' : '/',
    build: {
      outDir: path.resolve(__dirname, '../../src/main/resources/static/resources/app'),
      emptyOutDir: true,
      rollupOptions: {
        output: {
          entryFileNames: 'js/[name].js',
          chunkFileNames: 'js/[name].js',
          assetFileNames: (assetInfo) => {
            const name = assetInfo.name ?? ''
            const ext = path.extname(name).slice(1).toLowerCase()

            if (ext === 'css') return 'css/[name][extname]'
            if (/^(woff2?|eot|ttf|otf)$/.test(ext)) return 'fonts/[name][extname]'
            if (/^(png|jpe?g|gif|svg|webp|avif|ico)$/.test(ext)) return 'img/[name][extname]'
            return 'assets/[name][extname]'
          },
        },
      },
    },
    server: {
      port: 3500,
      open: true,
      host: true,
      proxy: {
        '/delivery/': {
          target: 'http://localhost:8920',
          changeOrigin: true,
          secure: false,
        },
      },
    },
  }
})
