package vn.ecommerce.order.payment.internal;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import vn.ecommerce.common.web.error.BadRequestException;
import vn.ecommerce.order.payment.PaymentMethod;

/** Gom các cổng theo phương thức; thêm cổng mới không sửa chỗ nào khác. */
@Component
class PaymentGatewayRegistry {

    private final Map<PaymentMethod, PaymentGateway> gateways = new EnumMap<>(PaymentMethod.class);

    PaymentGatewayRegistry(List<PaymentGateway> beans) {
        beans.forEach(g -> gateways.put(g.method(), g));
    }

    Set<PaymentMethod> methods() {
        return Collections.unmodifiableSet(gateways.keySet());
    }

    PaymentGateway require(PaymentMethod method) {
        PaymentGateway gateway = gateways.get(method);
        if (gateway == null) {
            throw new BadRequestException(PaymentErrorKeys.METHOD_NOT_SUPPORTED, method.name());
        }
        return gateway;
    }
}
