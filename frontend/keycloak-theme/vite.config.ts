import react from "@vitejs/plugin-react";
import { keycloakify } from "keycloakify/vite-plugin";
import { defineConfig } from "vite";

// Theme đăng nhập Keycloak (E6.4): chỉ theme login, Keycloak 26 dùng account console mặc định.
export default defineConfig({
  plugins: [
    react(),
    keycloakify({
      themeName: "ecommerce",
      accountThemeImplementation: "none",
      keycloakVersionTargets: {
        "22-to-25": false,
        "all-other-versions": "keycloak-theme.jar",
      },
    }),
  ],
});
