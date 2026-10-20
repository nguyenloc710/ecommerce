package vn.ecommerce.catalog.product.internal.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import vn.ecommerce.catalog.product.ProductSort;

/**
 * Tìm kiếm / lọc / facet bằng SQL native trên PostgreSQL (full-text {@code search_vector}, jsonb option, trigram).
 * SQL được ghép từ các mảnh cố định; mọi giá trị từ người dùng đều bind tham số, kể cả tên option.
 */
@Repository
public class ProductSearchRepository {

    /** Tên option (key jsonb) chỉ nhận chữ, số, gạch; chặn key lạ ngay từ đầu. */
    private static final Pattern OPTION_KEY = Pattern.compile("^[a-z0-9_-]{1,30}$");
    private static final String ACTIVE_VARIANT = "v.product_id = p.id AND v.deleted_at IS NULL AND v.status = 'ACTIVE'";
    private static final String MIN_PRICE = "(SELECT MIN(v.price) FROM product_variant v WHERE " + ACTIVE_VARIANT + ")";
    private static final String TS_QUERY = "websearch_to_tsquery('simple', immutable_unaccent(:q))";

    @PersistenceContext
    private EntityManager entityManager;

    /** Trang id sản phẩm theo thứ tự sắp xếp; service tra thông tin thẻ theo id. */
    public Page<ProductRow> search(ProductSearchCriteria criteria, Pageable pageable) {
        Map<String, Object> params = new LinkedHashMap<>();
        String where = buildWhere(criteria, params);

        String order = switch (criteria.sort()) {
            case PRICE_ASC -> MIN_PRICE + " ASC NULLS LAST, p.created_at DESC";
            case PRICE_DESC -> MIN_PRICE + " DESC NULLS LAST, p.created_at DESC";
            case NEWEST -> criteria.hasQuery()
                    ? "ts_rank(p.search_vector, " + TS_QUERY + ") DESC, p.published_at DESC NULLS LAST, p.created_at DESC"
                    : "p.published_at DESC NULLS LAST, p.created_at DESC";
        };

        Query select = entityManager.createNativeQuery(
                "SELECT p.id, p.slug, p.name, p.published_at FROM product p " + where + " ORDER BY " + order, Tuple.class);
        Query count = entityManager.createNativeQuery("SELECT COUNT(*) FROM product p " + where);
        params.forEach((k, v) -> {
            select.setParameter(k, v);
            count.setParameter(k, v);
        });
        select.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Tuple> tuples = select.getResultList();
        List<ProductRow> rows = tuples.stream().map(t -> new ProductRow(
                t.get(0, UUID.class), t.get(1, String.class), t.get(2, String.class), toInstant(t.get(3)))).toList();
        long total = ((Number) count.getSingleResult()).longValue();
        return new PageImpl<>(rows, pageable, total);
    }

    /** Đếm facet trên tập sản phẩm khớp từ khóa/danh mục/thương hiệu (không áp lọc option để người dùng còn chọn thêm). */
    public FacetCounts facets(ProductSearchCriteria criteria) {
        ProductSearchCriteria base = new ProductSearchCriteria(criteria.q(), criteria.categoryIds(), criteria.brandId(),
                Map.of(), null, null, ProductSort.NEWEST);
        Map<String, Object> params = new LinkedHashMap<>();
        String where = buildWhere(base, params);

        Map<String, LinkedHashMap<String, Long>> options = groupCounts(entityManager.createNativeQuery("""
                SELECT kv.key, kv.value, COUNT(DISTINCT p.id)
                FROM product p
                JOIN product_variant v ON %s
                CROSS JOIN LATERAL jsonb_each_text(v.option_values) kv
                %s
                GROUP BY kv.key, kv.value ORDER BY kv.key, kv.value
                """.formatted(ACTIVE_VARIANT, where), Tuple.class), params);
        Map<String, LinkedHashMap<String, Long>> attributes = groupCounts(entityManager.createNativeQuery("""
                SELECT kv.key, kv.value, COUNT(DISTINCT p.id)
                FROM product p
                CROSS JOIN LATERAL jsonb_each_text(p.attributes) kv
                %s AND jsonb_typeof(p.attributes -> kv.key) = 'string'
                GROUP BY kv.key, kv.value ORDER BY kv.key, kv.value
                """.formatted(where), Tuple.class), params);

        Query price = entityManager.createNativeQuery(
                "SELECT MIN(v.price), MAX(v.price) FROM product p JOIN product_variant v ON " + ACTIVE_VARIANT + " " + where, Tuple.class);
        params.forEach(price::setParameter);
        Tuple range = (Tuple) price.getSingleResult();
        return new FacetCounts(options, attributes, (BigDecimal) range.get(0), (BigDecimal) range.get(1));
    }

    // ---- ghép WHERE từ mảnh cố định + tham số bind ----

    private static String buildWhere(ProductSearchCriteria c, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("WHERE p.deleted_at IS NULL AND p.status = 'ACTIVE'");
        if (c.categoryIds() != null && !c.categoryIds().isEmpty()) {
            sb.append(" AND p.category_id IN (:categoryIds)");
            params.put("categoryIds", new ArrayList<>(c.categoryIds()));
        }
        if (c.brandId() != null) {
            sb.append(" AND p.brand_id = :brandId");
            params.put("brandId", c.brandId());
        }
        if (c.hasQuery()) {
            String q = c.q().trim();
            sb.append(" AND (p.search_vector @@ ").append(TS_QUERY)
                    .append(" OR immutable_unaccent(lower(p.name)) LIKE :qLike)");
            params.put("q", q);
            params.put("qLike", "%" + q.toLowerCase(Locale.ROOT) + "%");
        }
        List<String> variantConditions = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, List<String>> opt : c.options().entrySet()) {
            if (!OPTION_KEY.matcher(opt.getKey()).matches() || opt.getValue().isEmpty()) {
                continue;
            }
            variantConditions.add("v.option_values ->> :optKey" + i + " IN (:optVals" + i + ")");
            params.put("optKey" + i, opt.getKey());
            params.put("optVals" + i, opt.getValue());
            i++;
        }
        if (c.priceMin() != null) {
            variantConditions.add("v.price >= :priceMin");
            params.put("priceMin", c.priceMin());
        }
        if (c.priceMax() != null) {
            variantConditions.add("v.price <= :priceMax");
            params.put("priceMax", c.priceMax());
        }
        if (!variantConditions.isEmpty()) {
            sb.append(" AND EXISTS (SELECT 1 FROM product_variant v WHERE ").append(ACTIVE_VARIANT)
                    .append(" AND ").append(String.join(" AND ", variantConditions)).append(")");
        }
        return sb.toString();
    }

    /** Kiểu trả về của cột timestamptz trong native query khác nhau giữa các bản Hibernate/driver. */
    private static Instant toInstant(Object value) {
        return switch (value) {
            case null -> null;
            case Instant instant -> instant;
            case OffsetDateTime odt -> odt.toInstant();
            case Timestamp ts -> ts.toInstant();
            default -> throw new IllegalStateException("Kiểu thời gian không hỗ trợ: " + value.getClass());
        };
    }

    private static Map<String, LinkedHashMap<String, Long>> groupCounts(Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
        @SuppressWarnings("unchecked")
        List<Tuple> tuples = query.getResultList();
        Map<String, LinkedHashMap<String, Long>> result = new LinkedHashMap<>();
        for (Tuple t : tuples) {
            result.computeIfAbsent(t.get(0, String.class), k -> new LinkedHashMap<>())
                    .put(t.get(1, String.class), ((Number) t.get(2)).longValue());
        }
        return result;
    }
}
