import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  return {
    plugins: [react()],
    define: {
      // Make environment variables available at build time
      'import.meta.env.VITE_API_BASE_URL': JSON.stringify(env.VITE_API_BASE_URL || 'https://nsa4fef6um.ap-southeast-1.awsapprunner.com'),
      'import.meta.env.VITE_AUTH_BASE_URL': JSON.stringify(env.VITE_AUTH_BASE_URL || env.VITE_API_BASE_URL || 'https://nsa4fef6um.ap-southeast-1.awsapprunner.com'),
      'import.meta.env.VITE_NODE_ENV': JSON.stringify(env.VITE_NODE_ENV || mode),
    },
    server: {
      allowedHosts: true, // Allow all hosts (required for ngrok)
      host: "0.0.0.0",
      port: 5173,
      proxy: {
        "/api": {
          target: env.VITE_API_BASE_URL || "http://localhost:8082",
          changeOrigin: true,
          secure: false,
          ws: true,
        },
      },
    },
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "src"),
        "@assets": path.resolve(__dirname, "src/assets"),
        "@layouts": path.resolve(__dirname, "src/layouts"),
        "@fonts": path.resolve(__dirname, "src/assets/fonts"),
        "@components": path.resolve(__dirname, "src/components"),
        "@pages": path.resolve(__dirname, "src/pages"),
        "@utils": path.resolve(__dirname, "src/utils"),
        "@hooks": path.resolve(__dirname, "src/hooks"),
        "@services": path.resolve(__dirname, "src/services"),
        "@styles": path.resolve(__dirname, "src/styles"),
        "@config": path.resolve(__dirname, "config"),
      },
    },
  };
});
