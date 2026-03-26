package com.mirror.product.dto;

import com.mirror.product.enums.PaymentTermType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderPaymentTermsUpdateRequest {

    @NotNull
    private PaymentTermType paymentTermsType;

    private String paymentTerms;

    @Valid
    private List<OrderPaymentScheduleRequest> paymentSchedule;
}
