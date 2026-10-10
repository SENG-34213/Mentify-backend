package com.mentify.service.stripe;

import com.mentify.config.StripeProperties;
import com.mentify.exception.PaymentProviderException;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StripeRefundGatewayImpl implements StripeRefundGateway {

    private final StripeProperties stripeProperties;

    @Override
    public StripeRefund createRefund(CreateStripeRefundRequest request) {
        if (stripeProperties.getSecretKey() == null || stripeProperties.getSecretKey().isBlank()) {
            throw new PaymentProviderException("Stripe secret key is not configured");
        }

        RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                .setPaymentIntent(request.paymentIntentId())
                .putMetadata("payment_id", request.paymentId().toString());

        if (StringUtils.hasText(request.reason())) {
            paramsBuilder.putMetadata("reason", request.reason().trim());
        }

        RequestOptions requestOptions = RequestOptions.builder()
                .setApiKey(stripeProperties.getSecretKey())
                .setIdempotencyKey(request.idempotencyKey())
                .build();

        try {
            Refund refund = Refund.create(paramsBuilder.build(), requestOptions);
            if (refund.getId() == null || refund.getStatus() == null) {
                throw new PaymentProviderException("Stripe refund response was incomplete");
            }
            return new StripeRefund(refund.getId(), refund.getStatus());
        } catch (StripeException ex) {
            throw new PaymentProviderException("Stripe refund creation failed", ex);
        }
    }
}
