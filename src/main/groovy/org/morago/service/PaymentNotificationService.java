package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.event.SocketNotificationEvent;
import org.morago.model.Transaction;
import org.morago.model.WithdrawalRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentNotificationService {

    private final ApplicationEventPublisher eventPublisher;

    public void notifyTransactionCharged(Transaction transaction){
        Long clientId = transaction.getUser().getId();
        eventPublisher.publishEvent(
                SocketNotificationEvent.of(String.valueOf(clientId), "transaction:charged", transaction.getAmount()));
    }

    public void notifyTransactionEarned(Transaction transaction){
        Long translatorId = transaction.getUser().getId();
        eventPublisher.publishEvent(
                SocketNotificationEvent.of(String.valueOf(translatorId), "transaction:earned", transaction.getAmount()));
    }

    public void notifyTopUp(Transaction transaction){
        Long currentUserId = transaction.getUser().getId();
        eventPublisher.publishEvent(
                SocketNotificationEvent.of(String.valueOf(currentUserId), "transaction:topped-up", transaction.getAmount()));
    }

    public void notifyWithdrawalCreated(WithdrawalRequest request){
        Long translatorId = request.getTranslator().getId();
        eventPublisher.publishEvent(
                SocketNotificationEvent.of(String.valueOf(translatorId), "withdrawal:created",
                        request.getId(), request.getStatus(), request.getAmount()));
    }

    public void notifyWithdrawalApproved(WithdrawalRequest request){
        Long translatorId = request.getTranslator().getId();
        eventPublisher.publishEvent(
                SocketNotificationEvent.of(String.valueOf(translatorId), "withdrawal:approved",
                        request.getId(), request.getStatus(), request.getAmount()));
    }

    public void notifyWithdrawalRejected(WithdrawalRequest request){
        Long translatorId = request.getTranslator().getId();
        eventPublisher.publishEvent(
                SocketNotificationEvent.of(String.valueOf(translatorId), "withdrawal:rejected",
                        request.getId(), request.getStatus(), request.getAmount()));
    }

    public void notifyWithdrawalPaid(WithdrawalRequest request){
        Long translatorId = request.getTranslator().getId();
        eventPublisher.publishEvent(
                SocketNotificationEvent.of(String.valueOf(translatorId), "withdrawal:paid",
                        request.getId(), request.getStatus(), request.getAmount()));
    }
}