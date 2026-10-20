package vn.ecommerce.common.web.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Ký và kiểm tra chữ ký HMAC-SHA256 (hex, chữ thường) cho webhook; so sánh thời gian hằng. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HmacSignatures {

    private static final String ALGORITHM = "HmacSHA256";

    public static String sign(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Không khởi tạo được " + ALGORITHM, e);
        }
    }

    public static boolean matches(String secret, String payload, String providedSignature) {
        if (providedSignature == null || providedSignature.isBlank()) {
            return false;
        }
        byte[] expected = sign(secret, payload).getBytes(StandardCharsets.UTF_8);
        byte[] provided = providedSignature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }
}
