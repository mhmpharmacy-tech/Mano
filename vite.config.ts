import { defineConfig } from 'vite';
import { fileURLToPath } from 'node:url';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    rollupOptions: {
      input: {
        app: fileURLToPath(new URL('./index.html', import.meta.url)),
        preview: fileURLToPath(new URL('./preview/index.html', import.meta.url)),
      },
    },
  },
  server: {
    host: '0.0.0.0',
    port: 3000,
    hmr: false,
  },
  preview: {
    host: '0.0.0.0',
    port: 3000,
  },
});
