package com.mirror.product.dto.pod;

import com.mirror.product.enums.CommissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionSearchCriteria {

    private String partnerId;
    private CommissionStatus status;
    private LocalDate periodStartAfter;
    private LocalDate periodStartBefore;
    private LocalDate periodEndAfter;
    private LocalDate periodEndBefore;
    private BigDecimal minFinalAmount;
    private BigDecimal maxFinalAmount;
    private Boolean isPaid;
}
