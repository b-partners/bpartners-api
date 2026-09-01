package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.ACCOUNTHOLDER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.PROSPECT_1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpUserSubscription;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.api.ProspectingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.model.CreateProspectAnalyse;
import app.bpartners.api.endpoint.rest.model.ProspectAnalyse;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ProspectAnalyseIT extends MockedThirdParties {
  private static final String UNKNOWN_ANALYSE_ID = "unknown_analyse_id";

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort);
  }

  @BeforeEach
  void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    setUpUserSubscription(subscriptionService);
  }

  @Test
  void create_get_and_update_analyse_ok() throws Exception {
    ProspectingApi api = new ProspectingApi(anApiClient());
    var createPayload = new CreateProspectAnalyse().metadata(Map.of("score", "10"));

    ProspectAnalyse created = api.createProspectAnalyse(ACCOUNTHOLDER_ID, PROSPECT_1_ID, createPayload);

    assertEquals(Map.of("score", "10"), created.getMetadata());
    assertEquals(PROSPECT_1_ID, created.getProspect().getId());

    List<ProspectAnalyse> analyses = api.getProspectAnalyses(ACCOUNTHOLDER_ID, PROSPECT_1_ID);
    assertTrue(analyses.stream().anyMatch(analyse -> analyse.getId().equals(created.getId())));

    ProspectAnalyse fetched =
        api.getProspectAnalyseById(ACCOUNTHOLDER_ID, PROSPECT_1_ID, created.getId());
    assertEquals(created.getId(), fetched.getId());

    ProspectAnalyse updated =
        api.updateProspectAnalyse(
            ACCOUNTHOLDER_ID,
            PROSPECT_1_ID,
            created.getId(),
            new CreateProspectAnalyse().metadata(Map.of("score", "20")));
    assertEquals(Map.of("score", "20"), updated.getMetadata());
  }

  @Test
  void create_analyse_without_metadata_ko() {
    ProspectingApi api = new ProspectingApi(anApiClient());

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Metadata is mandatory and must not be empty.\"}",
        () ->
            api.createProspectAnalyse(
                ACCOUNTHOLDER_ID, PROSPECT_1_ID, new CreateProspectAnalyse().metadata(Map.of())));
  }

  @Test
  void get_unknown_analyse_by_id_ko() {
    ProspectingApi api = new ProspectingApi(anApiClient());

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"ProspectAnalyse(id="
            + UNKNOWN_ANALYSE_ID
            + ") not found\"}",
        () -> api.getProspectAnalyseById(ACCOUNTHOLDER_ID, PROSPECT_1_ID, UNKNOWN_ANALYSE_ID));
  }
}
