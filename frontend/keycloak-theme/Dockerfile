# Image Keycloak kèm theme đăng nhập "ecommerce" (E6.4), đã build sẵn (optimized) cho Keycloak Operator.
# Cần jar trước: npm run build-keycloak-theme  →  dist_keycloak/keycloak-theme.jar
#   docker build -t <registry>/keycloak:<sha> frontend/keycloak-theme
# Tùy chọn build-time phải khớp CR deploy/keycloak/base/keycloak.yaml (db, health, metrics).

ARG KEYCLOAK_IMAGE=quay.io/keycloak/keycloak:26.7.5

FROM ${KEYCLOAK_IMAGE} AS builder
ENV KC_DB=postgres \
    KC_HEALTH_ENABLED=true \
    KC_METRICS_ENABLED=true
COPY dist_keycloak/keycloak-theme.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build

FROM ${KEYCLOAK_IMAGE}
COPY --from=builder /opt/keycloak/ /opt/keycloak/
