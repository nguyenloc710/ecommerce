package vn.ecommerce.order.payment.internal;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Chữ ký VNPay 2.1.0: sắp xếp tham số {@code vnp_*} theo tên, bỏ giá trị rỗng và chính trường chữ ký, mã hóa giá trị
 * bằng URLEncoder US-ASCII, nối {@code key=value&...}, rồi HMAC-SHA512 với hashSecret (hex). Query string gửi đi dùng
 * đúng chuỗi đã ký nên hai bên luôn khớp.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class VnpaySigner {

    static final String SECURE_HASH = "vnp_SecureHash";
    static final String SECURE_HASH_TYPE = "vnp_SecureHashType";
    private static final String ALGORITHM = "HmacSHA512";

    /** Chuỗi dùng để ký (cũng là query string trước khi gắn chữ ký). */
    static String canonical(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(e -> !SECURE_HASH.equals(e.getKey()) && !SECURE_HASH_TYPE.equals(e.getKey()))
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    static String sign(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Không ký được dữ liệu VNPay", e);
        }
    }

    /** So khớp chữ ký VNPay gửi về (hex, không phân biệt hoa thường) trong thời gian hằng. */
    static boolean verify(String secret, Map<String, String> params) {
        String received = params.get(SECURE_HASH);
        if (received == null || received.isBlank() || secret == null || secret.isBlank()) {
            return false;
        }
        String expected = sign(secret, canonical(params));
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), received.trim().toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    /** Tách query string thô (IPN) thành map đã giải mã, giữ thứ tự. */
    static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new LinkedHashMap<>();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int i = pair.indexOf('=');
            String key = URLDecoder.decode(i < 0 ? pair : pair.substring(0, i), StandardCharsets.UTF_8);
            String value = i < 0 ? "" : URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }
}
