package app.bpartners.api.integration.conf;

import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.BusinessActivity;
import app.bpartners.api.endpoint.rest.model.CompanyBusinessActivity;
import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.model.LegalFile;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.fintecture.model.PaymentInitiation;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;
import app.bpartners.api.repository.sendinblue.SendinblueApi;
import app.bpartners.api.repository.sendinblue.model.Attributes;
import app.bpartners.api.repository.sendinblue.model.Contact;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.OnboardingSwanRepository;
import app.bpartners.api.repository.swan.TransactionSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.model.AccountHolder;
import app.bpartners.api.repository.swan.model.SwanAccount;
import app.bpartners.api.repository.swan.model.SwanUser;
import app.bpartners.api.repository.swan.model.Transaction;
import app.bpartners.api.service.aws.SesService;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.mail.MessagingException;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.CLIENT_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class TestUtils {
  public static final String BAD_ACCOUNT_HOLDER_ID = "bad_account_holder_id";
  public static final String JOE_DOE_ID = "joe_doe_id";
  public static final String JOE_DOE_SWAN_USER_ID = "c15924bf-61f9-4381-8c9b-d34369bf91f7";
  public static final String BAD_TOKEN = "bad_token";
  public static final String VALID_EMAIL = "username@domain.com";
  public static final String REDIRECT_SUCCESS_URL =
      "https://dashboard-dev.bpartners.app/login/success";
  public static final String REDIRECT_FAILURE_URL =
      "https://dashboard-dev.bpartners.app/login/failure";
  public static final String JOE_DOE_ACCOUNT_ID = "beed1765-5c16-472a-b3f4-5c376ce5db58";
  public static final String OTHER_ACCOUNT_ID = "other_account_id";
  public static final String USER1_ID = "user1_id";
  public static final String BAD_USER_ID = "bad_user_id";
  public static final String INVOICE1_ID = "invoice1_id";
  public static final String INVOICE2_ID = "invoice2_id";
  public static final String INVOICE3_ID = "invoice3_id";
  public static final String INVOICE4_ID = "invoice4_id";
  public static final String INVOICE7_ID = "invoice7_id";

  public static final String INVOICE1_FILE_ID = "BP001.pdf";
  public static final String FILE_ID = "test.jpeg";
  public static final String TEST_FILE_ID = "test.jpeg";
  public static final String TO_UPLOAD_FILE_ID = "to_upload_file_id.jpeg";
  public static final String MARKETPLACE1_ID = "marketplace1_id";
  public static final String MARKETPLACE2_ID = "marketplace2_id";
  public static final String BEARER_QUERY_PARAMETER_NAME = "accessToken";
  public static final String BEARER_PREFIX = "Bearer ";
  public static final String JOE_DOE_TOKEN = "joe_doe_token";
  public static final String BAD_CODE = "bad_code";
  public static final String SWAN_ONBOARDING_URL_FORMAT =
      "https://api.banking.sandbox.swan.io/projects/uuid/onboardings/uuid";
  public static final String INVOICE_RELAUNCH1_ID = "invoice_relaunch1_id";
  public static final String INVOICE_RELAUNCH2_ID = "invoice_relaunch2_id";
  public static final String INVALID_LOGO_TYPE = "invalid_logo_type";

  public static final String NOT_JOE_DOE_ACCOUNT_ID = "NOT_" + JOE_DOE_ACCOUNT_ID;

  public static User restJoeDoeUser() {
    return new User()
        .id(TestUtils.JOE_DOE_ID)
        .firstName(joeDoe().getFirstName())
        .lastName(joeDoe().getLastName())
        .phone(joeDoe().getMobilePhoneNumber())
        .monthlySubscriptionAmount(5)
        .status(ENABLED)
        .logoFileId("logo.jpeg");
  }

  public static SwanUser joeDoe() {
    return SwanUser.builder()
        .id(JOE_DOE_SWAN_USER_ID)
        .firstName("Joe")
        .lastName("Doe")
        .birthDate(LocalDate.of(2022, 8, 9))
        .idVerified(true)
        .identificationStatus("ValidIdentity")
        .nationalityCca3("FRA")
        .mobilePhoneNumber("+261340465338")
        .build();
  }

  public static app.bpartners.api.repository.swan.model.SwanUser joeDoeModel() {
    return app.bpartners.api.repository.swan.model.SwanUser.builder()
        .id(joeDoe().getId())
        .firstName(joeDoe().getFirstName())
        .lastName(joeDoe().getLastName())
        .identificationStatus(joeDoe().getIdentificationStatus())
        .birthDate(joeDoe().getBirthDate())
        .mobilePhoneNumber(joeDoe().getMobilePhoneNumber())
        .idVerified(joeDoe().getIdVerified())
        .nationalityCca3(joeDoe().getNationalityCca3())
        .build();
  }

  public static SwanAccount joeDoeSwanAccount() {
    return SwanAccount.builder()
        .id("beed1765-5c16-472a-b3f4-5c376ce5db58")
        .name("Numer Swan Account")
        .bic("SWNBFR22")
        .iban("FR7699999001001190346460988")
        .build();
  }

  public static AccountHolder joeDoeSwanAccountHolder() {
    return AccountHolder.builder()
        .id("b33e6eb0-e262-4596-a91f-20c6a7bfd343")
        .info(AccountHolder.Info.builder()
            .name("NUMER")
            .businessActivity("businessAndRetail")
            .businessActivityDescription("Phrase détaillée de mon activité")
            .registrationNumber("899067250")
            .build())
        .residencyAddress(AccountHolder.ResidencyAddress.builder()
            .addressLine1("6 RUE PAUL LANGEVIN")
            .city("FONTENAY-SOUS-BOIS")
            .country("FRA")
            .postalCode("94120")
            .build())
        .build();
  }

  public static Customer customer1() {
    return new Customer()
        .id("customer1_id")
        .name("Luc Artisan")
        .email("bpartners.artisans@gmail.com")
        .phone("+33 12 34 56 78")
        .website("https://luc.website.com")
        .address("15 rue Porte d'Orange")
        .zipCode(95160)
        .city("Montmorency")
        .country("France");
  }

  public static Customer customer2() {
    return new Customer()
        .id("customer2_id")
        .name("Jean Plombier")
        .email("jean@email.com")
        .phone("+33 12 34 56 78")
        .website("https://jean.website.com")
        .address("4 Avenue des Près")
        .zipCode(95160)
        .city("Montmorency")
        .country("France");
  }


  public static Product product1() {
    return new Product()
        .id("product1_id")
        .description("Tableau malgache")
        .quantity(1)
        .unitPrice(1000.0)
        .vatPercent(2000.0)
        .totalVat(200.0)
        .totalPriceWithVat(1200.0);
  }

  public static Product product2() {
    return new Product()
        .id("product2_id")
        .description("Tableau baobab")
        .quantity(2)
        .unitPrice(2000.0)
        .vatPercent(1000.0)
        .totalVat(400.0)
        .totalPriceWithVat(4400.0);
  }

  public static Product product3() {
    return new Product()
        .id("product3_id")
        .description("Tableau baobab")
        .quantity(3)
        .unitPrice(2000.0)
        .vatPercent(1000.0)
        .totalVat(600.0)
        .totalPriceWithVat(6600.0);
  }

  public static Product product4() {
    return new Product()
        .id("product4_id")
        .description("Tableau malgache")
        .quantity(1)
        .unitPrice(2000.0)
        .vatPercent(1000.0)
        .totalVat(200.0)
        .totalPriceWithVat(2200.0);
  }

  public static Product product5() {
    return new Product()
        .id("product5_id")
        .description("Mon tableau")
        .quantity(1)
        .unitPrice(1000.0)
        .vatPercent(1000.0)
        .totalVat(100.0)
        .totalPriceWithVat(1100.0);
  }


  public static CreateProduct createProduct3() {
    return new CreateProduct()
        .description("Tableau baobab")
        .quantity(3)
        .unitPrice(2000.0)
        .vatPercent(1000.0);
  }

  public static CreateProduct createProduct4() {
    return new CreateProduct()
        .description("Tableau malgache")
        .quantity(1)
        .unitPrice(2000.0)
        .vatPercent(1000.0);
  }

  public static CreateProduct createProduct5() {
    return new CreateProduct()
        .description("Mon tableau")
        .quantity(1)
        .unitPrice(1000.0)
        .vatPercent(1000.0);
  }

  public static TransactionCategory transactionCategory1() {
    return new TransactionCategory()
        .id("transaction_category1_id")
        .type("Recette TVA 20%")
        .userDefined(false)
        .vat(2000.0)
        .count(1L);
  }

  public static TransactionCategory transactionCategory2() {
    return new TransactionCategory()
        .id("transaction_category2_id")
        .type("Recette TVA 10%")
        .userDefined(false)
        .vat(1000.0)
        .count(2L);
  }

  public static TransactionCategory transactionCategory3() {
    return new TransactionCategory()
        .id("transaction_category3_id")
        .type("Recette TVA 10%")
        .userDefined(false)
        .vat(1000.0)
        .count(2L);
  }

  public static TransactionCategory transactionCategory4() {
    return new TransactionCategory()
        .id("transaction_category4_id")
        .type("Recette personnalisée TVA 1%")
        .userDefined(true)
        .vat(100.0)
        .count(1L);
  }

  public static TransactionCategory transactionCategory5() {
    return new TransactionCategory()
        .id("transaction_category5_id")
        .type("Recette personnalisée TVA 1,2%")
        .userDefined(true)
        .vat(120.0)
        .count(2L);
  }

  public static TransactionCategory transactionCategory6() {
    return new TransactionCategory()
        .id("transaction_category6_id")
        .type("Recette personnalisée TVA 1,2%")
        .vat(120.0)
        .userDefined(true)
        .count(2L);
  }

  static Transaction swanTransaction1() {
    return Transaction.builder()
        .node(Transaction.Node.builder()
            .id("bosci_f224704f2555a42303e302ffb8e69eef")
            .label("Création de site vitrine")
            .reference("REF_001")
            .amount(Transaction.Amount.builder()
                .value(500.0)
                .currency("EUR")
                .build())
            .createdAt(Instant.parse("2022-08-26T06:33:50.595Z"))
            .build())
        .build();
  }

  static Transaction swanTransaction2() {
    return Transaction.builder()
        .node(Transaction.Node.builder()
            .id("bosci_28cb4daf35d3ab24cb775dcdefc8fdab")
            .label("Test du virement")
            .reference("TEST-001")
            .amount(Transaction.Amount.builder()
                .value(100.0)
                .currency("EUR")
                .build())
            .createdAt(Instant.parse("2022-08-24T04:57:02.606Z"))
            .build())
        .build();
  }

  static Transaction swanTransaction3() {
    return Transaction.builder()
        .node(Transaction.Node.builder()
            .id("bosci_0fe167566b234808a44aae415f057b6c")
            .label("Premier virement")
            .reference("JOE-001")
            .amount(Transaction.Amount.builder()
                .value(500.0)
                .currency("EUR")
                .build())
            .createdAt(Instant.parse("2022-08-24T03:39:33.315Z"))
            .build())
        .build();
  }


  public static app.bpartners.api.endpoint.rest.model.Transaction restTransaction1() {
    return new app.bpartners.api.endpoint.rest.model.Transaction()
        .id("bosci_0fe167566b234808a44aae415f057b6c")
        .label("Premier virement")
        .reference("JOE-001")
        .amount(50000)
        .paymentDatetime(Instant.parse("2022-08-24T03:39:33.315Z"))
        .category(List.of(transactionCategory1()));
  }

  public static app.bpartners.api.endpoint.rest.model.Transaction restTransaction2() {
    return new app.bpartners.api.endpoint.rest.model.Transaction()
        .id("bosci_f224704f2555a42303e302ffb8e69eef")
        .label("Création de site vitrine")
        .reference("REF_001")
        .amount(50000)
        .paymentDatetime(Instant.parse("2022-08-26T06:33:50.595Z"));
  }

  public static app.bpartners.api.endpoint.rest.model.Transaction restTransaction3() {
    return new app.bpartners.api.endpoint.rest.model.Transaction()
        .id("bosci_28cb4daf35d3ab24cb775dcdefc8fdab")
        .label("Test du virement")
        .reference("TEST-001")
        .amount(10000)
        .paymentDatetime(Instant.parse("2022-08-24T04:57:02.606Z"))
        .category(List.of(transactionCategory6()));
  }

  public static Invoice invoice1() {
    return new Invoice()
        .id(INVOICE1_ID)
        .fileId("BP001.pdf")
        .title("Facture tableau")
        .customer(customer1())
        .ref("BP001")
        .sendingDate(LocalDate.of(2022, 9, 1))
        .toPayAt(LocalDate.of(2022, 10, 1))
        .status(InvoiceStatus.CONFIRMED)
        .products(List.of(product3(), product4()))
        .totalPriceWithVat(8800.0)
        .totalVat(800.0)
        .totalPriceWithoutVat(8000.0)
        .comment("Tableau de Madagascar")
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        ;
  }

  public static Invoice invoice2() {
    return new Invoice()
        .id(INVOICE2_ID)
        .title("Facture plomberie")
        .fileId("BP002.pdf")
        .customer(customer2().address("Nouvelle adresse"))
        .ref("BP002")
        .sendingDate(LocalDate.of(2022, 9, 10))
        .toPayAt(LocalDate.of(2022, 10, 10))
        .status(InvoiceStatus.CONFIRMED)
        .products(List.of(product5()))
        .totalPriceWithVat(1100.0)
        .totalVat(100.0)
        .totalPriceWithoutVat(1000.0)
        .paymentUrl("https://connect-v2-sbx.fintecture.com");
  }

  public static InvoiceRelaunchConf invoiceRelaunchConf1() {
    return new InvoiceRelaunchConf()
        .id("relaunch1_id")
        .updatedAt(Instant.parse("2022-01-01T04:00:00.00Z"))
        .draftRelaunch(1)
        .unpaidRelaunch(1);
  }

  public static CreateInvoiceRelaunchConf createInvoiceRelaunchConf() {
    return new CreateInvoiceRelaunchConf()
        .draftRelaunch(2)
        .unpaidRelaunch(2);
  }

  public static BusinessActivity businessActivity1() {
    return new BusinessActivity()
        .id("businessActivityTemplate1_id")
        .name("IT");
  }

  public static BusinessActivity businessActivity2() {
    return new BusinessActivity()
        .id("businessActivityTemplate2_id")
        .name("TECHNOLOGY");
  }

  public static CompanyBusinessActivity companyBusinessActivity() {
    return new CompanyBusinessActivity()
        .primary("OTHER")
        .secondary("IT");
  }

  public static CompanyInfo companyInfo() {
    return new CompanyInfo()
        .email("anotherEmail@gmail.com")
        .phone("+33 5 13 3234")
        .socialCapital(40000)
        .tvaNumber("123");
  }

  public static LegalFile legalFile1() {
    return new LegalFile()
        .id("legal_file1_id")
        .name("CGU-November-2022-version-1")
        .fileUrl("https://s3.eu-west-3.amazonaws.com/legal.bpartners.app/cgu.pdf")
        .approvalDatetime(Instant.parse("2022-01-01T00:00:00.00Z"));
  }

  public static LegalFile legalFile2() {
    return new LegalFile()
        .id("legal_file2_id")
        .name("CGU-November-2022-version-2")
        .fileUrl("https://s3.eu-west-3.amazonaws.com/legal.bpartners.app/cgu.pdf")
        .approvalDatetime(Instant.parse("2022-01-02T00:00:00.00Z"));
  }

  public static LegalFile legalFile3() {
    return new LegalFile()
        .id("legal_file3_id")
        .name("CGU-November-2022-version-3")
        .fileUrl("https://s3.eu-west-3.amazonaws.com/legal.bpartners.app/cgu.pdf")
        .approvalDatetime(null);
  }

  public static ApiClient anApiClient(String token, int serverPort) {
    ApiClient client = new ApiClient();
    client.setScheme("http");
    client.setHost("localhost");
    client.setPort(serverPort);
    client.setRequestInterceptor(httpRequestBuilder ->
        httpRequestBuilder.header("Authorization", BEARER_PREFIX + token));
    return client;
  }

  public static void setUpSwanComponent(SwanComponent swanComponent) {
    try {
      when(swanComponent.getSwanUserByToken(BAD_TOKEN)).thenReturn(null);
      when(swanComponent.getSwanUserIdByToken(JOE_DOE_TOKEN)).thenReturn(joeDoe().getId());
      when(swanComponent.getSwanUserByToken(JOE_DOE_TOKEN)).thenReturn(joeDoe());
      when(swanComponent.getTokenByCode(BAD_CODE, REDIRECT_SUCCESS_URL)).thenThrow(
          BadRequestException.class);
    } catch (URISyntaxException | IOException | InterruptedException e) {
      throw new app.bpartners.api.model.exception.ApiException(CLIENT_EXCEPTION, e);
    }
  }

  public static void setUpUserSwanRepository(UserSwanRepository swanRepository) {
    when(swanRepository.whoami()).thenReturn(joeDoeModel());
    when(swanRepository.getByToken(JOE_DOE_TOKEN)).thenReturn(joeDoeModel());
  }

  public static void setUpAccountSwanRepository(AccountSwanRepository swanRepository) {
    when(swanRepository.findAll()).thenReturn(List.of(joeDoeSwanAccount()));
    when(swanRepository.findById(JOE_DOE_ACCOUNT_ID)).thenReturn(List.of(joeDoeSwanAccount()));
    //TODO: fix this as it should be : only accountId instead of userId
    when(swanRepository.findById(JOE_DOE_ID)).thenReturn(List.of(joeDoeSwanAccount()));
    when(swanRepository.findByBearer(JOE_DOE_TOKEN)).thenReturn(List.of(joeDoeSwanAccount()));
    when(swanRepository.findByUserId(JOE_DOE_ID)).thenReturn(List.of(joeDoeSwanAccount()));
  }

  public static void setUpTransactionRepository(TransactionSwanRepository repository) {
    when(repository.getTransactions()).thenReturn(List.of(swanTransaction1(), swanTransaction2(),
        swanTransaction3()));
    when(repository.findById(swanTransaction1().getNode().getId())).thenReturn(swanTransaction1());
    when(repository.findById(swanTransaction2().getNode().getId())).thenReturn(swanTransaction2());
    when(repository.findById(swanTransaction3().getNode().getId())).thenReturn(swanTransaction3());
  }

  public static void setUpOnboardingSwanRepositoryMock(OnboardingSwanRepository repository) {
    when(repository.getOnboardingUrl(REDIRECT_SUCCESS_URL)).thenReturn(SWAN_ONBOARDING_URL_FORMAT);
  }

  public static void setUpAccountHolderSwanRep(AccountHolderSwanRepository swanRepository) {
    when(swanRepository.getById(any())).thenReturn(joeDoeSwanAccountHolder());
    when(swanRepository.getAccountHoldersByAccountId(any()))
        .thenReturn(List.of(joeDoeSwanAccountHolder()));
  }

  public static void setUpPaymentInitiationRep(FintecturePaymentInitiationRepository repository) {
    when(repository.save(any(PaymentInitiation.class), any()))
        .thenReturn(PaymentRedirection.builder()
            .meta(PaymentRedirection.Meta.builder()
                .sessionId("uuid")
                .url("https://connect-v2-sbx.fintecture.com")
                .build())
            .build());
  }

  public static void setUpSesService(SesService service) {
    try {
      doNothing().when(service).sendEmail(any(), any(), any(), any(), any());
    } catch (IOException | MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  public static void setUpSendiblueApi(SendinblueApi sendinblueApi) {
    Attributes attributes = Attributes.builder()
        .id(0D)
        .firstName("John")
        .lastName("Doe")
        .smsPhoneNumber("+33611223344")
        .build();
    when(sendinblueApi.createContact(any())).thenReturn(Contact.builder()
        .email(VALID_EMAIL)
        .updateEnabled(true)
        .listIds(List.of())
        .smtpBlackListed(List.of())
        .smsBlackListed(false)
        .emailBlackListed(false)
        .attributes(attributes)
        .build());
  }

  public static void setUpEventBridge(EventBridgeClient eventBridgeClient) {
    when(eventBridgeClient.putEvents((PutEventsRequest) any())).thenReturn(
        PutEventsResponse.builder().build()
    );
  }

  public static void assertThrowsApiException(String expectedBody, Executable executable) {
    ApiException apiException = assertThrows(ApiException.class, executable);
    assertEquals(expectedBody, apiException.getResponseBody());
  }

  public static void assertThrowsBadRequestException(String expectedBody, Executable executable) {
    BadRequestException badRequestException = assertThrows(BadRequestException.class, executable);
    assertEquals(expectedBody, badRequestException.getMessage());
  }

  public static void assertThrowsForbiddenException(Executable executable) {
    ApiException apiException = assertThrows(ApiException.class, executable);
    String responseBody = apiException.getResponseBody();
    assertEquals("{"
        + "\"type\":\"403 FORBIDDEN\","
        + "\"message\":\"Access is denied\"}", responseBody);
  }

  public static int anAvailableRandomPort() {
    try {
      return new ServerSocket(0).getLocalPort();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static ApiException getApiException(String operationId, HttpResponse<byte[]> response) {
    String body = response.body() == null ? null : new String(response.body());
    String message = formatExceptionMessage(operationId, response.statusCode(), body);
    return new ApiException(response.statusCode(), message, response.headers(), body);
  }

  private static String formatExceptionMessage(String operationId, int statusCode, String body) {
    if (body == null || body.isEmpty()) {
      body = "[no body]";
    }
    return operationId + " call failed with: " + statusCode + " - " + body;
  }
}
