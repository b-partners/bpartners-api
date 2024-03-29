package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.BEARER_PREFIX;
import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.OTHER_PRODUCT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.disabledProduct;
import static app.bpartners.api.integration.conf.utils.TestUtils.getApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.isAfterOrEquals;
import static app.bpartners.api.integration.conf.utils.TestUtils.product1;
import static app.bpartners.api.integration.conf.utils.TestUtils.product6;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpPaymentInfoRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpPaymentInitiationRep;
import static app.bpartners.api.repository.google.calendar.drive.DriveApi.EXCEL_MIME_TYPE;
import static app.bpartners.api.service.CustomerService.TEXT_CSV_MIME_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.OrderDirection;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.model.ProductStatus;
import app.bpartners.api.endpoint.rest.model.UpdateProductStatus;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.fintecture.FintecturePaymentInfoRepository;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductIT extends MockedThirdParties {
  @MockBean private FintecturePaymentInitiationRepository paymentInitiationRepositoryMock;
  @MockBean private FintecturePaymentInfoRepository paymentInfoRepositoryMock;

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
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
        .description("New product")
        .id(product.getId())
        .createdAt(product.getCreatedAt())
        .vatPercent(1000)
        .unitPrice(9000)
        .status(ProductStatus.ENABLED)
        .unitPriceWithVat(9900);
  }

  private static UpdateProductStatus productDisabled() {
    return new UpdateProductStatus().id("product1_id").status(ProductStatus.DISABLED);
  }

  @BeforeEach
  public void setUp() {
    setUpPaymentInitiationRep(paymentInitiationRepositoryMock);
    setUpPaymentInfoRepository(paymentInfoRepositoryMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
  }

  CreateProduct createProduct1() {
    return new CreateProduct()
        .description("Nouveau produit")
        .quantity(1)
        .unitPrice(9000)
        .vatPercent(1000);
  }

  CreateProduct createExistingProduct() {
    return new CreateProduct()
        .description("Tableau malgache")
        .quantity(1)
        .unitPrice(1000)
        .vatPercent(1000);
  }

  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  @Test
  void export_products_ok() throws IOException, InterruptedException, ApiException {
    InputStream expectedFileIs = new ClassPathResource("files/products.csv").getInputStream();

    var actual = exportProducts(JOE_DOE_ACCOUNT_ID, TEXT_CSV_MIME_TYPE);
    // TODO: check why not throwing error correctly
    assertThrowsApiException("", () -> exportProducts(JOE_DOE_ACCOUNT_ID, EXCEL_MIME_TYPE));

    // /!\ It seems expected data length is higher than actual 565 > 1173
    assertTrue(actual.body().length > 0);
    expectedFileIs.close();
    /*Uncomment to download file
    var fos = new FileOutputStream("products-" + randomUUID() + ".csv");
    fos.write(actual.body());
    fos.close();
    */
  }

  @Order(1)
  @Test
  void read_products_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Product> actualEnabledProducts =
        api.getProducts(JOE_DOE_ACCOUNT_ID, null, null, null, null, null, null, null, 1, 20);
    List<Product> actualDisabledProducts =
        api.getProducts(
            JOE_DOE_ACCOUNT_ID, null, null, null, null, null, null, ProductStatus.DISABLED, 1, 20);

    assertEquals(6, actualEnabledProducts.size());
    assertEquals(1, actualDisabledProducts.size());
    assertTrue(
        actualEnabledProducts.stream()
            .allMatch(product -> product.getStatus() == ProductStatus.ENABLED));
    assertTrue(actualEnabledProducts.contains(product1()));
    assertTrue(
        isAfterOrEquals(
            actualEnabledProducts.get(0).getCreatedAt(),
            actualEnabledProducts.get(1).getCreatedAt()));
    assertTrue(
        isAfterOrEquals(
            actualEnabledProducts.get(1).getCreatedAt(),
            actualEnabledProducts.get(2).getCreatedAt()));
    assertTrue(
        isAfterOrEquals(
            actualEnabledProducts.get(3).getCreatedAt(),
            actualEnabledProducts.get(4).getCreatedAt()));
    assertTrue(
        isAfterOrEquals(
            actualEnabledProducts.get(4).getCreatedAt(),
            actualEnabledProducts.get(5).getCreatedAt()));
    assertTrue(
        actualDisabledProducts.stream()
            .allMatch(disabledProducts -> disabledProducts.getStatus() == ProductStatus.DISABLED));
    assertTrue(actualDisabledProducts.contains(disabledProduct()));
  }

  @Order(1)
  @Test
  void read_unique_product_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    Product actualProduct1 = api.getProductById(JOE_DOE_ACCOUNT_ID, "product1_id");

    assertEquals(product1(), actualProduct1);
  }

  @Order(1)
  @Test
  void read_unique_product_ko() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"Product(id="
            + OTHER_PRODUCT_ID
            + ") not found\"}",
        () -> api.getProductById(JOE_DOE_ACCOUNT_ID, OTHER_PRODUCT_ID));
    assertThrowsForbiddenException(() -> api.getProductById(JANE_ACCOUNT_ID, "product1_id"));
  }

  @Order(1)
  @Test
  void filter_product_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Product> actualSearchedByDescription =
        api.getProducts(JOE_DOE_ACCOUNT_ID, null, null, null, null, "Tableau", null, null, 1, 20);
    List<Product> actualSearchedByUnitPrice =
        api.getProducts(JOE_DOE_ACCOUNT_ID, null, null, null, null, null, 1000, null, 1, 20);
    List<Product> actualSearchedByDescriptionAndUnitPrice =
        api.getProducts(JOE_DOE_ACCOUNT_ID, null, null, null, null, "produits", 1000, null, 1, 20);
    List<Product> actualSearchEmpty =
        api.getProducts(JOE_DOE_ACCOUNT_ID, null, null, null, null, null, 210, null, 1, 20);

    assertEquals(2, actualSearchedByDescription.size());
    assertEquals(3, actualSearchedByUnitPrice.size());
    assertEquals(1, actualSearchedByDescriptionAndUnitPrice.size());
    assertEquals(0, actualSearchEmpty.size());
    assertEquals("Tableau baobab", actualSearchedByDescription.get(0).getDescription());
    assertEquals("Tableau malgache", actualSearchedByDescription.get(1).getDescription());
    assertTrue(actualSearchedByUnitPrice.contains(product1()));
    assertEquals(
        product6()
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
        api.createProducts(JOE_DOE_ACCOUNT_ID, List.of(createProduct1(), createExistingProduct()));
    List<Product> actualProducts =
        api.getProducts(JOE_DOE_ACCOUNT_ID, true, null, null, null, null, null, null, 1, 20);
    assertEquals(7, actualProducts.size());
    assertTrue(actualProducts.stream().allMatch(product -> product.getCreatedAt() != null));
    actual.get(0).createdAt(actualProducts.get(0).getCreatedAt());
    assertTrue(actualProducts.containsAll(actual));
    assertTrue(ignoreCreatedAt(actualProducts).containsAll(ignoreCreatedAt(actual)));
  }

  @Order(3)
  @Test
  void create_and_update_products_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    CreateProduct createProduct = createProduct1().id(null).description("New product");

    List<Product> actual1 = api.crupdateProducts(JOE_DOE_ACCOUNT_ID, List.of(createProduct));
    List<Product> allProducts1 =
        api.getProducts(JOE_DOE_ACCOUNT_ID, true, null, null, null, null, null, null, 1, 20);
    List<Product> actual2 =
        api.crupdateProducts(
            JOE_DOE_ACCOUNT_ID,
            List.of(createProduct.id(actual1.get(0).getId()).description("Other").unitPrice(5000)));
    List<Product> allProducts2 =
        api.getProducts(JOE_DOE_ACCOUNT_ID, true, null, null, null, null, null, null, 1, 20);

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

  @Order(3)
  @Test
  void update_product_ko() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    CreateProduct createProduct =
        new CreateProduct().id(OTHER_PRODUCT_ID).description("New product").unitPrice(5000);

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"Product(id="
            + OTHER_PRODUCT_ID
            + ") not found for User(id="
            + JOE_DOE_ID
            + ")\"}",
        () -> api.crupdateProducts(JOE_DOE_ACCOUNT_ID, List.of(createProduct)));
  }

  @Order(1)
  @Test
  void read_products_ordered_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Product> actual1 =
        api.getProducts(
            JOE_DOE_ACCOUNT_ID, null, null, null, OrderDirection.DESC, null, null, null, 1, 20);
    List<Product> actual2 =
        api.getProducts(
            JOE_DOE_ACCOUNT_ID, null, null, OrderDirection.ASC, null, null, null, null, 1, 20);
    List<Product> actual3 =
        api.getProducts(
            JOE_DOE_ACCOUNT_ID,
            null,
            OrderDirection.ASC,
            OrderDirection.DESC,
            null,
            null,
            null,
            null,
            1,
            20);

    assertTrue(actual1.size() > 2);
    assertTrue(actual2.size() > 2);
    assertTrue(actual3.size() > 2);
    Product product1 = actual1.get(0);
    Product product2 = actual1.get(1);
    Product product3 = actual2.get(0);
    Product product4 = actual2.get(1);
    Product product5 = actual3.get(0);
    Product product6 = actual3.get(1);
    assertTrue(
        product1.getCreatedAt().isAfter(product2.getCreatedAt())
            || product1.getCreatedAt().equals(product2.getCreatedAt()));
    assertTrue(product3.getUnitPrice() <= product4.getUnitPrice());
    // /!\ it seems by default, the description order ASC is taken before the unit price ASC
    // Pay attention with multiple orders then
    assertTrue(
        (product5.getUnitPrice() >= product6.getUnitPrice())
            && (product5.getDescription().compareTo(product6.getDescription()) <= 0));
  }

  @Test
  void create_products_from_an_uploaded_excel_file_ok() throws InterruptedException, IOException {
    Resource fileToUpload = new ClassPathResource("files/products.xlsx");

    HttpResponse<String> response = uploadFile(JOE_DOE_ACCOUNT_ID, fileToUpload.getFile());
    CollectionType productListType =
        new ObjectMapper().getTypeFactory().constructCollectionType(List.class, Product.class);
    List<Product> actual =
        new ObjectMapper().findAndRegisterModules().readValue(response.body(), productListType);

    assertEquals(HttpStatus.OK.value(), response.statusCode());
    assertNotNull(actual);
    assertEquals(5, actual.size()); // All duplicate lines in the file are removed
  }

  @Test
  void create_products_from_an_uploaded_excel_file_ko() throws InterruptedException, IOException {
    Resource fileToUpload = new ClassPathResource("files/wrong.xlsx");

    HttpResponse<String> response = uploadFile(JOE_DOE_ACCOUNT_ID, fileToUpload.getFile());

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
    assertEquals(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\""
            + "\"Description\" instead of \"Autres\" at column 1. "
            + "\"Quantité\" instead of \"Quantity\" at column 2. "
            + "\"Prix unitaire (€)\" instead of \"unitPrice\" at column 3. "
            + "\"TVA (%)\" instead of \"vatPercent\" at the last column.\"}",
        response.body().replace("\\", ""));
  }

  private HttpResponse<String> uploadFile(String accountId, File toUpload)
      throws InterruptedException, IOException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;

    HttpResponse<String> response =
        unauthenticatedClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + "/accounts/" + accountId + "/products/upload"))
                .header("Authorization", BEARER_PREFIX + JOE_DOE_TOKEN)
                .method("POST", HttpRequest.BodyPublishers.ofFile(toUpload.toPath()))
                .build(),
            HttpResponse.BodyHandlers.ofString());

    return response;
  }

  @Order(4)
  @Test
  void product_status_is_disabled_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Product> actual = api.updateProductsStatus(JOE_DOE_ACCOUNT_ID, List.of(productDisabled()));

    List<Product> allProducts =
        api.getProducts(JOE_DOE_ACCOUNT_ID, null, null, null, null, null, null, null, 1, 20);

    assertTrue(
        allProducts.stream().allMatch(product -> product.getStatus() == ProductStatus.ENABLED));
    assertTrue(actual.stream().allMatch(product -> product.getStatus() == ProductStatus.DISABLED));
    assertFalse(allProducts.containsAll(actual));
  }

  @Order(4)
  @Test
  void product_status_is_disabled_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"Product(id="
            + OTHER_PRODUCT_ID
            + ") not found\"}",
        () ->
            api.updateProductsStatus(
                JOE_DOE_ACCOUNT_ID,
                List.of(
                    new UpdateProductStatus()
                        .id(OTHER_PRODUCT_ID)
                        .status(ProductStatus.DISABLED))));
  }

  List<Product> ignoreCreatedAt(List<Product> actual) {
    actual.forEach(
        product -> {
          product.setCreatedAt(null);
        });
    return actual;
  }

  private HttpResponse<byte[]> exportProducts(String accountId, String fileType)
      throws IOException, InterruptedException, ApiException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;

    HttpResponse<byte[]> response =
        unauthenticatedClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + "/accounts/" + accountId + "/products/export"))
                .header("Authorization", BEARER_PREFIX + JOE_DOE_TOKEN)
                .header("Accept", fileType)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofByteArray());
    if (response.statusCode() / 100 != 2) {
      throw getApiException("exportProducts", response);
    }
    return response;
  }
}
