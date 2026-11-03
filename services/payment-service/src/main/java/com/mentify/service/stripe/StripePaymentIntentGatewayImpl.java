package com.mentify.service.stripe;

import com.mentify.config.StripeProperties;
import com.mentify.exception.PaymentProviderException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StripePaymentIntentGatewayImpl implements StripePaymentIntentGateway {

    private final StripeProperties stripeProperties;

    @Override
    public StripePaymentIntent createPaymentIntent(CreateStripePaymentIntentRequest request) {
        if (stripeProperties.getSecretKey() == null || stripeProperties.getSecretKey().isBlank()) {
            throw new PaymentProviderException("Stripe secret key is not configured");
        }

        PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(request.amountInMinorUnits())
                .setCurrency(request.currency().toLowerCase(Locale.ROOT))
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .putMetadata("payment_id", request.paymentId().toString())
                .putMetadata("payment_type", request.paymentType())
                ;

        if (request.studentId() != null) paramsBuilder.putMetadata("student_id", request.studentId().toString());
        if (request.courseId() != null) paramsBuilder.putMetadata("course_id", request.courseId().toString());
        if (request.productId() != null) paramsBuilder.putMetadata("product_id", request.productId().toString());
        if (request.receiptEmail() != null && !request.receiptEmail().isBlank()) {
            paramsBuilder.setReceiptEmail(request.receiptEmail());
        }
        PaymentIntentCreateParams params = paramsBuilder.build();

        RequestOptions requestOptions = RequestOptions.builder()
                .setApiKey(stripeProperties.getSecretKey())
                .setIdempotencyKey(request.idempotencyKey())
                .build();

        try {
            PaymentIntent paymentIntent = PaymentIntent.create(params, requestOptions);
            if (paymentIntent.getId() == null || paymentIntent.getClientSecret() == null) {
                throw new PaymentProviderException("Stripe payment intent response was incomplete");
            }
            return new StripePaymentIntent(paymentIntent.getId(), paymentIntent.getClientSecret());
        } catch (StripeException ex) {
            throw new PaymentProviderException("Stripe payment intent creation failed", ex);
        }
    }

    @Override
    public String retrievePaymentIntentStatus(String paymentIntentId) {
        if (stripeProperties.getSecretKey() == null || stripeProperties.getSecretKey().isBlank()) {
            throw new PaymentProviderException("Stripe secret key is not configured");
        }

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(
                    paymentIntentId,
                    RequestOptions.builder().setApiKey(stripeProperties.getSecretKey()).build());
            return paymentIntent.getStatus();
        } catch (StripeException ex) {
            throw new PaymentProviderException("Stripe payment status lookup failed", ex);
        }
    }
}
