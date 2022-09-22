package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = ProductIT.ContextInitializer.class)
@AutoConfigureMockMvc
class ProductIT {
  @MockBean
  private SentryConf sentryConf;
  @Value("${test.user.access.token}")
  private String bearerToken;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ProductIT.ContextInitializer.SERVER_PORT);
  }

  Product product1() {
    return new Product()
        .id("product1_id")
        .description("Tableau malgache")
        .quantity(1)
        .unitPrice(1000)
        .vatPercent(2000)
        .totalVat(200)
        .totalPriceWithVat(1200);
  }

  Product product2() {
    return new Product()
        .id("product2_id")
        .description("Plomberie")
        .quantity(2)
        .unitPrice(2000)
        .vatPercent(1000)
        .totalVat(400)
        .totalPriceWithVat(4400);
  }

  Product product3() {
    return new Product()
        .id("product3_id")
        .description("Plomberie")
        .quantity(3)
        .unitPrice(2000)
        .vatPercent(1000)
        .totalVat(600)
        .totalPriceWithVat(6600);
  }

  Product product4() {
    return new Product()
        .id("product4_id")
        .description("Tableau malgache")
        .quantity(1)
        .unitPrice(2000)
        .vatPercent(1000)
        .totalVat(200)
        .totalPriceWithVat(2200);
  }

  @Test
  void read_products_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    PayingApi api = new PayingApi(joeDoeClient);

    List<Product> actualNotUnique = api.getProducts(JOE_DOE_ACCOUNT_ID, false);
    List<Product> actualUnique = api.getProducts(JOE_DOE_ACCOUNT_ID, true);

    assertEquals(4, actualNotUnique.size());
    assertEquals(2, actualUnique.size());
    assertTrue(actualUnique.contains(product4()));
    assertTrue(actualUnique.contains(product3()));
    assertTrue(actualNotUnique.containsAll(actualUnique));
    assertTrue(actualNotUnique.contains(product1()));
    assertTrue(actualNotUnique.contains(product2()));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
