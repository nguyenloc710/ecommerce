import type { NextConfig } from "next";

// Kho ảnh đánh giá (R2 prod, SeaweedFS local): next/image chỉ tối ưu ảnh từ domain khai báo. Chốt lúc build như NEXT_PUBLIC_*.
const mediaBase = new URL(process.env.NEXT_PUBLIC_MEDIA_BASE_URL ?? "http://localhost:8333/ecommerce-media");

const nextConfig: NextConfig = {
  // Build ra .next/standalone (server.js tối giản) để đóng image chạy trên k3s
  output: "standalone",
  cacheComponents: true,
  partialPrefetching: true,
  images: {
    // Next chặn ảnh từ IP nội bộ (SSRF); chỉ cho phép khi kho ảnh là SeaweedFS chạy local khi dev
    dangerouslyAllowLocalIP: ["localhost", "127.0.0.1"].includes(mediaBase.hostname),
    remotePatterns: [
      {
        protocol: mediaBase.protocol.replace(":", "") as "http" | "https",
        hostname: mediaBase.hostname,
        port: mediaBase.port,
        pathname: `${mediaBase.pathname.replace(/\/$/, "")}/**`,
      },
    ],
  },
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
