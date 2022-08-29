package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.api.swan.SwanApi;
import app.bpartners.api.repository.swan.OnboardingSwanRepository;
import app.bpartners.api.repository.swan.response.OnboardingResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class OnboardingSwanRepositoryImpl implements OnboardingSwanRepository {
  private final ProjectTokenManager tokenManager;
  private final SwanApi<OnboardingResponse> swanApi;


  @Override
  public String getOnboardingUrl(String redirectUrl) {
    String message =
        "{\"query\": \"mutation MyMutation { onboardCompanyAccountHolder( input: { redirectUrl:"
            + " \\\"" + redirectUrl + "\\\" }) "
            + "{ ... on OnboardCompanyAccountHolderSuccessPayload { onboarding "
            + "{ onboardingUrl } } }}\"}";
    OnboardingResponse onboardingResponse =
        swanApi.getData(message, tokenManager.getSwanProjecToken());
    if (onboardingResponse.getData().getOnboardCompanyAccountHolder().getOnboarding() == null) {
      throw new BadRequestException("Invalid redirect URL");
    }
    return onboardingResponse.getData().getOnboardCompanyAccountHolder().getOnboarding()
        .getOnboardingUrl();
  }
}
