import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    include: ["test/**/*.test.ts"],
    // Testes de Rules e de Firestore falam com o Emulator: um arquivo por vez.
    fileParallelism: false,
    testTimeout: 20_000,
  },
});
