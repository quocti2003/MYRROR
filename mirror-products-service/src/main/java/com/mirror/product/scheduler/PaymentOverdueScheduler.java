package com.mirror.product.scheduler;

import com.mirror.product.entity.Order;
import com.mirror.product.entity.OrderPaymentSchedule;
import com.mirror.product.enums.PaymentScheduleStatus;
import com.mirror.product.repository.OrderPaymentScheduleRepository;
import com.mirror.product.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentOverdueScheduler {

    private final OrderPaymentScheduleRepository scheduleRepository;
    private final OrderRepository orderRepository;

    @Scheduled(cron = "${mirror.payment.overdue-check.cron:0 0 1 * * ?}")
    @Transactional
    public void checkOverduePayments() {
        log.info("Starting overdue payment check...");
        Instant now = Instant.now();

        List<String> overdueOrderIds = scheduleRepository.findOrderIdsWithOverdueSchedules(now);

        if (overdueOrderIds.isEmpty()) {
            log.info("No overdue payment schedules found.");
            return;
        }

        log.info("Found {} orders with potentially overdue schedules", overdueOrderIds.size());
        int transitionsCount = 0;

        for (String orderId : overdueOrderIds) {
            try {
                Order order = orderRepository.findById(orderId).orElse(null);
                if (order == null) continue;

                for (OrderPaymentSchedule schedule : order.getPaymentSchedule()) {
                    if (schedule.getDueDate() != null
                            && schedule.getDueDate().isBefore(now)
                            && schedule.getAmountPaid().compareTo(schedule.getAmountDue()) < 0
                            && schedule.getStatus() != PaymentScheduleStatus.PAID
                            && schedule.getStatus() != PaymentScheduleStatus.OVERDUE) {

                        PaymentScheduleStatus previousStatus = schedule.getStatus();
                        schedule.setStatus(PaymentScheduleStatus.OVERDUE);
                        transitionsCount++;
                        log.info("Schedule {} for order {} transitioned from {} to OVERDUE (due: {}, paid: {}/{})",
                                schedule.getId(), orderId, previousStatus,
                                schedule.getDueDate(), schedule.getAmountPaid(), schedule.getAmountDue());
                    }
                }

                orderRepository.save(order);
            } catch (Exception e) {
                log.error("Error processing overdue check for order {}: {}", orderId, e.getMessage());
            }
        }

        log.info("Overdue payment check completed. {} schedule entries transitioned to OVERDUE.", transitionsCount);
    }
}
