package app.bpartners.api.unit.service;

import app.bpartners.api.endpoint.event.EventConf;
import app.bpartners.api.endpoint.event.model.gen.InvoiceRelaunchSaved;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceDiscount;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.User;
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
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
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
  EventConf eventConfMock;

  @BeforeEach
  void setUp() throws MessagingException, IOException {
    sesService = mock(SesService.class);
    fileService = mock(FileService.class);
    eventConfMock = mock(EventConf.class);
    invoiceRelaunchSavedService =
        new InvoiceRelaunchSavedService(sesService, fileService, eventConfMock);

    doNothing().when(sesService).sendEmail(any(), any(), any(), any(), any());
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

    verify(sesService, times(1)).sendEmail(eq(recipient), any(), eq(subject), eq(htmlBody), any());
  }

  Invoice invoice() {
    Account account = Account.builder()
        .id("account")
        .build();
    User user = User.builder()
        .id(JOE_DOE_ID)
        .accounts(List.of(account))
        .build();
    return Invoice.builder()
        .status(PROPOSAL)
        .sendingDate(LocalDate.now())
        .toPayAt(LocalDate.now())
        .user(user)
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
        .totalPriceWithVat(new Fraction())
        .totalVat(new Fraction())
        .totalPriceWithoutDiscount(new Fraction())
        .totalPriceWithoutVat(new Fraction())
        .customer(Customer.builder()
            .firstName("Olivier")
            .lastName("Durant")
            .phone("+33 6 12 45 89 76")
            .email("exemple@email.com")
            .address("Paris 745")
            .build())
        .discount(InvoiceDiscount.builder()
            .percentValue(new Fraction())
            .amountValue(null)
            .build())
        .build();
  }
}
