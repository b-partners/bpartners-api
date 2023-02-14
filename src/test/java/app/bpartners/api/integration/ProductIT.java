package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.Product;
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
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.product1;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpPaymentInfoRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

  CreateProduct createProduct2() {
    return new CreateProduct()
        .description("test produit")
        .quantity(1)
        .unitPrice(9000)
        .vatPercent(1000);
  }

  CreateProduct updateProduct2() {
    return new CreateProduct()
        .id("product6_id")
        .description("last test")
        .quantity(1)
        .unitPrice(95000)
        .vatPercent(1000);
  }

  @Order(1)
  @Test
  void read_products_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Product> actual = api.getProducts(JOE_DOE_ACCOUNT_ID, null, null, null, null);

    assertEquals(6, actual.size());
    assertTrue(actual.contains(product1()));
  }

  @Order(2)
  @Test
  void create_products_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Product> actual =
        api.createProducts(JOE_DOE_ACCOUNT_ID, List.of(createProduct1()));

    List<Product> actualProducts = api.getProducts(JOE_DOE_ACCOUNT_ID, true, null, null, null);
    assertTrue(actualProducts.stream()
        .allMatch(product -> product.getCreatedAt() != null));
    assertTrue(actualProducts.containsAll(actual));
  }

  @Order(3)
  @Test
  void create_and_update_products_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    CreateProduct createProduct = createProduct1().id(String.valueOf(randomUUID()));

    List<Product> actual1 = api.crupdateProducts(JOE_DOE_ACCOUNT_ID, List.of(createProduct));
    List<Product> allProducts1 = api.getProducts(
        JOE_DOE_ACCOUNT_ID, true, null, null, null);
    List<Product> actual2 = api.crupdateProducts(JOE_DOE_ACCOUNT_ID,
        List.of(createProduct
            .description("Other")
            .unitPrice(5000)));
    List<Product> allProducts2 = api.getProducts(
        JOE_DOE_ACCOUNT_ID, true, null, null, null);

    Product actualProduct = actual1.get(0);
    Product actualUpdated = actual2.get(0);
    Product oldProduct = oldProduct(actualProduct);
    Product expectedProduct = updatedProduct(actualProduct);
    assertTrue(allProducts1.containsAll(actual1));
    assertTrue(allProducts2.containsAll(actual2));
    assertEquals(actualProduct.getId(), actualUpdated.getId());
    assertEquals(actualProduct.getCreatedAt(), actualUpdated.getCreatedAt());
    assertEquals(oldProduct, actualProduct);
    assertEquals(expectedProduct, actualUpdated);
  }

  private static Product updatedProduct(Product product) {
    return new Product()
        .id(product.getId())
        .createdAt(product.getCreatedAt())
        .description("Other")
        .unitPrice(5000)
        .vatPercent(1000)
        .unitPriceWithVat(5500);
  }

  private static Product oldProduct(Product product) {
    return new Product()
        .description("Nouveau produit")
        .id(product.getId())
        .createdAt(product.getCreatedAt())
        .vatPercent(1000)
        .unitPrice(9000)
        .unitPriceWithVat(9900);
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
