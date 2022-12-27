package app.bpartners.api.unit.service;

import app.bpartners.api.endpoint.event.model.gen.InvoiceCrupdated;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceCustomer;
import app.bpartners.api.model.Product;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.InvoiceCrupdatedService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.integration.conf.TestUtils.FILE_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoiceCrupdatedServiceTest {
  public static final byte[] logoAsBytes = new byte[0];
  FileService fileService;
  InvoiceJpaRepository invoiceJpaRepository;
  InvoiceCrupdatedService invoiceCrupdatedService;


  @BeforeEach
  void setUp() {
    fileService = mock(FileService.class);
    invoiceJpaRepository = mock(InvoiceJpaRepository.class);
    invoiceCrupdatedService = new InvoiceCrupdatedService(fileService, invoiceJpaRepository);

    when(fileService.downloadFile(any(), any(), any())).thenReturn(logoAsBytes);
    when(fileService.upload(any(), any(), any(), any(), any())).thenReturn(fileInfo());
  }

  InvoiceCrupdated invoiceCrupdated() {
    return InvoiceCrupdated.builder()
        .invoice(Invoice.builder()
            .id(INVOICE1_ID)
            .ref("invoice_ref")
            .title("invoice_title")
            .sendingDate(LocalDate.now())
            .toPayAt(LocalDate.now())
            .updatedAt(Instant.now())
            .fileId(FILE_ID)
            .status(InvoiceStatus.CONFIRMED)
            .comment("comment")
            .paymentUrl("payment_url")
            .totalPriceWithVat(new Fraction())
            .totalPriceWithoutVat(new Fraction())
            .totalVat(new Fraction())
            .account(Account.builder()
                .id(JOE_DOE_ACCOUNT_ID)
                .iban("iban")
                .bic("bic")
                .name("account_name")
                .build())
            .products(List.of(Product.builder()
                .id("product_id")
                .quantity(50)
                .description("product description")
                .vatPercent(new Fraction())
                .unitPrice(new Fraction())
                .build()))
            .invoiceCustomer(InvoiceCustomer.customerTemplateBuilder()
                .name("Olivier Durant")
                .phone("+33 6 12 45 89 76")
                .email("exemple@email.com")
                .address("Paris 745")
                .build())
            .build())
        .accountHolder(AccountHolder.builder()
            .id("b33e6eb0-e262-4596-a91f-20c6a7bfd343")
            .name("NUMER")
            .mainActivity("businessAndRetail")
            .mainActivityDescription("Phrase détaillée de mon activité")
            .mobilePhoneNumber("899067250")
            .address("6 RUE PAUL LANGEVIN")
            .accountId("account1_id")
            .tvaNumber("20")
            .siren("siren")
            .socialCapital(400000)
            .email("exemple@email.com")
            .city("FONTENAY-SOUS-BOIS")
            .country("FRA")
            .postalCode("94120")
            .build())
        .logoFileId("logo.pdf")
        .build();
  }

  @SneakyThrows
  HInvoice invoiceEntity() {
    return HInvoice.builder()
        .id(invoiceCrupdated().getInvoice().getId())
        .fileId(fileInfo().getId())
        .comment(invoiceCrupdated().getInvoice().getComment())
        .ref(invoiceCrupdated().getInvoice().getRealReference())
        .title(invoiceCrupdated().getInvoice().getTitle())
        .idAccount(invoiceCrupdated().getInvoice().getAccount().getId())
        .sendingDate(invoiceCrupdated().getInvoice().getSendingDate())
        .toPayAt(invoiceCrupdated().getInvoice().getToPayAt())
        .status(invoiceCrupdated().getInvoice().getStatus())
        .metadataString(
            new ObjectMapper().writeValueAsString(invoiceCrupdated().getInvoice().getMetadata()))
        .build();
  }

  @Test
  void accept_crupdated_invoice_ok() {
    ArgumentCaptor<String> fileIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<FileType> fileTypeArgumentCaptor = ArgumentCaptor.forClass(FileType.class);
    ArgumentCaptor<String> accountIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<HInvoice> invoiceArgumentCaptor = ArgumentCaptor.forClass(HInvoice.class);

    invoiceCrupdatedService.accept(invoiceCrupdated());
    verify(invoiceJpaRepository).save(invoiceArgumentCaptor.capture());
    verify(fileService).upload(fileIdCaptor.capture(), fileTypeArgumentCaptor.capture(),
        accountIdCaptor.capture(), any(), any());

    assertEquals(invoiceFileId(), fileIdCaptor.getValue());
    assertEquals(INVOICE, fileTypeArgumentCaptor.getValue());
    assertEquals(invoiceAccountId(), accountIdCaptor.getValue());
    assertEquals(invoiceEntity(), invoiceArgumentCaptor.getValue());
  }

  private String invoiceAccountId() {
    return invoiceCrupdated().getInvoice().getAccount().getId();
  }

  private String invoiceFileId() {
    return invoiceCrupdated().getInvoice().getFileId();
  }

  FileInfo fileInfo() {
    return FileInfo.builder()
        .id(FILE_ID)
        .sha256(null)
        .sizeInKb(0)
        .build();
  }
}
