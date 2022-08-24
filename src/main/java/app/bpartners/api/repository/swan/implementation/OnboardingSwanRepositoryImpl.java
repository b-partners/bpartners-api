package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
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
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Repository
@AllArgsConstructor
public class OnboardingSwanRepositoryImpl implements OnboardingSwanRepository {
  private final SwanConf swanConf;
  private final ProjectTokenManager tokenManager;


  @Override
  public String getOnboardingUrl(String redirectUrl) {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      String message =
          "{\"query\": \"mutation MyMutation { onboardCompanyAccountHolder( input: { redirectUrl:"
              + " \\\"" + redirectUrl + "\\\" }) "
              + "{ ... on OnboardCompanyAccountHolderSuccessPayload { onboarding "
              + "{ onboardingUrl } } }}\"}";
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(swanConf.getApiUrl()))
          .header("Content-Type", "application/json")
          .header("Authorization", BEARER_PREFIX + tokenManager.getSwanProjecToken())
          .POST(HttpRequest.BodyPublishers.ofString(message))
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      OnboardingResponse onboardingResponse = new ObjectMapper()
          .findAndRegisterModules() //Load DateTime Module
          .readValue(response.body(), OnboardingResponse.class);
      if (onboardingResponse.data.onboardCompanyAccountHolder.onboarding == null) {
        throw new BadRequestException("Invalid redirect URL");
      }
      return onboardingResponse.data.onboardCompanyAccountHolder.onboarding.onboardingUrl;
    } catch (IOException | InterruptedException | URISyntaxException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}
