package app.bpartners.api.unit.mapper;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.mapper.InvoiceRestMapper;
import app.bpartners.api.endpoint.rest.mapper.SubscriptionInvoiceRestMapper;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.service.aws.S3Service;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionInvoiceRestMapperTest {
  private static final String USER_TO_CREDIT_ID = "user_to_credit_id";
  private static final String FILE_ID = "file_id";
  private static final String PRESIGNED_URL = "https://s3.presigned/invoice";
  private static final long ONE_HOUR_IN_SECONDS = 3600L;

  InvoiceRestMapper invoiceRestMapperMock = mock();
  S3Service s3ServiceMock = mock();
  UserSubscriptionConf userSubscriptionConfMock = mock();

  SubscriptionInvoiceRestMapper subject =
      new SubscriptionInvoiceRestMapper(
          invoiceRestMapperMock, s3ServiceMock, userSubscriptionConfMock);

  @BeforeEach
  void setUp() {
    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(USER_TO_CREDIT_ID);
    when(s3ServiceMock.presignURL(any(), any(), any(), any())).thenReturn(PRESIGNED_URL);
  }

  @Test
  void map_domain_to_rest_ok() {
    var restInvoice = new Invoice().id("invoice_id");
    var domain = domainInvoice();
    when(invoiceRestMapperMock.toRest(domain)).thenReturn(restInvoice);

    var actual = subject.toRest(domain);

    assertEquals(restInvoice, actual.getInvoice());
    assertEquals(PRESIGNED_URL, actual.getFileUrl().getValue());
    assertEquals((int) ONE_HOUR_IN_SECONDS, actual.getFileUrl().getExpirationDelay());
  }

  @Test
  void stamp_url_freshness_at_mapping_time_ok() {
    var domain = domainInvoice();
    when(invoiceRestMapperMock.toRest(domain)).thenReturn(new Invoice());
    var before = Instant.now();

    var actual = subject.toRest(domain);

    // updatedAt pairs with expirationDelay to let the client know when the URL goes stale,
    // so it must be the mapping instant rather than any date carried by the invoice
    var updatedAt = actual.getFileUrl().getUpdatedAt();
    assertNotNull(updatedAt);
    assertFalse(updatedAt.isBefore(before));
    assertFalse(updatedAt.isAfter(Instant.now()));
  }

  @Test
  void presign_the_invoice_file_under_the_user_to_credit_ok() {
    var domain = domainInvoice();
    when(invoiceRestMapperMock.toRest(domain)).thenReturn(new Invoice());

    subject.toRest(domain);

    // subscription invoices are owned by the crediting account, not by the debited user,
    // so the bucket key must be resolved under that account for the URL to resolve
    verify(s3ServiceMock)
        .presignURL(eq(INVOICE), eq(FILE_ID), eq(USER_TO_CREDIT_ID), eq(ONE_HOUR_IN_SECONDS));
  }

  @Test
  void map_invoice_without_file_ok() {
    var domain = domainInvoice().toBuilder().fileId(null).build();
    when(invoiceRestMapperMock.toRest(domain)).thenReturn(new Invoice());

    var actual = subject.toRest(domain);

    assertNotNull(actual.getFileUrl());
    verify(s3ServiceMock).presignURL(eq(INVOICE), isNull(), eq(USER_TO_CREDIT_ID), any());
  }

  private static app.bpartners.api.model.Invoice domainInvoice() {
    return app.bpartners.api.model.Invoice.builder().id("invoice_id").fileId(FILE_ID).build();
  }
}
