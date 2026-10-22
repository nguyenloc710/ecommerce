import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { KcPage } from "./kc.gen";

// Keycloak nhúng kcContext vào trang; chạy `npm run dev` không có context thì báo rõ
createRoot(document.getElementById("root")!).render(
  <StrictMode>{window.kcContext ? <KcPage kcContext={window.kcContext} /> : <h1>Không có Keycloak context (chạy qua Keycloak hoặc storybook)</h1>}</StrictMode>,
);
