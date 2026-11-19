import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// In dev, forward /chat to the Java backend (PORT defaults to 8080).
export default defineConfig({
  plugins: [react()],
  server: { proxy: { "/chat": "http://127.0.0.1:8080" } },
});
