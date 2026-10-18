import { defineConfig } from 'vite';
export default defineConfig({ base: './', build: { outDir: 'site', emptyOutDir: true }, worker: { format: 'es' } });
