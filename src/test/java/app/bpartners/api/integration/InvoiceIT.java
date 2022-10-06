package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.service.InvoiceService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.CustomerTemplateIT.customer1;
import static app.bpartners.api.integration.CustomerTemplateIT.customer2;
import static app.bpartners.api.integration.ProductIT.product3;
import static app.bpartners.api.integration.ProductIT.product4;
import static app.bpartners.api.integration.ProductIT.product5;
import static app.bpartners.api.integration.conf.TestUtils.FILE_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE2_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE_FILE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = InvoiceIT.ContextInitializer.class)
@AutoConfigureMockMvc
class InvoiceIT {
  @MockBean
  private SentryConf sentryConf;
  @Value("${test.user.access.token}")
  private String bearerToken;

  @Autowired
  private InvoiceService invoiceService;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, InvoiceIT.ContextInitializer.SERVER_PORT);
  }

  private static final String NEW_INVOICE_ID = "invoice_uuid";

  CrupdateInvoice validInvoice() {
    return new CrupdateInvoice()
        .ref("BP003")
        .title("Facture sans produit")
        .customer(customer1())
        .products(List.of())
        .sendingDate(LocalDate.of(2022, 9, 10))
        .toPayAt(LocalDate.of(2022, 9, 11));
  }

  Invoice invoice1() {
    return new Invoice()
        .id(INVOICE1_ID)
        .fileId(INVOICE_FILE_ID)
        .title("Facture tableau")
        .customer(customer1())
        .ref("BP001")
        .sendingDate(LocalDate.of(2022, 9, 1))
        .toPayAt(LocalDate.of(2022, 10, 1))
        .status(InvoiceStatus.CONFIRMED)
        .products(List.of(product3(), product4()))
        .totalPriceWithVat(8800)
        .totalVat(800)
        .totalPriceWithoutVat(8000)
        ;
  }

  Invoice invoice2() {
    return new Invoice()
        .id(INVOICE2_ID)
        .title("Facture plomberie")
        .customer(customer2().address("Nouvelle adresse"))
        .ref("BP002")
        .sendingDate(LocalDate.of(2022, 9, 10))
        .toPayAt(LocalDate.of(2022, 10, 10))
        .status(InvoiceStatus.CONFIRMED)
        .products(List.of(product5()))
        .totalPriceWithVat(1100)
        .totalVat(100)
        .totalPriceWithoutVat(1000);
  }

  Invoice createdInvoice() {
    return new Invoice()
        .id(NEW_INVOICE_ID)
        .ref(validInvoice().getRef())
        .title("Facture sans produit")
        .customer(validInvoice().getCustomer())
        .status(InvoiceStatus.CONFIRMED)
        .sendingDate(validInvoice().getSendingDate())
        .products(List.of())
        .toPayAt(validInvoice().getToPayAt())
        .totalPriceWithVat(0)
        .totalVat(0)
        .totalPriceWithoutVat(0);
  }

  @Test
  void read_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    PayingApi api = new PayingApi(joeDoeClient);

    Invoice actual1 = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, INVOICE1_ID);
    Invoice actual2 = api.getInvoiceById(JOE_DOE_ACCOUNT_ID, INVOICE2_ID);

    assertEquals(invoice1(), actual1.paymentUrl(null));
    assertEquals(invoice2(), actual2.paymentUrl(null));
  }

  @Test
  void crupdate_invoice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    PayingApi api = new PayingApi(joeDoeClient);

    Invoice actual = api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID, validInvoice());

    assertEquals(createdInvoice(), actual.paymentUrl(null));
  }

  @Test
  void generate_invoice_pdf_ok() throws IOException {
    byte[] data = invoiceService.generateInvoicePdf(INVOICE1_ID);
    File generatedFile = new File("test.pdf");
    OutputStream os = new FileOutputStream(generatedFile);
    os.write(data);
    os.close();
  }

  @Test
  void download_invoice_file_ok() throws IOException, InterruptedException {
    String basePath = "http://localhost:" + FileIT.ContextInitializer.SERVER_PORT;
    Resource logoFileResource = new ClassPathResource(
        "files/invoice_downloaded.jpeg");

    HttpResponse<byte[]> response = download(basePath, INVOICE_FILE_ID);

    assertEquals(HttpStatus.OK.value(), response.statusCode());
    assertEquals(logoFileResource.getInputStream().readAllBytes().length, response.body().length);
  }

  public HttpResponse<byte[]> download(String basePath, String fileId)
      throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    return unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(
                basePath + "/accounts/" + JOE_DOE_ACCOUNT_ID + "/files/" + fileId + "/raw"))
            .header("Access-Control-Request-Method", "GET")
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofByteArray());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
