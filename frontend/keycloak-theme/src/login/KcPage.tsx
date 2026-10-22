import type { ClassKey } from "keycloakify/login";
import DefaultPage from "keycloakify/login/DefaultPage";
import Template from "keycloakify/login/Template";
import { lazy, Suspense } from "react";
import "./main.css";
import type { KcContext } from "./KcContext";
import { useI18n } from "./i18n";

const UserProfileFormFields = lazy(() => import("keycloakify/login/UserProfileFormFields"));

/**
 * Dùng trang mặc định của Keycloakify (giữ đủ logic Keycloak: OTP nhân viên, đổi mật khẩu, xác minh email...) nhưng
 * tắt CSS PatternFly và gắn class riêng theo nhận diện storefront (UI-01..UI-05) trong main.css.
 */
export default function KcPage(props: { kcContext: KcContext }) {
  const { kcContext } = props;
  const { i18n } = useI18n({ kcContext });

  return (
    <Suspense>
      <DefaultPage
        kcContext={kcContext}
        i18n={i18n}
        classes={classes}
        Template={Template}
        doUseDefaultCss={false}
        UserProfileFormFields={UserProfileFormFields}
        doMakeUserConfirmPassword={true}
      />
    </Suspense>
  );
}

const classes = {
  kcHtmlClass: "kc-html",
  kcBodyClass: "kc-body",
  kcLoginClass: "kc-login",
  kcHeaderClass: "kc-header",
  kcHeaderWrapperClass: "kc-brand",
  kcFormCardClass: "kc-card",
  kcFormHeaderClass: "kc-card-header",
  kcLocaleMainClass: "kc-locale",
  kcLocaleListClass: "kc-locale-list",
  kcLocaleItemClass: "kc-locale-item",
  kcFormClass: "kc-form",
  kcFormGroupClass: "kc-group",
  kcLabelWrapperClass: "kc-label-wrapper",
  kcLabelClass: "kc-label",
  kcInputWrapperClass: "kc-input-wrapper",
  kcInputClass: "kc-input",
  kcInputGroup: "kc-input-group",
  kcInputErrorMessageClass: "kc-input-error",
  kcInputHelperTextBeforeClass: "kc-help",
  kcInputHelperTextAfterClass: "kc-help",
  kcFormPasswordVisibilityButtonClass: "kc-eye",
  kcFormPasswordVisibilityIconShow: "kc-eye-show",
  kcFormPasswordVisibilityIconHide: "kc-eye-hide",
  kcFormOptionsClass: "kc-options",
  kcFormOptionsWrapperClass: "kc-options-wrapper",
  kcFormSettingClass: "kc-settings",
  kcCheckboxInputClass: "kc-checkbox",
  kcFormButtonsClass: "kc-buttons",
  kcButtonClass: "kc-btn",
  kcButtonPrimaryClass: "kc-btn-primary",
  kcButtonDefaultClass: "kc-btn-secondary",
  kcButtonBlockClass: "kc-btn-block",
  kcButtonLargeClass: "kc-btn-large",
  kcAlertClass: "kc-alert",
  kcAlertTitleClass: "kc-alert-text",
  kcSignUpClass: "kc-signup",
  kcInfoAreaWrapperClass: "kc-info",
  kcContentWrapperClass: "kc-content",
  kcFormSocialAccountSectionClass: "kc-social",
  kcFormSocialAccountListClass: "kc-social-list",
  kcFormSocialAccountListButtonClass: "kc-btn kc-btn-secondary kc-btn-block",
  kcFormSocialAccountNameClass: "kc-social-name",
} satisfies { [key in ClassKey]?: string };
