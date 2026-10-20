package vn.ecommerce.catalog.product.internal;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Sinh slug từ tên tiếng Việt: bỏ dấu, chữ thường, gạch nối (B-06). */
public final class Slugs {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("^-+|-+$");
    private static final int MAX_LENGTH = 120;

    private Slugs() {
    }

    public static String from(String name) {
        String ascii = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replace('đ', 'd').replace('Đ', 'D');
        ascii = DIACRITICS.matcher(ascii).replaceAll("").toLowerCase(Locale.ROOT);
        String slug = EDGE_DASHES.matcher(NON_ALNUM.matcher(ascii).replaceAll("-")).replaceAll("");
        return slug.length() > MAX_LENGTH ? EDGE_DASHES.matcher(slug.substring(0, MAX_LENGTH)).replaceAll("") : slug;
    }

    /** Slug do người dùng nhập (nếu có) được chuẩn hóa lại; trống thì sinh từ tên. */
    public static String resolve(String requestedSlug, String name) {
        return requestedSlug == null || requestedSlug.isBlank() ? from(name) : from(requestedSlug);
    }
}
