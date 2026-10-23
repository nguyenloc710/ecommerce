package vn.ecommerce.order.checkout.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import vn.ecommerce.order.catalogclient.ReserveLine;
import vn.ecommerce.order.catalogclient.SkuSnapshot;
import vn.ecommerce.order.checkout.CheckoutQuoteView;
import vn.ecommerce.order.orders.PlaceOrderCommand;

/** Dòng hàng đã ghép giá catalog; cùng SKU khác nội dung khắc vẫn là hai dòng nhưng giữ hàng gộp theo SKU. */
record PricedLines(List<PricedLine> lines, BigDecimal subtotal) {

    record PricedLine(SkuSnapshot sku, int quantity, String engraving) {
        BigDecimal lineTotal() {
            return sku.price().multiply(BigDecimal.valueOf(quantity));
        }
    }

    List<ReserveLine> toReserveLines() {
        Map<UUID, PricedLine> bySku = lines.stream().collect(Collectors.toMap(l -> l.sku().skuId(), l -> l,
                (a, b) -> new PricedLine(a.sku(), a.quantity() + b.quantity(), null), java.util.LinkedHashMap::new));
        return bySku.values().stream().map(l -> new ReserveLine(l.sku().skuId(), l.quantity(), l.sku().price())).toList();
    }

    List<UUID> skuIds() {
        return lines.stream().map(l -> l.sku().skuId()).distinct().toList();
    }

    List<PlaceOrderCommand.Line> toOrderLines() {
        return lines.stream().map(l -> new PlaceOrderCommand.Line(l.sku().skuId(), l.sku().productId(), l.sku().productSlug(),
                l.sku().productName(), l.sku().variantLabel(), l.sku().skuCode(), l.sku().imageUrl(), l.sku().price(), l.quantity(),
                l.engraving())).toList();
    }

    List<CheckoutQuoteView.Line> toQuoteLines() {
        return lines.stream().map(l -> new CheckoutQuoteView.Line(l.sku().skuId(), l.sku().productSlug(), l.sku().productName(),
                l.sku().variantLabel(), l.sku().imageUrl(), l.sku().price(), l.quantity(), l.engraving(), l.lineTotal())).toList();
    }
}
