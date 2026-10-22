import { i18nBuilder } from "keycloakify/login";
import type { ThemeName } from "../kc.gen";

/**
 * Keycloak/Keycloakify không có sẵn tiếng Việt: thêm ngôn ngữ "vi" (realm đặt mặc định vi, xem
 * local/keycloak/ecommerce-realm.json). Câu chưa dịch rơi về tiếng Anh (xem i18n.vi.ts).
 */
const { useI18n, ofTypeI18n } = i18nBuilder
  .withThemeName<ThemeName>()
  .withExtraLanguages({
    vi: {
      label: "Tiếng Việt",
      getMessages: () => import("./i18n.vi"),
    },
  })
  .build();

type I18n = typeof ofTypeI18n;

export { useI18n, type I18n };
