/** Vite configuration for the Vue 3 frontend, API proxy, and vendor chunks. */
import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:8080'
  const wsTarget = env.VITE_WS_PROXY_TARGET || 'ws://127.0.0.1:8080'

  return {
    plugins: [
      vue(),
      AutoImport({
        imports: ['vue', 'vue-router', 'pinia'],
        resolvers: [ElementPlusResolver()],
        dts: 'src/auto-imports.d.ts',
      }),
      Components({
        resolvers: [ElementPlusResolver()],
        dts: 'src/components.d.ts',
      }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      host: true,
      port: 5173,
      proxy: {
        '/api': { target: apiTarget, changeOrigin: true },
        '/ws': { target: wsTarget, ws: true, changeOrigin: true },
      },
    },
    preview: {
      proxy: {
        '/api': { target: apiTarget, changeOrigin: true },
        '/ws': { target: wsTarget, ws: true, changeOrigin: true },
      },
    },
    build: {
      chunkSizeWarningLimit: 700,
      rollupOptions: {
        output: {
          manualChunks(id: string) {
            if (id.includes('node_modules/echarts') || id.includes('node_modules/zrender')) return 'charts'
            if (id.includes('node_modules/element-plus') || id.includes('@element-plus')) return 'element-plus'
            if (id.includes('node_modules/vue') || id.includes('node_modules/pinia') || id.includes('node_modules/vue-router')) return 'vue-vendor'
            return undefined
          },
        },
      },
    },
  }
})
