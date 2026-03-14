package com.btg.funds.adapter.out.sns;

import com.btg.funds.domain.model.Client;
import com.btg.funds.domain.model.Fund;
import com.btg.funds.domain.model.enums.NotificationPreference;
import com.btg.funds.domain.model.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnsNotificationAdapterTest {

    @Mock
    private SnsClient snsClient;

    private SnsNotificationAdapter adapter;

    private static final String TOPIC_ARN = "arn:aws:sns:us-east-1:000000000000:test-topic";

    private static final Client EMAIL_CLIENT = new Client(
            "client-001", "Juan Pérez", "juan@email.com", "+573001234567",
            new BigDecimal("500000"), NotificationPreference.EMAIL
    );

    private static final Client SMS_CLIENT = new Client(
            "client-002", "María López", "maria@email.com", "+573009876543",
            new BigDecimal("300000"), NotificationPreference.SMS
    );

    private static final Fund FUND = new Fund("1", "FPV_BTG", new BigDecimal("75000"), "FPV");

    @BeforeEach
    void setUp() {
        adapter = new SnsNotificationAdapter(snsClient, TOPIC_ARN);
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("msg-001").build());
    }

    @Test
    void notify_shouldPublishToCorrectTopic() {
        adapter.notify(EMAIL_CLIENT, FUND, TransactionType.APERTURA);

        var captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        assertThat(captor.getValue().topicArn()).isEqualTo(TOPIC_ARN);
    }

    @Test
    void notify_shouldSetSubjectForApertura() {
        adapter.notify(EMAIL_CLIENT, FUND, TransactionType.APERTURA);

        var captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        assertThat(captor.getValue().subject()).contains("Suscripción exitosa");
        assertThat(captor.getValue().subject()).contains("FPV_BTG");
    }

    @Test
    void notify_shouldSetSubjectForCancelacion() {
        adapter.notify(EMAIL_CLIENT, FUND, TransactionType.CANCELACION);

        var captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        assertThat(captor.getValue().subject()).contains("Cancelación exitosa");
    }

    @Test
    void notify_shouldIncludeChannelAttribute() {
        adapter.notify(EMAIL_CLIENT, FUND, TransactionType.APERTURA);

        var captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());

        var attributes = captor.getValue().messageAttributes();
        assertThat(attributes).containsKey("channel");
        assertThat(attributes.get("channel").stringValue()).isEqualTo("EMAIL");
    }

    @Test
    void notify_shouldIncludeEmailInMessage_whenPreferenceIsEmail() {
        adapter.notify(EMAIL_CLIENT, FUND, TransactionType.APERTURA);

        var captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        assertThat(captor.getValue().message()).contains("juan@email.com");
    }

    @Test
    void notify_shouldIncludePhoneInMessage_whenPreferenceIsSms() {
        adapter.notify(SMS_CLIENT, FUND, TransactionType.APERTURA);

        var captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        assertThat(captor.getValue().message()).contains("+573009876543");
    }

    @Test
    void notify_shouldIncludeClientIdAttribute() {
        adapter.notify(EMAIL_CLIENT, FUND, TransactionType.APERTURA);

        var captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());

        var attributes = captor.getValue().messageAttributes();
        assertThat(attributes).containsKey("clientId");
        assertThat(attributes.get("clientId").stringValue()).isEqualTo("client-001");
    }
}
