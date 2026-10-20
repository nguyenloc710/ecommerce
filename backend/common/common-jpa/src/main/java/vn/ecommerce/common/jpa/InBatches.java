package vn.ecommerce.common.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/** Chia danh sách khóa thành lô để truy vấn {@code IN (...)} không vượt giới hạn tham số của driver. */
public final class InBatches {

    public static final int DEFAULT_SIZE = 500;

    private InBatches() {
    }

    public static <K, R> List<R> query(Collection<K> keys, Function<List<K>, List<R>> queryFn) {
        return query(keys, DEFAULT_SIZE, queryFn);
    }

    public static <K, R> List<R> query(Collection<K> keys, int batchSize, Function<List<K>, List<R>> queryFn) {
        if (keys.isEmpty()) {
            return List.of();
        }
        List<K> all = new ArrayList<>(keys);
        List<R> result = new ArrayList<>();
        for (int from = 0; from < all.size(); from += batchSize) {
            result.addAll(queryFn.apply(all.subList(from, Math.min(all.size(), from + batchSize))));
        }
        return result;
    }
}
