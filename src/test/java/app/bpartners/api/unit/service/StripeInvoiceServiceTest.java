package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import app.bpartners.api.service.subscription.StripeInvoiceService;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceCollection;
import com.stripe.param.InvoiceListParams;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class StripeInvoiceServiceTest {
  StripeInvoiceService subject = new StripeInvoiceService();

  @Test
  void get_unpaid_stripe_invoices_without_subscription_id_only_filters_by_customer() {
    var openInvoice = mock(Invoice.class);
    var uncollectibleInvoice = mock(Invoice.class);
    var openCollection = mock(InvoiceCollection.class);
    var uncollectibleCollection = mock(InvoiceCollection.class);
    when(openCollection.getData()).thenReturn(List.of(openInvoice));
    when(uncollectibleCollection.getData()).thenReturn(List.of(uncollectibleInvoice));
    ArgumentCaptor<InvoiceListParams> paramsCaptor = ArgumentCaptor.forClass(InvoiceListParams.class);

    try (MockedStatic<Invoice> invoiceMockedStatic = mockStatic(Invoice.class)) {
      invoiceMockedStatic
          .when(() -> Invoice.list(paramsCaptor.capture()))
          .thenReturn(openCollection, uncollectibleCollection);

      var actual = subject.getUnpaidStripeInvoices("cus_1");

      assertEquals(List.of(openInvoice, uncollectibleInvoice), actual);
      var capturedParams = paramsCaptor.getAllValues();
      assertEquals("cus_1", capturedParams.get(0).getCustomer());
      assertEquals(InvoiceListParams.Status.OPEN, capturedParams.get(0).getStatus());
      assertNull(capturedParams.get(0).getSubscription());
      assertEquals("cus_1", capturedParams.get(1).getCustomer());
      assertEquals(InvoiceListParams.Status.UNCOLLECTIBLE, capturedParams.get(1).getStatus());
      assertNull(capturedParams.get(1).getSubscription());
    }
  }

  @Test
  void get_unpaid_stripe_invoices_with_subscription_id_scopes_to_that_subscription() {
    var openCollection = mock(InvoiceCollection.class);
    var uncollectibleCollection = mock(InvoiceCollection.class);
    when(openCollection.getData()).thenReturn(List.of());
    when(uncollectibleCollection.getData()).thenReturn(List.of());
    ArgumentCaptor<InvoiceListParams> paramsCaptor = ArgumentCaptor.forClass(InvoiceListParams.class);

    try (MockedStatic<Invoice> invoiceMockedStatic = mockStatic(Invoice.class)) {
      invoiceMockedStatic
          .when(() -> Invoice.list(paramsCaptor.capture()))
          .thenReturn(openCollection, uncollectibleCollection);

      var actual = subject.getUnpaidStripeInvoices("cus_1", "sub_1");

      assertEquals(List.of(), actual);
      var capturedParams = paramsCaptor.getAllValues();
      assertEquals("sub_1", capturedParams.get(0).getSubscription());
      assertEquals("sub_1", capturedParams.get(1).getSubscription());
    }
  }
}
