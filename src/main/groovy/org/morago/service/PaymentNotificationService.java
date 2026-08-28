package org.morago.service;


import com.corundumstudio.socketio.SocketIOServer;
import org.morago.model.Transaction;
import org.morago.model.WithdrawalRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class PaymentNotificationService {
    private final SocketIOServer socketIOServer;

    public PaymentNotificationService(@Lazy SocketIOServer socketIOServer) {
        this.socketIOServer = socketIOServer;
    }

    public void notifyTransactionCharged(Transaction transaction){
        Long clientId = transaction.getUser().getId();
        socketIOServer.getRoomOperations(String.valueOf(clientId))
                .sendEvent("transaction:charged", transaction.getAmount());
    }

    public void notifyTransactionEarned(Transaction transaction){
        Long translatorId = transaction.getUser().getId();
        socketIOServer.getRoomOperations(String.valueOf(translatorId))
                .sendEvent("transaction:earned", transaction.getAmount());
    }
    public void notifyTopUp(Transaction transaction){
        Long currentUserId = transaction.getUser().getId();
        socketIOServer.getRoomOperations(String.valueOf(currentUserId))
                .sendEvent("transaction:topped-up", transaction.getAmount());
    }

    public void notifyWithdrawalCreated(WithdrawalRequest request){
        Long translatorId = request.getTranslator().getId();
        socketIOServer.getRoomOperations(String.valueOf(translatorId))
                .sendEvent("withdrawal:created", request.getId(), request.getStatus(), request.getAmount());
    }
    public void notifyWithdrawalApproved(WithdrawalRequest request){
        Long translatorId = request.getTranslator().getId();
        socketIOServer.getRoomOperations(String.valueOf(translatorId))
                .sendEvent("withdrawal:approved", request.getId(), request.getStatus(), request.getAmount());
    }
    public void notifyWithdrawalRejected(WithdrawalRequest request){
        Long translatorId = request.getTranslator().getId();
        socketIOServer.getRoomOperations(String.valueOf(translatorId))
                .sendEvent("withdrawal:rejected", request.getId(), request.getStatus(), request.getAmount());
    }
    public void notifyWithdrawalPaid(WithdrawalRequest request){
        Long translatorId = request.getTranslator().getId();
        socketIOServer.getRoomOperations(String.valueOf(translatorId))
                .sendEvent("withdrawal:paid", request.getId(), request.getStatus(), request.getAmount());
    }
}
