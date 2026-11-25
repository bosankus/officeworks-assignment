import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    include: ['test/**/*.test.ts'],
    coverage: {
      provider: 'v8',
      include: ['src/**/*.ts'],
      // The composition root only wires real adapters together; it's exercised by running the server.
      exclude: ['src/server.ts'],
      // A floor, not a target: CI fails if a change drops coverage noticeably.
      thresholds: { lines: 90, statements: 90, functions: 90, branches: 80 },
    },
  },
});
