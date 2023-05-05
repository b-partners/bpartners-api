package app.bpartners.api.integration.conf;

import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.AccountInvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.model.AnnualRevenueTarget;
import app.bpartners.api.endpoint.rest.model.BusinessActivity;
import app.bpartners.api.endpoint.rest.model.CompanyBusinessActivity;
import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import app.bpartners.api.endpoint.rest.model.CreateAccountInvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.model.CreateAnnualRevenueTarget;
import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.InvoiceDiscount;
import app.bpartners.api.endpoint.rest.model.LegalFile;
import app.bpartners.api.endpoint.rest.model.PaymentRegulation;
import app.bpartners.api.endpoint.rest.model.PaymentRequest;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.model.ProductStatus;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.endpoint.rest.model.UpdateCustomerStatus;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentInfoRepository;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.fintecture.model.FPaymentInitiation;
import app.bpartners.api.repository.fintecture.model.FPaymentRedirection;
import app.bpartners.api.repository.fintecture.model.Session;
import app.bpartners.api.repository.sendinblue.SendinblueApi;
import app.bpartners.api.repository.sendinblue.model.Attributes;
import app.bpartners.api.repository.sendinblue.model.Contact;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.OnboardingSwanRepository;
import app.bpartners.api.repository.swan.TransactionSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.model.SwanAccount;
import app.bpartners.api.repository.swan.model.SwanAccountHolder;
import app.bpartners.api.repository.swan.model.SwanTransaction;
import app.bpartners.api.repository.swan.model.SwanUser;
import app.bpartners.api.repository.swan.response.AccountResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import javax.net.ssl.SSLSession;
import lombok.SneakyThrows;
import org.junit.jupiter.api.function.Executable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import static app.bpartners.api.endpoint.rest.model.AccountStatus.OPENED;
import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.IdentificationStatus.VALID_IDENTITY;
import static app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.IN_INSTALMENT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.TransactionTypeEnum.INCOME;
import static app.bpartners.api.endpoint.rest.model.TransactionTypeEnum.OUTCOME;
import static app.bpartners.api.model.Invoice.DEFAULT_DELAY_PENALTY_PERCENT;
import static app.bpartners.api.model.Invoice.DEFAULT_TO_PAY_DELAY_DAYS;
import static app.bpartners.api.model.Transaction.RELEASED_STATUS;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.CLIENT_EXCEPTION;
import static app.bpartners.api.model.mapper.UserMapper.VALID_IDENTITY_STATUS;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class TestUtils {
  public static final String CREDIT_SIDE = "Credit";
  public static final String DEBIT_SIDE = "Debit";
  public static final String BOOKED_STATUS = "Booked";
  public static final String PENDING_STATUS = "Pending";
  public static final String JOE_DOE_ID = "joe_doe_id";
  public static final String BERNARD_DOE_ID = "bernard_doe_id";
  public static final String JOE_DOE_SWAN_USER_ID = "c15924bf-61f9-4381-8c9b-d34369bf91f7";
  public static final String JANE_DOE_SWAN_USER_ID = "jane_doe_user_id";
  public static final String BERNARD_DOE_SWAN_USER_ID = "c15924bf-61f9-4381-8c9b-d34369bf91f7";
  public static final String BAD_TOKEN = "bad_token";
  public static final String VALID_EMAIL = "username@domain.com";
  public static final String API_URL = "https://api.swan.io/sandbox-partner/graphql";
  public static final String OAUTH_URL = "https://api-sandbox.fintecture.com/oauth/accesstoken";
  public static final String PIS_URL = "https://api-sandbox.fintecture.com/pis/v2/";
  public static final String REDIRECT_SUCCESS_URL =
      "https://dashboard-dev.bpartners.app/login/success";
  public static final String REDIRECT_FAILURE_URL =
      "https://dashboard-dev.bpartners.app/login/failure";
  public static final String JOE_DOE_ACCOUNT_ID = "beed1765-5c16-472a-b3f4-5c376ce5db58";
  public static final String BERNARD_DOE_ACCOUNT_ID = "account_pro_id";
  public static final String OTHER_ACCOUNT_ID = "other_account_id";
  public static final String OTHER_CUSTOMER_ID = "other_customer_id";
  public static final String USER1_ID = "user1_id";
  public static final String BAD_USER_ID = "bad_user_id";
  public static final String INVOICE1_ID = "invoice1_id";
  public static final String INVOICE2_ID = "invoice2_id";
  public static final String INVOICE3_ID = "invoice3_id";
  public static final String INVOICE4_ID = "invoice4_id";
  public static final String INVOICE7_ID = "invoice7_id";
  public static final String FILE_ID = "test.jpeg";
  public static final String TEST_FILE_ID = "test.jpeg";
  public static final String OTHER_TEST_FILE_ID = "image.jpeg";
  public static final String TO_UPLOAD_FILE_ID = "to_upload_file_id.jpeg";
  public static final String MARKETPLACE1_ID = "marketplace1_id";
  public static final String MARKETPLACE2_ID = "marketplace2_id";
  public static final String BEARER_QUERY_PARAMETER_NAME = "accessToken";
  public static final String BEARER_PREFIX = "Bearer ";
  public static final String JOE_DOE_TOKEN = "joe_doe_token";
  public static final String BERNARD_DOE_TOKEN = "bernard_doe_token";
  public static final String JOE_DOE_COGNITO_TOKEN = "joe_doe_token";
  public static final String BERNARD_DOE_COGNITO_TOKEN = "bernard_doe_token";
  //TODO(distinct-cognito): should be diff from Swan
  public static final String PROJECT_TOKEN = "project_token";
  public static final String BAD_CODE = "bad_code";
  public static final String SWAN_ONBOARDING_URL_FORMAT =
      "https://api.banking.sandbox.swan.io/projects/uuid/onboardings/uuid";
  public static final String INVOICE_RELAUNCH1_ID = "invoice_relaunch1_id";
  public static final String INVOICE_RELAUNCH2_ID = "invoice_relaunch2_id";
  public static final String INVALID_LOGO_TYPE = "invalid_logo_type";

  public static final String NOT_JOE_DOE_ACCOUNT_ID = "NOT_" + JOE_DOE_ACCOUNT_ID;
  public static final String VERIFIED_STATUS = "Verified";
  public static final String SWAN_TRANSACTION_ID = "bosci_f224704f2555a42303e302ffb8e69eef";
  public static final String SWAN_ACCOUNTHOLDER_ID = "b33e6eb0-e262-4596-a91f-20c6a7bfd343";
  public static final String ACCOUNTHOLDER2_ID = "account_holder_id_2";
  public static final String JANE_ACCOUNT_ID = "jane_account_id";
  public static final String JANE_DOE_TOKEN = "jane_doe_token";
  public static final String JANE_DOE_ID = "jane_doe_id";
  public static final String SESSION_ID = "session_id";
  public static final String ACCOUNT_OPENED = "Opened";
  public static final String VALIDATION_REQUIRED = "Validation Required";
  public static final String ACCOUNT_CLOSED = "Closed";
  public static final String ACCOUNT_CLOSING = "Closing";
  public static final String ACCOUNT_SUSPENDED = "Suspended";
  public static final String TRANSACTION1_ID = "transaction1_id";
  public static final String UNKNOWN_TRANSACTION_ID = "unknown_transaction_id";
  public static final String SESSION1_ID = "session1_id";
  public static final String SESSION2_ID = "session2_id";
  public static final String NOT_JOE_DOE_ACCOUNT_HOLDER_ID = "NOT_" + SWAN_ACCOUNTHOLDER_ID;
  public static final String GEOJSON_TYPE_POINT = "Point";

  public static User restJoeDoeUser() {
    return new User()
        .id(TestUtils.JOE_DOE_ID)
        .firstName(joeDoe().getFirstName())
        .lastName(joeDoe().getLastName())
        .phone(joeDoe().getMobilePhoneNumber())
        .monthlySubscriptionAmount(5)
        .status(ENABLED)
        .idVerified(true)
        .identificationStatus(VALID_IDENTITY)
        .logoFileId("logo.jpeg");
  }

  public static User restJaneDoeUser() {
    return new User()
        .id(TestUtils.JANE_DOE_ID)
        .firstName(janeDoe().getFirstName())
        .lastName(janeDoe().getLastName())
        .phone(janeDoe().getMobilePhoneNumber())
        .monthlySubscriptionAmount(5)
        .status(ENABLED)
        .idVerified(true)
        .identificationStatus(VALID_IDENTITY)
        .logoFileId("logo.jpeg");
  }

  public static SwanUser joeDoe() {
    return SwanUser.builder()
        .id(JOE_DOE_SWAN_USER_ID)
        .firstName("Joe")
        .lastName("Doe")
        .idVerified(true)
        .identificationStatus(VALID_IDENTITY_STATUS)
        .mobilePhoneNumber("+261340465338")
        .build();
  }

  public static SwanUser bernardDoe() {
    return SwanUser.builder()
        .id(BERNARD_DOE_SWAN_USER_ID)
        .firstName("Bernard")
        .lastName("Doe")
        .idVerified(true)
        .identificationStatus(VALID_IDENTITY_STATUS)
        .mobilePhoneNumber("+261342463616")
        .build();
  }

  public static SwanUser janeDoe() {
    return SwanUser.builder()
        .id("jane_doe_user_id")
        .firstName("Jane")
        .lastName("Doe")
        .idVerified(true)
        .identificationStatus(VALID_IDENTITY_STATUS)
        .mobilePhoneNumber("+261341122334")
        .build();
  }

  public static app.bpartners.api.repository.swan.model.SwanUser joeDoeModel() {
    return app.bpartners.api.repository.swan.model.SwanUser.builder()
        .id(joeDoe().getId())
        .firstName(joeDoe().getFirstName())
        .lastName(joeDoe().getLastName())
        .identificationStatus(joeDoe().getIdentificationStatus())
        .mobilePhoneNumber(joeDoe().getMobilePhoneNumber())
        .idVerified(joeDoe().isIdVerified())
        .build();
  }

  public static app.bpartners.api.repository.swan.model.SwanUser bernardDoeModel() {
    return app.bpartners.api.repository.swan.model.SwanUser.builder()
        .id(bernardDoe().getId())
        .firstName(bernardDoe().getFirstName())
        .lastName(bernardDoe().getLastName())
        .identificationStatus(bernardDoe().getIdentificationStatus())
        .mobilePhoneNumber(bernardDoe().getMobilePhoneNumber())
        .idVerified(bernardDoe().isIdVerified())
        .build();
  }

  public static app.bpartners.api.repository.swan.model.SwanUser janeDoeModel() {
    return app.bpartners.api.repository.swan.model.SwanUser.builder()
        .id(janeDoe().getId())
        .firstName(janeDoe().getFirstName())
        .lastName(janeDoe().getLastName())
        .identificationStatus(janeDoe().getIdentificationStatus())
        .mobilePhoneNumber(janeDoe().getMobilePhoneNumber())
        .idVerified(janeDoe().isIdVerified())
        .build();
  }

  public static BridgeAccount joeDoeBridgeAccount() {
    return BridgeAccount.builder()
        .id(123L)
        .bankId(1234L)
        .name("Numer Bridge Account")
        .iban("FR7699999001001190346460988")
        .status(0)
        .balance(0.0)
        .build();
  }

  public static BridgeAccount otherBridgeAccount() {
    return BridgeAccount.builder()
        .id(456L)
        .bankId(456L)
        .name("Other")
        .iban("FR12349001001190346460988")
        .status(0)
        .balance(0.0)
        .build();
  }

  public static SwanAccount joeDoeSwanAccount() {
    return SwanAccount.builder()
        .id("beed1765-5c16-472a-b3f4-5c376ce5db58")
        .name("Numer Swan Account")
        .bic("SWNBFR22")
        .iban("FR7699999001001190346460988")
        .balances(new SwanAccount.Balances(
            new SwanAccount.Balances.Available(1000.0)
        ))
        .statusInfo(new SwanAccount.StatusInfo(ACCOUNT_OPENED))
        .build();
  }

  public static SwanAccount bernardDoeSwanAccount() {
    return SwanAccount.builder()
        .id("account_pro_id")
        .name("Numer Swan Account")
        .bic("SWNBFR22")
        .iban("FR7699999001001190346460988")
        .balances(new SwanAccount.Balances(
            new SwanAccount.Balances.Available(1000.0)
        ))
        .statusInfo(new SwanAccount.StatusInfo(VALIDATION_REQUIRED))
        .build();
  }

  public static SwanAccount janeSwanAccount() {
    return SwanAccount.builder()
        .id("jane_account_id")
        .name("Jane Account")
        .bic("SWNBFR22")
        .iban("FR7699999001001190346460988")
        .balances(new SwanAccount.Balances(
            new SwanAccount.Balances.Available(1000.0)
        ))
        .statusInfo(new SwanAccount.StatusInfo(ACCOUNT_OPENED))
        .build();
  }

  public static SwanAccountHolder joeDoeSwanAccountHolder() {
    return SwanAccountHolder.builder()
        .id("b33e6eb0-e262-4596-a91f-20c6a7bfd343")
        .verificationStatus(VERIFIED_STATUS)
        .info(SwanAccountHolder.Info.builder()
            .name("NUMER")
            .businessActivity("businessAndRetail")
            .businessActivityDescription("Phrase détaillée de mon activité")
            .registrationNumber("899067250")
            .vatNumber("FR32123456789")
            .build())
        .residencyAddress(SwanAccountHolder.ResidencyAddress.builder()
            .addressLine1("6 RUE PAUL LANGEVIN")
            .city("FONTENAY-SOUS-BOIS")
            .country("FRA")
            .postalCode("94120")
            .build())
        .accounts(SwanAccountHolder.Accounts.builder()
            .edges(List.of(AccountResponse.Edge.builder()
                .node(SwanAccount.builder()
                    .id(joeDoeSwanAccount().getId())
                    .statusInfo(SwanAccount.StatusInfo.builder()
                        .status(ACCOUNT_OPENED)
                        .build())
                    .build())
                .build()))
            .build())
        .build();
  }

  public static SwanAccountHolder bernardDoeSwanAccountHolder() {
    return SwanAccountHolder.builder()
        .id("account_holder_id_3")
        .verificationStatus(VERIFIED_STATUS)
        .info(SwanAccountHolder.Info.builder()
            .name("NUMER")
            .businessActivity("businessAndRetail")
            .businessActivityDescription("Phrase détaillée de mon activité")
            .registrationNumber("899067250")
            .vatNumber("FR32123456789")
            .build())
        .residencyAddress(SwanAccountHolder.ResidencyAddress.builder()
            .addressLine1("6 RUE PAUL LANGEVIN")
            .city("FONTENAY-SOUS-BOIS")
            .country("FRA")
            .postalCode("94120")
            .build())
        .accounts(SwanAccountHolder.Accounts.builder()
            .edges(List.of(AccountResponse.Edge.builder()
                .node(SwanAccount.builder()
                    .id(bernardDoeSwanAccount().getId())
                    .statusInfo(SwanAccount.StatusInfo.builder()
                        .status(VALIDATION_REQUIRED)
                        .build())
                    .build())
                .build()))
            .build())
        .build();
  }

  public static AnnualRevenueTarget annualRevenueTarget1() {
    return new AnnualRevenueTarget()
        .year(2023)
        .updatedAt(Instant.parse("2022-01-01T01:00:00.00Z"))
        .amountAttempted(1356000)
        .amountTarget(1000000)
        .amountAttemptedPercent(13560);
  }

  public static AnnualRevenueTarget annualRevenueTarget2() {
    return new AnnualRevenueTarget()
        .year(2024)
        .updatedAt(Instant.parse("2022-01-08T01:00:00.00Z"))
        .amountAttempted(0)
        .amountTarget(1000000)
        .amountAttemptedPercent(0);
  }

  public static CreateAnnualRevenueTarget createAnnualRevenueTarget() {
    return new CreateAnnualRevenueTarget()
        .year(2025)
        .amountTarget(150000);
  }

  public static CreateAnnualRevenueTarget toUpdateAnnualRevenueTarget() {
    return new CreateAnnualRevenueTarget()
        .year(2023)
        .amountTarget(2000000);
  }

  public static Customer customer1() {
    return new Customer()
        .id("customer1_id")
        .firstName("Luc")
        .lastName("Artisan")
        .email("bpartners.artisans@gmail.com")
        .phone("+33 12 34 56 78")
        .website("https://luc.website.com")
        .address("15 rue Porte d'Orange")
        .zipCode(95160)
        .city("Metz")
        .country(null)
        .comment("Rencontre avec Luc")
        .status(CustomerStatus.ENABLED);
  }

  public static Customer customer2() {
    return new Customer()
        .id("customer2_id")
        .firstName("Jean")
        .lastName("Plombier")
        .email(null)
        .phone("+33 12 34 56 78")
        .website("https://jean.website.com")
        .address("4 Avenue des Près")
        .zipCode(95160)
        .city("Montmorency")
        .country("France")
        .comment("Rencontre avec le plombier")
        .status(CustomerStatus.ENABLED);
  }

  public static Customer customerUpdated() {
    return new Customer()
        .id("customer3_id")
        .firstName("Marc")
        .lastName("Montagnier")
        .email("marcmontagnier@gmail.com")
        .phone("+33 12 34 56 78")
        .website("https://marc.website.com")
        .address("15 rue Porte d'Orange")
        .zipCode(95160)
        .city("Montmorency")
        .country("France")
        .comment("Rencontre avec Marc")
        .status(CustomerStatus.ENABLED);
  }

  public static Customer customerWithSomeNullAttributes() {
    return new Customer()
        .id("customer3_id")
        .firstName("Marc")
        .lastName("Montagnier")
        .email(null)
        .phone(null)
        .website(null)
        .address(null)
        .zipCode(95160)
        .city(null)
        .country(null)
        .comment(null)
        .status(CustomerStatus.ENABLED);
  }

  public static UpdateCustomerStatus customerDisabled() {
    return new UpdateCustomerStatus()
        .id("customer3_id")
        .status(CustomerStatus.DISABLED);
  }

  public static Product product1() {
    return new Product()
        .id("product1_id")
        .description("Tableau malgache")
        .unitPrice(1000)
        .vatPercent(2000)
        .unitPriceWithVat(1200)
        .quantity(null)
        .totalVat(null)
        .createdAt(Instant.parse("2022-01-01T01:00:00.00Z"))
        .status(ProductStatus.ENABLED)
        .totalPriceWithVat(null);
  }

  public static Product product2() {
    return new Product()
        .id("product2_id")
        .description("Tableau baobab")
        .quantity(2)
        .unitPrice(2000)
        .vatPercent(1000)
        .unitPriceWithVat(2200)
        .totalVat(400)
        .createdAt(Instant.parse("2022-01-01T02:00:00.00Z"))
        .totalPriceWithVat(4400)
        .status(ProductStatus.ENABLED);
  }

  public static Product product3() {
    return new Product()
        .id("product3_id")
        .description("Tuyau 1m")
        .quantity(3)
        .unitPrice(2000)
        .vatPercent(1000)
        .unitPriceWithVat(2200)
        .totalVat(600)
        .createdAt(null)
        .status(ProductStatus.ENABLED)
        .totalPriceWithVat(6600);
  }

  public static Product product4() {
    return new Product()
        .id("product4_id")
        .description("Autres produits")
        .quantity(1)
        .unitPrice(2000)
        .vatPercent(1000)
        .unitPriceWithVat(2200)
        .totalVat(200)
        .createdAt(null)
        .totalPriceWithVat(2200)
        .status(ProductStatus.ENABLED);
  }

  public static Product product6() {
    return new Product()
        .id("product6_id")
        .description("Autres produits")
        .unitPrice(1000)
        .vatPercent(1000)
        .unitPriceWithVat(1100)
        .status(ProductStatus.ENABLED);
  }

  public static Product product5() {
    return new Product()
        .id("product5_id")
        .description("Machine agro-alimentaire")
        .quantity(1)
        .unitPrice(1000)
        .vatPercent(1000)
        .unitPriceWithVat(1100)
        .totalVat(100)
        .createdAt(null)
        .status(ProductStatus.ENABLED)
        .totalPriceWithVat(1100);
  }

  public static Product disabledProduct() {
    return new Product()
        .id("product8_id")
        .description("Cartable")
        .unitPrice(1200)
        .vatPercent(1000)
        .unitPriceWithVat(1320)
        .createdAt(Instant.parse("2022-01-01T04:00:00.00Z"))
        .status(ProductStatus.DISABLED);
  }

  public static CreateProduct createProduct2() {
    return new CreateProduct()
        .description("Tableau baobab")
        .quantity(1)
        .unitPrice(2000)
        .vatPercent(1000);
  }

  public static CreateProduct createProduct4() {
    return new CreateProduct()
        .description("Autres produits")
        .quantity(1)
        .unitPrice(2000)
        .vatPercent(1000);
  }

  public static CreateProduct createProduct5() {
    return new CreateProduct()
        .description("Machine agro-alimentaire")
        .quantity(1)
        .unitPrice(1000)
        .vatPercent(1000);
  }

  public static TransactionCategory transactionCategory1() {
    return new TransactionCategory()
        .id("transaction_category1_id")
        .type("Recette TVA 20%")
        .description("Prestations ou ventes soumises à 20% de TVA")
        .transactionType(INCOME)
        .description("Prestations ou ventes soumises à 20% de TVA")
        .vat(2000)
        .count(0L)
        .isOther(false);
  }

  public static SwanTransaction swanTransaction1() {
    return SwanTransaction.builder()
        .node(SwanTransaction.Node.builder()
            .id("bosci_f224704f2555a42303e302ffb8e69eef")
            .label("Création de site vitrine")
            .reference("REF_001")
            .amount(SwanTransaction.Amount.builder()
                .value(500.0)
                .currency("EUR")
                .build())
            .createdAt(Instant.parse("2022-08-26T06:33:50.595Z"))
            .side(CREDIT_SIDE)
            .statusInfo(new SwanTransaction.Node.StatusInfo(PENDING_STATUS))
            .build())
        .build();
  }

  public static SwanTransaction swanTransaction2() {
    return SwanTransaction.builder()
        .node(SwanTransaction.Node.builder()
            .id("bosci_28cb4daf35d3ab24cb775dcdefc8fdab")
            .label("Test du virement")
            .reference("TEST-001")
            .amount(SwanTransaction.Amount.builder()
                .value(100.0)
                .currency("EUR")
                .build())
            .createdAt(Instant.parse("2022-08-24T04:57:02.606Z"))
            .side(DEBIT_SIDE)
            .statusInfo(new SwanTransaction.Node.StatusInfo(BOOKED_STATUS))
            .build())
        .build();
  }

  public static SwanTransaction swanTransaction3() {
    return SwanTransaction.builder()
        .node(SwanTransaction.Node.builder()
            .id("bosci_0fe167566b234808a44aae415f057b6c")
            .label("Premier virement")
            .reference("JOE-001")
            .amount(SwanTransaction.Amount.builder()
                .value(500.0)
                .currency("EUR")
                .build())
            .createdAt(Instant.parse("2022-08-24T03:39:33.315Z"))
            .side(CREDIT_SIDE)
            .statusInfo(new SwanTransaction.Node.StatusInfo(BOOKED_STATUS))
            .build())
        .build();
  }

  public static SwanTransaction swanTransaction4() {
    return SwanTransaction.builder()
        .node(SwanTransaction.Node.builder()
            .id("bosci_12azgrb712gzf057b6c")
            .label("Transaction avec nouveau statut")
            .reference("123456")
            .amount(SwanTransaction.Amount.builder()
                .value(400.0)
                .currency("EUR")
                .build())
            .createdAt(Instant.parse("2023-01-01T00:00:00.00Z"))
            .side(CREDIT_SIDE)
            .statusInfo(new SwanTransaction.Node.StatusInfo(RELEASED_STATUS))
            .build())
        .build();
  }

  public static SwanTransaction updatedSwanTransaction() {
    return SwanTransaction.builder()
        .node(SwanTransaction.Node.builder()
            .id("bosci_f224704f2555a42303e302ffb8e69eef")
            .label("Label à jour")
            .reference("NEW_REF")
            .amount(SwanTransaction.Amount.builder()
                .value(111.1)
                .currency("EUR")
                .build())
            .createdAt(Instant.parse("2022-08-26T01:00:00.00Z"))
            .side(DEBIT_SIDE)
            .statusInfo(new SwanTransaction.Node.StatusInfo(BOOKED_STATUS))
            .build())
        .build();
  }

  public static app.bpartners.api.endpoint.rest.model.Transaction restTransaction2() {
    return new app.bpartners.api.endpoint.rest.model.Transaction()
        .id("transaction2_id")
        .label("Premier virement")
        .reference("JOE-001")
        .amount(50000)
        .type(INCOME)
        .status(TransactionStatus.BOOKED)
        .paymentDatetime(Instant.parse("2022-08-24T03:39:33.315Z"))
        .category(List.of(transactionCategory1()));
  }

  public static app.bpartners.api.endpoint.rest.model.Transaction restTransaction1() {
    return new app.bpartners.api.endpoint.rest.model.Transaction()
        .id(TRANSACTION1_ID)
        .label("Création de site vitrine")
        .reference("REF_001")
        .amount(50000)
        .type(INCOME)
        .status(TransactionStatus.PENDING)
        .category(null)
        .paymentDatetime(Instant.parse("2022-08-26T06:33:50.595Z"));
  }

  public static app.bpartners.api.endpoint.rest.model.Transaction restUpdatedTransaction() {
    return new app.bpartners.api.endpoint.rest.model.Transaction()
        .id(TRANSACTION1_ID)
        .label(updatedSwanTransaction().getNode().getLabel())
        .reference(updatedSwanTransaction().getNode().getReference())
        .amount(11110)
        .type(OUTCOME)
        .status(TransactionStatus.BOOKED)
        .category(null)
        .paymentDatetime(Instant.parse("2022-08-26T01:00:00.00Z"));
  }

  public static app.bpartners.api.endpoint.rest.model.Transaction restTransaction4() {
    return new app.bpartners.api.endpoint.rest.model.Transaction()
        .id(swanTransaction4().getNode().getId())
        .label(swanTransaction4().getNode().getLabel())
        .reference(swanTransaction4().getNode().getReference())
        .amount(40000)
        .type(INCOME)
        .status(TransactionStatus.RELEASED)
        .paymentDatetime(Instant.parse("2023-01-01T00:00:00.00Z"))
        .category(null);
  }

  public static app.bpartners.api.endpoint.rest.model.Transaction restTransaction3() {
    return new app.bpartners.api.endpoint.rest.model.Transaction()
        .id("bosci_28cb4daf35d3ab24cb775dcdefc8fdab")
        .label("Test du virement")
        .reference("TEST-001")
        .amount(10000)
        .type(OUTCOME)
        .status(TransactionStatus.BOOKED)
        .paymentDatetime(Instant.parse("2022-08-24T04:57:02.606Z"))
        .category(null);
  }

  public static PaymentRequest basePaymentRequest() {
    return new PaymentRequest()
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .amount(4400)
        .payerName("Luc Artisan")
        .payerEmail("bpartners.artisans@gmail.com");
  }

  public static PaymentRegulation datedPaymentRequest2() {
    return new PaymentRegulation()
        .maturityDate(LocalDate.of(2023, 2, 1))
        .paymentRequest(basePaymentRequest()
            .id("bab75c91-f275-4f68-b10a-0f30f96f7806")
            .label("Reste 50%")
            .reference("FAC2023ACT02")
            .percentValue(5000)
            .initiatedDatetime(Instant.parse("2023-01-02T00:00:00Z")));
  }

  public static PaymentRegulation datedPaymentRequest1() {
    return new PaymentRegulation()
        .maturityDate(LocalDate.of(2023, 2, 1))
        .paymentRequest(basePaymentRequest()
            .id("a1275c91-f275-4f68-b10a-0f30f96f7806")
            .label("Acompte 50%")
            .reference("FAC2023ACT01")
            .percentValue(5000)
            .initiatedDatetime(Instant.parse("2023-01-01T00:00:00Z")));
  }


  public static Invoice invoice1() {
    return new Invoice()
        .id(INVOICE1_ID)
        .comment(null)
        .title("Outils pour plomberie")
        .fileId("file1_id")
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .customer(customer1()).ref("BP001")
        .createdAt(Instant.parse("2022-01-01T01:00:00.00Z"))
        .sendingDate(LocalDate.of(2022, 9, 1))
        .validityDate(LocalDate.of(2022, 10, 3))
        .toPayAt(LocalDate.of(2022, 10, 1))
        .delayInPaymentAllowed(null)
        .delayPenaltyPercent(DEFAULT_DELAY_PENALTY_PERCENT)
        .status(CONFIRMED)
        .products(List.of(product3(), product4()))
        .totalPriceWithVat(8800)
        .totalVat(800)
        .totalPriceWithoutVat(8000)
        .paymentType(IN_INSTALMENT)
        .paymentRegulations(List.of(datedPaymentRequest1(), datedPaymentRequest2()))
        .totalPriceWithoutDiscount(8000)
        .globalDiscount(new InvoiceDiscount()
            .percentValue(0)
            .amountValue(0))
        .metadata(Map.of());
  }

  public static Invoice invoice2() {
    return new Invoice()
        .id(INVOICE2_ID)
        .title("Facture plomberie")
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .paymentRegulations(List.of())
        .customer(customer2())
        .ref("BP002")
        .sendingDate(LocalDate.of(2022, 9, 10))
        .validityDate(LocalDate.of(2022, 10, 14))
        .createdAt(Instant.parse("2022-01-01T03:00:00.00Z"))
        .toPayAt(LocalDate.of(2022, 10, 10))
        .delayInPaymentAllowed(DEFAULT_TO_PAY_DELAY_DAYS)
        .delayPenaltyPercent(DEFAULT_DELAY_PENALTY_PERCENT)
        .status(CONFIRMED)
        .products(List.of(product5()))
        .totalPriceWithVat(1100)
        .totalVat(100).totalPriceWithoutVat(1000)
        .metadata(Map.of());
  }

  public static AccountInvoiceRelaunchConf invoiceRelaunchConf1() {
    return new AccountInvoiceRelaunchConf()
        .id("relaunch1_id")
        .updatedAt(Instant.parse("2022-01-01T04:00:00.00Z"))
        .draftRelaunch(1)
        .unpaidRelaunch(1);
  }

  public static CreateAccountInvoiceRelaunchConf createInvoiceRelaunchConf() {
    return new CreateAccountInvoiceRelaunchConf()
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
        .primary("Boulanger")
        .secondary("Fromager");
  }

  public static CompanyInfo companyInfo() {
    return new CompanyInfo()
        .isSubjectToVat(false)
        .email("anotherEmail@gmail.com")
        .phone("+33 5 13 3234")
        .socialCapital(40000)
        .tvaNumber("FR12323456789");
  }

  public static LegalFile legalFile1() {
    return new LegalFile()
        .id("legal_file1_id")
        .name("CGU-November-2022-version-1")
        .fileUrl("https://s3.eu-west-3.amazonaws.com/legal.bpartners.app/cgu.pdf")
        .approvalDatetime(Instant.parse("2022-01-01T00:00:00.00Z"));
  }

  public static LegalFile defaultLegalFile() {
    return new LegalFile()
        .id("e200a1fd-5bb7-4b7a-a521-4a6002dc1927")
        .name("cgu_28-10-22.pdf")
        .fileUrl("https://legal.bpartners.app/cgu_28-10-22.pdf")
        .approvalDatetime(null);
  }

  public static app.bpartners.api.model.LegalFile domainLegalFile() {
    return app.bpartners.api.model.LegalFile.builder()
        .id(defaultLegalFile().getId())
        .fileUrl(defaultLegalFile().getFileUrl())
        .name(defaultLegalFile().getName())
        .build();
  }

  public static app.bpartners.api.model.LegalFile domainApprovedLegalFile() {
    return app.bpartners.api.model.LegalFile.builder()
        .id(defaultLegalFile().getId())
        .fileUrl(defaultLegalFile().getFileUrl())
        .name(defaultLegalFile().getName())
        .approvalDatetime(Instant.now())
        .build();
  }

  public static Session fintectureSession() {
    return Session.builder()
        .meta(Session.Meta.builder()
            .code("200")
            .build())
        .data(Session.Data.builder()
            .type("payments")
            .attributes(Session.Attributes.builder()
                .paymentScheme("SEPA")
                .endToEndId("end_to_end_id")
                .build())
            .build())
        .build();
  }

  public static HttpResponse<Object> httpResponseMock(Object body) {
    return new HttpResponse<>() {
      @Override
      public int statusCode() {
        return 200;
      }

      @Override
      public HttpRequest request() {
        return null;
      }

      @Override
      public Optional<HttpResponse<Object>> previousResponse() {
        return Optional.empty();
      }

      @Override
      public HttpHeaders headers() {
        return null;
      }

      @Override
      public Object body() {
        if (body.getClass() == String.class) {
          return body;
        }
        try {
          return new ObjectMapper().writeValueAsString(body);
        } catch (JsonProcessingException e) {
          return null;
        }
      }

      @Override
      public Optional<SSLSession> sslSession() {
        return Optional.empty();
      }

      @Override
      public URI uri() {
        return null;
      }

      @Override
      public HttpClient.Version version() {
        return null;
      }
    };
  }

  public static Geojson location() {
    return new Geojson()
        .type(GEOJSON_TYPE_POINT)
        .longitude(1.0)
        .latitude(23.5);
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

  public static void setUpProvider(PrincipalProvider provider) {
    when(provider.getAuthentication()).thenReturn(
        new UsernamePasswordAuthenticationToken(
            new Principal(
                new app.bpartners.api.model.User(),
                JOE_DOE_TOKEN
            ),
            new Object()
        )
    );
  }

  public static void setUpFintectureConf(FintectureConf fintectureConfMock)
      throws NoSuchAlgorithmException {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair = generator.generateKeyPair();
    String encodedKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
    when(fintectureConfMock.getPrivateKey()).thenReturn(encodedKey);
    when(fintectureConfMock.getRequestToPayUrl()).thenReturn(PIS_URL + "request-to-pay");
    when(fintectureConfMock.getPaymentUrl()).thenReturn(PIS_URL + "payments");
  }

  public static void setUpSwanComponent(SwanComponent swanComponent) {
    try {
      when(swanComponent.getSwanUserByToken(BAD_TOKEN)).thenReturn(null);
      when(swanComponent.getSwanUserIdByToken(JOE_DOE_TOKEN)).thenReturn(joeDoe().getId());
      when(swanComponent.getSwanUserByToken(JOE_DOE_TOKEN)).thenReturn(joeDoe());

      when(swanComponent.getSwanUserIdByToken(BERNARD_DOE_TOKEN)).thenReturn(bernardDoe().getId());
      when(swanComponent.getSwanUserByToken(BERNARD_DOE_TOKEN)).thenReturn(bernardDoe());

      when(swanComponent.getSwanUserIdByToken(JANE_DOE_TOKEN)).thenReturn(janeDoe().getId());
      when(swanComponent.getSwanUserByToken(JANE_DOE_TOKEN)).thenReturn(janeDoe());
      when(swanComponent.getTokenByCode(BAD_CODE, REDIRECT_SUCCESS_URL)).thenThrow(
          BadRequestException.class);
    } catch (URISyntaxException | IOException e) {
      throw new app.bpartners.api.model.exception.ApiException(CLIENT_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  public static void setUpUserSwanRepository(UserSwanRepository swanRepository) {
    when(swanRepository.whoami()).thenReturn(joeDoeModel());
    when(swanRepository.getByToken(JOE_DOE_TOKEN)).thenReturn(joeDoeModel());
    when(swanRepository.getByToken(JANE_DOE_TOKEN)).thenReturn(janeDoeModel());
  }

  public static void setUpBernardUserSwanRepository(UserSwanRepository swanRepository) {
    when(swanRepository.whoami()).thenReturn(bernardDoeModel());
    when(swanRepository.getByToken(BERNARD_DOE_TOKEN)).thenReturn(bernardDoeModel());
  }

  public static void setUpAccountSwanRepository(AccountSwanRepository swanRepository) {
    when(swanRepository.findById(JOE_DOE_ACCOUNT_ID)).thenReturn(List.of(joeDoeSwanAccount()));
    when(swanRepository.findById(JOE_DOE_ID)).thenReturn(List.of(joeDoeSwanAccount()));
    when(swanRepository.findByBearer(JOE_DOE_TOKEN)).thenReturn(List.of(joeDoeSwanAccount()));
    when(swanRepository.findByUserId(JOE_DOE_ID)).thenReturn(List.of(joeDoeSwanAccount()));

    when(swanRepository.findById(BERNARD_DOE_ACCOUNT_ID)).thenReturn(
        List.of(bernardDoeSwanAccount()));
    when(swanRepository.findById(BERNARD_DOE_ID)).thenReturn(List.of(bernardDoeSwanAccount()));
    when(swanRepository.findByBearer(BERNARD_DOE_TOKEN)).thenReturn(
        List.of(bernardDoeSwanAccount()));
    when(swanRepository.findByUserId(BERNARD_DOE_ID)).thenReturn(List.of(bernardDoeSwanAccount()));

    when(swanRepository.findById(JANE_ACCOUNT_ID)).thenReturn(List.of(janeSwanAccount()));
    when(swanRepository.findByBearer(JANE_DOE_TOKEN)).thenReturn(List.of(janeSwanAccount()));
    when(swanRepository.findByUserId(JANE_DOE_ID)).thenReturn(List.of(janeSwanAccount()));
  }

  public static void setUpTransactionRepository(TransactionSwanRepository repository) {
    when(repository.getByIdAccount(any(), any())).thenReturn(
        List.of(
            swanTransaction1(),
            swanTransaction2(),
            swanTransaction3(),
            swanTransaction4()));
    when(repository.findById(eq(swanTransaction1().getNode().getId()), any()))
        .thenReturn(swanTransaction1());
    when(repository.findById(eq(swanTransaction2().getNode().getId()), any()))
        .thenReturn(swanTransaction2());
    when(repository.findById(eq(swanTransaction3().getNode().getId()), any()))
        .thenReturn(swanTransaction3());
    when(repository.findById(eq(swanTransaction4().getNode().getId()), any()))
        .thenReturn(swanTransaction4());
  }

  public static void setUpOnboardingSwanRepositoryMock(OnboardingSwanRepository repository) {
    when(repository.getOnboardingUrl(REDIRECT_SUCCESS_URL)).thenReturn(SWAN_ONBOARDING_URL_FORMAT);
  }

  public static void setUpAccountHolderSwanRep(AccountHolderSwanRepository swanRepository) {
    when(swanRepository.getById(JOE_DOE_ID)).thenReturn(joeDoeSwanAccountHolder());
    when(swanRepository.findAllByBearerAndAccountId(JOE_DOE_TOKEN, JOE_DOE_ACCOUNT_ID))
        .thenReturn(List.of(joeDoeSwanAccountHolder()));
    when(swanRepository.findAllByAccountId(JOE_DOE_ACCOUNT_ID))
        .thenReturn(List.of(joeDoeSwanAccountHolder()));

    when(swanRepository.getById(BERNARD_DOE_ID)).thenReturn(bernardDoeSwanAccountHolder());
    when(swanRepository.findAllByBearerAndAccountId(BERNARD_DOE_TOKEN, BERNARD_DOE_ACCOUNT_ID))
        .thenReturn(List.of(bernardDoeSwanAccountHolder()));
    when(swanRepository.findAllByAccountId(BERNARD_DOE_ACCOUNT_ID))
        .thenReturn(List.of(bernardDoeSwanAccountHolder()));
  }

  public static void setUpPaymentInitiationRep(FintecturePaymentInitiationRepository repository) {
    when(repository.save(any(FPaymentInitiation.class), any()))
        .thenAnswer(invocation ->
            FPaymentRedirection.builder()
                .meta(FPaymentRedirection.Meta.builder()
                    .sessionId(SESSION1_ID)
                    .url("https://connect-v2-sbx.fintecture.com")
                    .build())
                .build()
        )
        .thenAnswer(invocation ->
            FPaymentRedirection.builder()
                .meta(FPaymentRedirection.Meta.builder()
                    .sessionId(SESSION2_ID)
                    .url("https://connect-v2-sbx.fintecture.com")
                    .build())
                .build()
        );
  }

  public static void setUpPaymentInfoRepository(FintecturePaymentInfoRepository repository) {
    when(repository.getPaymentBySessionId(any(String.class)))
        .thenReturn(fintectureSession());
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

  public static void setUpLegalFileRepository(LegalFileRepository legalFileRepositoryMock) {
    when(legalFileRepositoryMock.findAllToBeApprovedLegalFilesByUserId(JOE_DOE_ID))
        .thenReturn(List.of(domainApprovedLegalFile()));
    when(legalFileRepositoryMock.findAllToBeApprovedLegalFilesByUserId(JANE_DOE_ID))
        .thenReturn(List.of(domainApprovedLegalFile()));
    when(legalFileRepositoryMock.findAllToBeApprovedLegalFilesByUserId(BERNARD_DOE_ID))
        .thenReturn(List.of(domainApprovedLegalFile()));
  }

  public static void setUpAccountRepository(AccountRepository accountRepository) {
    when(accountRepository.findById(any())).thenReturn(joeModelAccount());
    when(accountRepository.findByBearer(any())).thenReturn(List.of(joeModelAccount()));
    when(accountRepository.findByUserId(any())).thenReturn(List.of(joeModelAccount()));
    when(accountRepository.saveAll(anyList(), any())).thenReturn(List.of(joeModelAccount()));
  }

  private static Account joeModelAccount() {
    return Account.builder()
        .id(joeDoeSwanAccount().getId())
        .userId("joe_doe_id")
        .name(joeDoeSwanAccount().getName())
        .iban(joeDoeSwanAccount().getIban())
        .bic(joeDoeSwanAccount().getBic())
        .availableBalance(
            parseFraction(joeDoeSwanAccount().getBalances().getAvailable().getValue()))
        .status(OPENED)
        .build();
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

  public static Map<String, String> adsFilter() {
    Map<String, String> filter = new HashMap<>();
    filter.put("insee[like]", "8");
    filter.put("annee[gte]", String.valueOf(Year.now().minusYears(1).getValue()));
    filter.put("type[in]", "PC,PA");
    return filter;
  }

  public static boolean isAfterOrEquals(Instant before, Instant after) {
    return before.isAfter(after) || before.equals(after);
  }

  @SneakyThrows
  public static <T> Optional<T> getOptionalFutureResult(Future<T> future) {
    try {
      return Optional.of(future.get());
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
