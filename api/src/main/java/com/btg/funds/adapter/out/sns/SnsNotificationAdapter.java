package com.btg.funds.adapter.out.sns;

import com.btg.funds.domain.model.Client;
import com.btg.funds.domain.model.Fund;
import com.btg.funds.domain.model.enums.NotificationPreference;
import com.btg.funds.domain.model.enums.TransactionType;
import com.btg.funds.domain.port.out.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

public class SnsNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(SnsNotificationAdapter.class);

    private final SnsClient snsClient;
    private final String topicArn;

    public SnsNotificationAdapter(SnsClient snsClient, String topicArn) {
        this.snsClient = snsClient;
        this.topicArn = topicArn;
    }

    @Override
    public void notify(Client client, Fund fund, TransactionType type) {
        var message = buildMessage(client, fund, type);
        var subject = type == TransactionType.APERTURA
                ? "Suscripción exitosa - " + fund.name()
                : "Cancelación exitosa - " + fund.name();

        var request = PublishRequest.builder()
                .topicArn(topicArn)
                .subject(subject)
                .message(message)
                .messageAttributes(java.util.Map.of(
                        "channel", software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(client.notificationPreference().name())
                                .build(),
                        "clientId", software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(client.id())
                                .build()
                ))
                .build();

        snsClient.publish(request);
        log.info("Notificación enviada vía {} al cliente {} para fondo {}",
                client.notificationPreference(), client.id(), fund.name());
    }

    private String buildMessage(Client client, Fund fund, TransactionType type) {
        var destination = client.notificationPreference() == NotificationPreference.EMAIL
                ? client.email()
                : client.phone();

        return String.format(
                """
                Estimado(a) %s,

                Su %s al fondo %s ha sido procesada exitosamente.
                Monto: COP $%,.0f
                Canal de notificación: %s (%s)
                """,
                client.name(),
                type == TransactionType.APERTURA ? "suscripción" : "cancelación",
                fund.name(),
                fund.minimumAmount(),
                client.notificationPreference(),
                destination
        );
    }
}
