package vn.ecommerce.order.checkout.internal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.common.web.error.ConflictException;
import vn.ecommerce.order.catalogclient.CatalogClient;
import vn.ecommerce.order.catalogclient.SkuSnapshot;

/** Bước 1 của checkout: tra giá/tồn hiện tại từ catalog, không tin giá client. */
@Component
@RequiredArgsConstructor
class PricingStep {

    private final CatalogClient catalog;

    PricedLines price(List<CheckoutRequest.Line> requested) {
        if (requested.isEmpty()) {
            throw new BadRequestException(CheckoutErrorKeys.EMPTY_CART);
        }
        List<UUID> skuIds = requested.stream().map(CheckoutRequest.Line::skuId).distinct().toList();
        Map<UUID, SkuSnapshot> skus = catalog.lookup(skuIds).stream().collect(Collectors.toMap(SkuSnapshot::skuId, Function.identity()));
        Map<UUID, Integer> wanted = requested.stream().collect(Collectors.toMap(CheckoutRequest.Line::skuId, CheckoutRequest.Line::quantity, Integer::sum));

        List<PricedLines.PricedLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CheckoutRequest.Line line : requested) {
            SkuSnapshot sku = skus.get(line.skuId());
            if (sku == null) {
                throw new BadRequestException(CheckoutErrorKeys.SKU_NOT_FOUND, line.skuId().toString());
            }
            if (!sku.sellable()) {
                throw new ConflictException(CheckoutErrorKeys.SKU_NOT_SELLABLE, sku.productName());
            }
            if (sku.available() < wanted.get(sku.skuId())) {
                throw new ConflictException(CheckoutErrorKeys.INSUFFICIENT_STOCK, sku.productName(), sku.available());
            }
            PricedLines.PricedLine priced = new PricedLines.PricedLine(sku, line.quantity(), line.normalizedEngraving());
            lines.add(priced);
            subtotal = subtotal.add(priced.lineTotal());
        }
        return new PricedLines(lines, subtotal);
    }
}
