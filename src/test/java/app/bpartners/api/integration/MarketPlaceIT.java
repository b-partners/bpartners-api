package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.ProspectingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Marketplace;
import app.bpartners.api.integration.conf.DbEnvContextInitializer;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.MARKETPLACE1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.MARKETPLACE2_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.NOT_JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Testcontainers
class MarketPlaceIT extends MockedThirdParties {

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
  }

  Marketplace marketPlace1() {
    return new Marketplace()
        .id(MARKETPLACE1_ID)
        .phoneNumber("+261340465338")
        .name("marketplace1_name")
        .description("marketplace1_description")
        .websiteUrl("website URL")
        .logoUrl("logo URL");
  }

  Marketplace marketPlace2() {
    return new Marketplace()
        .id(MARKETPLACE2_ID)
        .phoneNumber("+261340465338")
        .name("marketplace2_name")
        .description("marketplace2_description")
        .websiteUrl("website URL")
        .logoUrl("logo URL");
  }

  Marketplace defaultMarketPlace1() {
    return new Marketplace()
        .id("428473e5-f075-48b5-9b7f-60035e103db5")
        .name("Marchés publics - Travaux")
        .websiteUrl(
            "https://www.marches-publics.gouv.fr/?page=Entreprise"
                + ".EntrepriseAdvancedSearch&searchAnnCons&type=multicriteres")
        .logoUrl(
            "https://public-logo-resources.s3.eu-west-3.amazonaws.com/place-logo.png");
  }

  Marketplace defaultMarketPlace2() {
    return new Marketplace()
        .id("e09290f0-34f9-40cd-b643-e8afd1a721b4")
        .name("Marchés publics - Services")
        .websiteUrl(
            "https://www.marches-publics.gouv.fr/?page=Entreprise"
                + ".EntrepriseAdvancedSearch&searchAnnCons&type=multicriteres")
        .logoUrl(
            "https://public-logo-resources.s3.eu-west-3.amazonaws.com/place-logo.png");
  }

  @Test
  void read_marketplaces_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    List<Marketplace> actual =
        ignoreDescription(api.getMarketplaces(JOE_DOE_ACCOUNT_ID, 1, 20));
    assertTrue(actual.containsAll(List.of(marketPlace1(), marketPlace2(), defaultMarketPlace1(),
        defaultMarketPlace2())));
  }

  @Test
  void read_marketplaces_ko() {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getMarketplaces(NOT_JOE_DOE_ACCOUNT_ID, 1, 20));
  }

  private List<Marketplace> ignoreDescription(List<Marketplace> marketplaces) {
    marketplaces.forEach(marketplace -> {
      if (marketplace.getId().equals(defaultMarketPlace1().getId())
          || marketplace.getId().equals(defaultMarketPlace2().getId())) {
        marketplace.setDescription(null);
      }
    });
    return marketplaces;
  }

}
