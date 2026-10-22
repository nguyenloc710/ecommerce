# Storefront Next.js (output: standalone). Build riêng cho từng môi trường vì NEXT_PUBLIC_* và các trang prerender
# (metadata, sitemap, JSON-LD) chốt URL site lúc build:
#   docker build --build-arg NEXT_PUBLIC_SITE_URL=https://shop.example.vn -t <registry>/storefront:<sha>-prod .
# Biến chạy (AUTH_*, API_BASE_URL...) truyền lúc runtime qua ConfigMap/SealedSecret (deploy/storefront).

ARG NODE_IMAGE=public.ecr.aws/docker/library/node:22.21.0-alpine

FROM ${NODE_IMAGE} AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

FROM ${NODE_IMAGE} AS build
WORKDIR /app
ARG NEXT_PUBLIC_SITE_URL=http://localhost:3000
ARG NEXT_PUBLIC_SHOP_NAME="Tên shop"
# Lúc build có thể không gọi được API: trang dùng dữ liệu dự phòng rồi tự làm mới theo cacheLife khi chạy
ARG API_BASE_URL=http://localhost:8080
ENV NEXT_PUBLIC_SITE_URL=${NEXT_PUBLIC_SITE_URL} \
    NEXT_PUBLIC_SHOP_NAME=${NEXT_PUBLIC_SHOP_NAME} \
    API_BASE_URL=${API_BASE_URL} \
    NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM ${NODE_IMAGE} AS runtime
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    HOSTNAME=0.0.0.0 \
    PORT=3000
# Bản standalone chỉ gồm server.js + node_modules cần thiết; static và public chép riêng
COPY --from=build --chown=1000:1000 /app/.next/standalone ./
COPY --from=build --chown=1000:1000 /app/.next/static ./.next/static
COPY --from=build --chown=1000:1000 /app/public ./public
# Chạy non-root (user "node" uid 1000 của image); .next/cache gắn emptyDir khi root filesystem read-only
USER 1000:1000
EXPOSE 3000
CMD ["node", "server.js"]
