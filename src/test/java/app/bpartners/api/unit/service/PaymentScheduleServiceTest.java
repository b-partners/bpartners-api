package app.bpartners.api.unit.service;

import app.bpartners.api.repository.fintecture.FintecturePaymentInfoRepository;
import app.bpartners.api.repository.fintecture.model.Session;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import app.bpartners.api.service.payment.PaymentScheduleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static app.bpartners.api.endpoint.rest.model.PaymentStatus.PAID;
import static app.bpartners.api.service.payment.PaymentScheduleService.paymentMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class PaymentScheduleServiceTest {
     FintecturePaymentInfoRepository infoRepositoryMock = mock();
     PaymentRequestJpaRepository jpaRepositoryMock = mock();
    PaymentScheduleService subject  = new PaymentScheduleService(infoRepositoryMock, jpaRepositoryMock);

    @Test
    void update_payment_status(){
        var sessionId = "sessionId";
        var unpaidPayments = HPaymentRequest.builder().sessionId(sessionId).build();
        when(jpaRepositoryMock.findAllByStatus(any())).thenReturn(List.of(unpaidPayments));
        var meta = Session.Meta.builder().sessionId(sessionId).status("payment_created").build();
        var externalPayment = Session.builder().meta(meta).build();
        when(infoRepositoryMock.getAllPaymentsByStatus(any())).thenReturn(List.of(externalPayment));
        when(jpaRepositoryMock.saveAll(anyList())).thenReturn(List.of(unpaidPayments));

        subject.updatePaymentStatus();

        verify(jpaRepositoryMock, times(1)).findAllByStatus(any());
        verify(infoRepositoryMock, times(1)).getAllPaymentsByStatus(any());
        verify(jpaRepositoryMock, times(1)).saveAll(anyList());
    }

    @Test
    void payment_message(){
        var paymentRequest = HPaymentRequest.builder().id("paymentId").sessionId("sessionId").status(PAID).build();

        var actual = paymentMessage(List.of(paymentRequest));

        var expected = "(id=paymentId, sessionId=sessionId, status=PAID) ";
        assertEquals(expected, actual);
    }
}
