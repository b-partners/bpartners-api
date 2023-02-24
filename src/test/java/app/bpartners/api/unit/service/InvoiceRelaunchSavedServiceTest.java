package app.bpartners.api.unit.service;

import app.bpartners.api.endpoint.event.model.gen.InvoiceRelaunchSaved;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceDiscount;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.InvoiceRelaunchSavedService;
import app.bpartners.api.service.aws.SesService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import javax.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoiceRelaunchSavedServiceTest {
  public static final byte[] logoAsBytes = new byte[0];
  InvoiceRelaunchSavedService invoiceRelaunchSavedService;
  SesService sesService;
  FileService fileService;

  @BeforeEach
  void setUp() throws MessagingException, IOException {
    sesService = mock(SesService.class);
    fileService = mock(FileService.class);
    invoiceRelaunchSavedService =
        new InvoiceRelaunchSavedService(sesService, fileService);

    doNothing().when(sesService).sendEmail(any(), any(), any(), any());
    when(fileService.downloadOptionalFile(any(), any(), any())).thenReturn(List.of(logoAsBytes));
  }

  @Test
  void sendEmail_triggers() throws MessagingException, IOException {
    String recipient = "test" + randomUUID() + "@bpartners.app";
    String subject = "Objet du mail";
    String htmlBody = "<html><body>Corps du mail</body></html>";

    invoiceRelaunchSavedService.accept(InvoiceRelaunchSaved.builder()
        .recipient(recipient)
        .subject(subject)
        .htmlBody(htmlBody)
        .attachmentName(null)
        .invoice(invoice())
        .logoFileId(null)
        .accountHolder(new AccountHolder())
        .attachments(List.of())
        .build());

    verify(sesService, times(1)).sendEmail(eq(recipient), eq(subject), eq(htmlBody), any());
  }

  Invoice invoice() {
    return Invoice.builder()
        .status(PROPOSAL)
        .sendingDate(LocalDate.now())
        .toPayAt(LocalDate.now())
        .account(Account.builder()
            .id("account")
            .build())
        .products(List.of(InvoiceProduct.builder()
            .id("product_id")
            .quantity(50)
            .description("product description")
            .vatPercent(new Fraction())
            .unitPrice(new Fraction())
            .priceNoVatWithDiscount(new Fraction())
            .vatWithDiscount(new Fraction())
            .totalWithDiscount(new Fraction())
            .build()))
        .customer(Customer.builder()
            .firstName("Olivier")
            .lastName("Durant")
            .phone("+33 6 12 45 89 76")
            .email("exemple@email.com")
            .address("Paris 745")
            .build())
        .totalVat(new Fraction())
        .totalPriceWithVat(new Fraction())
        .totalPriceWithoutVat(new Fraction())
        .totalPriceWithoutDiscount(new Fraction())
        .discount(InvoiceDiscount.builder()
            .percentValue(new Fraction())
            .amountValue(null)
            .build())
        .build();
  }
}
