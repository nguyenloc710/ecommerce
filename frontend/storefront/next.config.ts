import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Build ra .next/standalone (server.js tối giản) để đóng image chạy trên k3s
  output: "standalone",
  cacheComponents: true,
  partialPrefetching: true,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
