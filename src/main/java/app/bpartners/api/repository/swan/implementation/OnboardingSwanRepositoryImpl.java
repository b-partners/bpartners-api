package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.swan.OnboardingSwanRepository;
import app.bpartners.api.repository.swan.response.OnboardingResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.BEARER_PREFIX;

@Repository
@AllArgsConstructor
public class OnboardingSwanRepositoryImpl implements OnboardingSwanRepository {
  private final ProjectTokenManager tokenManager;
  private SwanConf swanConf;

  @Override
  public String getOnboardingUrl(String redirectUrl) {
    String query =
        "{ \"query\": \n"
            + "\"mutation Onboarding { onboardCompanyAccountHolder(input: { redirectUrl: "
            + "\\\"" + redirectUrl + "\\\"}) { ... on "
            + "OnboardCompanyAccountHolderSuccessPayload { onboarding { onboardingUrl } }}} \"\n"
            + "}";
    OnboardingResponse onboardingResponse;
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      HttpRequest request = HttpRequest.newBuilder().uri(new URI(swanConf.getApiUrl()))
          .header("Content-Type", "application/json")
          .header("Authorization", BEARER_PREFIX + tokenManager.getSwanProjecToken())
          .POST(HttpRequest.BodyPublishers.ofString(query)).build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      onboardingResponse = new ObjectMapper().readValue(response.body(),
          OnboardingResponse.class);
    } catch (IOException | InterruptedException | URISyntaxException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
    return onboardingResponse.getData()
        .getOnboardCompanyAccountHolder()
        .getOnboarding()
        .getOnboardingUrl();
  }
}
