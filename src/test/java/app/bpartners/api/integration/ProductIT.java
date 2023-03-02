package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.OrderDirection;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.model.ProductStatus;
import app.bpartners.api.endpoint.rest.model.UpdateProductStatus;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentInfoRepository;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.BEARER_PREFIX;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.isAfterOrEquals;
import static app.bpartners.api.integration.conf.TestUtils.product1;
import static app.bpartners.api.integration.conf.TestUtils.product4;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpPaymentInfoRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = ProductIT.ContextInitializer.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class ProductIT {
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
  @MockBean
  private S3Conf s3Conf;
  @MockBean
  private SwanConf swanConf;
  @MockBean
  private FintectureConf fintectureConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private UserSwanRepository userSwanRepositoryMock;
  @MockBean
  private AccountSwanRepository accountSwanRepositoryMock;
  @MockBean
  private AccountHolderSwanRepository accountHolderRepositoryMock;
  @MockBean
  private SwanComponent swanComponentMock;
  @MockBean
  private FintecturePaymentInitiationRepository paymentInitiationRepositoryMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;
  @MockBean
  private FintecturePaymentInfoRepository paymentInfoRepositoryMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, ProductIT.ContextInitializer.SERVER_PORT);
  }

  private static Product updatedProduct(Product product) {
    return new Product()
        .id(product.getId())
        .createdAt(product.getCreatedAt())
        .description("Other")
        .unitPrice(5000)
        .vatPercent(1000)
        .status(ProductStatus.ENABLED)
        .unitPriceWithVat(5500);
  }

  private static Product oldProduct(Product product) {
    return new Product()
        .description("Nouveau produit")
        .id(product.getId())
        .createdAt(product.getCreatedAt())
        .vatPercent(1000)
        .unitPrice(9000)
        .status(ProductStatus.ENABLED)
        .unitPriceWithVat(9900);
  }

  private static UpdateProductStatus productDisabled() {
    return new UpdateProductStatus()
        .id("product1_id")
        .status(ProductStatus.DISABLED);
  }

  @BeforeEach
  public void setUp() {
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderRepositoryMock);
    setUpSwanComponent(swanComponentMock);
    setUpPaymentInitiationRep(paymentInitiationRepositoryMock);
    setUpPaymentInfoRepository(paymentInfoRepositoryMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  CreateProduct createProduct1() {
    return new CreateProduct()
        .description("Nouveau produit")
        .quantity(1)
        .unitPrice(9000)
        .vatPercent(1000);
  }

  @Order(1)
  @Test
  void read_products_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Product> actual = api.getProducts(
        JOE_DOE_ACCOUNT_ID, null, null, null, null,
        null, null, 1, 20);

    assertEquals(6, actual.size());
    assertTrue(actual.stream()
        .allMatch(product -> product.getStatus() == ProductStatus.ENABLED));
    assertTrue(actual.contains(product1()));
    assertTrue(isAfterOrEquals(actual.get(0).getCreatedAt(), actual.get(1).getCreatedAt()));
    assertTrue(isAfterOrEquals(actual.get(1).getCreatedAt(), actual.get(2).getCreatedAt()));
    assertTrue(isAfterOrEquals(actual.get(3).getCreatedAt(), actual.get(4).getCreatedAt()));
    assertTrue(isAfterOrEquals(actual.get(4).getCreatedAt(), actual.get(5).getCreatedAt()));
  }

  @Order(1)
  @Test
  void search_product_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Product> actualSearchedByDescription = api.getProducts(
        JOE_DOE_ACCOUNT_ID, null, null, null, null,
        "Tableau malgache", null, 1, 20);
    List<Product> actualSearchedByUnitPrice = api.getProducts(
        JOE_DOE_ACCOUNT_ID, null, null, null, null,
        null, 1000, 1, 20);
    List<Product> actualSearchedByDescriptionAndUnitPrice = api.getProducts(
        JOE_DOE_ACCOUNT_ID, null, null, null, null,
        "Autres produits", 2000, 1, 20);

    assertEquals(1, actualSearchedByDescription.size());
    assertEquals("Tableau malgache", actualSearchedByDescription.get(0).getDescription());
    assertEquals(3, actualSearchedByUnitPrice.size());
    assertTrue(actualSearchedByUnitPrice.contains(product1()));
    assertEquals(1, actualSearchedByDescriptionAndUnitPrice.size());
    assertEquals(product4()
            .quantity(null)
            .totalVat(null)
            .totalPriceWithVat(null)
            .createdAt(Instant.parse("2022-01-01T04:00:00.00Z")),
        actualSearchedByDescriptionAndUnitPrice.get(0));

  }

  @Order(2)
  @Test
  void create_products_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Product> actual =
        api.createProducts(JOE_DOE_ACCOUNT_ID, List.of(createProduct1()));
    List<Product> actualProducts = api.getProducts(
        JOE_DOE_ACCOUNT_ID, true, null, null, null,
        null, null, 1, 20);
    assertTrue(actualProducts.stream()
        .allMatch(product -> product.getCreatedAt() != null));
    actual.get(0).createdAt(actualProducts.get(0).getCreatedAt());
    assertTrue(actualProducts.containsAll(actual));
    assertTrue(ignoreCreatedAt(actualProducts).containsAll(ignoreCreatedAt(actual)));
  }

  @Order(3)
  @Test
  void create_and_update_products_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    CreateProduct createProduct = createProduct1().id(String.valueOf(randomUUID()));

    List<Product> actual1 = api.crupdateProducts(JOE_DOE_ACCOUNT_ID, List.of(createProduct));
    List<Product> allProducts1 = api.getProducts(
        JOE_DOE_ACCOUNT_ID, true, null, null, null,
        null, null, 1, 20);
    List<Product> actual2 = api.crupdateProducts(JOE_DOE_ACCOUNT_ID,
        List.of(createProduct
            .description("Other")
            .unitPrice(5000)));
    List<Product> allProducts2 = api.getProducts(
        JOE_DOE_ACCOUNT_ID, true, null, null, null,
        null, null, 1, 20);

    Product actualProduct = actual1.get(0);
    Product actualUpdated = actual2.get(0);
    Product oldProduct = oldProduct(actualProduct);
    Product expectedProduct = updatedProduct(actualProduct);
    assertTrue(ignoreCreatedAt(allProducts1).containsAll(ignoreCreatedAt(actual1)));
    assertTrue(ignoreCreatedAt(allProducts2).containsAll(ignoreCreatedAt(actual2)));
    assertEquals(actualProduct.getId(), actualUpdated.getId());
    assertEquals(actualProduct.getCreatedAt(), actualUpdated.getCreatedAt());
    oldProduct.setCreatedAt(null);
    expectedProduct.setCreatedAt(null);
    assertEquals(oldProduct, actualProduct);
    assertEquals(expectedProduct, actualUpdated);
  }

  @Test
  void read_products_ordered_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Product> actual1 = api.getProducts(
        JOE_DOE_ACCOUNT_ID, null, null, null, OrderDirection.DESC,
        null, null, 1, 20);
    List<Product> actual2 = api.getProducts(
        JOE_DOE_ACCOUNT_ID, null,
        null, OrderDirection.ASC, null,
        null, null, 1, 20);
    List<Product> actual3 = api.getProducts(
        JOE_DOE_ACCOUNT_ID, null, OrderDirection.ASC, OrderDirection.DESC, null,
        null, null, 1, 20);

    assertTrue(actual1.size() > 2);
    assertTrue(actual2.size() > 2);
    assertTrue(actual3.size() > 2);
    Product product1 = actual1.get(0);
    Product product2 = actual1.get(1);
    Product product3 = actual2.get(0);
    Product product4 = actual2.get(1);
    Product product5 = actual3.get(0);
    Product product6 = actual3.get(1);
    assertTrue(product1.getCreatedAt().isAfter(product2.getCreatedAt())
        || product1.getCreatedAt().equals(product2.getCreatedAt()));
    assertTrue(product3.getUnitPrice() <= product4.getUnitPrice());
    // /!\ it seems by default, the description order ASC is taken before the unit price ASC
    // Pay attention with multiple orders then
    assertTrue((product5.getUnitPrice() >= product6.getUnitPrice())
        && (product5.getDescription().compareTo(product6.getDescription()) <= 0));
  }

  @Test
  void create_products_from_an_uploaded_excel_file_ok()
      throws InterruptedException, IOException {
    Resource fileToUpload = new ClassPathResource("files/products.xlsx");

    HttpResponse<String> response = uploadFile(JOE_DOE_ACCOUNT_ID, fileToUpload.getFile());
    CollectionType productListType = new ObjectMapper().getTypeFactory()
        .constructCollectionType(List.class, Product.class);
    List<Product> actual = new ObjectMapper().findAndRegisterModules()
        .readValue(response.body(), productListType);

    assertEquals(HttpStatus.OK.value(), response.statusCode());
    assertNotNull(actual);
    assertEquals(4, actual.size()); //All duplicate lines in the file are removed
  }

  @Test
  void create_products_from_an_uploaded_excel_file_ko()
      throws InterruptedException, IOException {
    Resource fileToUpload = new ClassPathResource("files/wrong.xlsx");

    HttpResponse<String> response = uploadFile(JOE_DOE_ACCOUNT_ID, fileToUpload.getFile());

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
    assertEquals(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\""
            + "\"Description\" instead of \"Autres\" at column 1. "
            + "\"Quantité\" instead of \"Quantity\" at column 2. "
            + "\"Prix unitaire (€)\" instead of \"unitPrice\" at column 3. "
            + "\"TVA (%)\" instead of \"vatPercent\" at the last column.\"}"
        , response.body().replace("\\", ""));
  }

  private HttpResponse<String> uploadFile(String accountId, File toUpload)
      throws InterruptedException, IOException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + ProductIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(
                basePath + "/accounts/" + accountId + "/products/upload"))
            .header("Authorization", BEARER_PREFIX + JOE_DOE_TOKEN)
            .method("POST", HttpRequest.BodyPublishers.ofFile(toUpload.toPath())).build(),
        HttpResponse.BodyHandlers.ofString());

    return response;
  }

  @Order(4)
  @Test
  void product_status_is_disabled_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Product> actual =
        api.updateProductsStatus(JOE_DOE_ACCOUNT_ID, List.of(productDisabled()));

    List<Product> allProducts = api.getProducts(
        JOE_DOE_ACCOUNT_ID, null, null, null, null,
        null, null, 1, 20);

    assertTrue(
        allProducts.stream()
            .allMatch(product -> product.getStatus() == ProductStatus.ENABLED));
    assertTrue(
        actual.stream()
            .allMatch(product -> product.getStatus() == ProductStatus.DISABLED));
    assertFalse(allProducts.containsAll(actual));

  }

  List<Product> ignoreCreatedAt(List<Product> actual) {
    actual.forEach(product -> {
      product.setCreatedAt(null);
    });
    return actual;
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
