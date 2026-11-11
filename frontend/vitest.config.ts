import path from "node:path";
import { defineConfig } from "vitest/config";

// Aliasul `@/` din tsconfig, ca testele să importe modulele la fel ca aplicația. Fără mediu DOM: testăm doar logică pură.
export default defineConfig({
  resolve: { alias: { "@": path.resolve(__dirname, "src") } },
  test: { include: ["src/**/*.test.ts"] }
});
