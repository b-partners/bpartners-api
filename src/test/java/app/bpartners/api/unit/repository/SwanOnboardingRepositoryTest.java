package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.swan.implementation.OnboardingSwanRepositoryImpl;
import app.bpartners.api.repository.swan.response.OnboardingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static app.bpartners.api.integration.conf.TestUtils.API_URL;
import static app.bpartners.api.integration.conf.TestUtils.PROJECT_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.REDIRECT_SUCCESS_URL;
import static app.bpartners.api.integration.conf.TestUtils.SWAN_ONBOARDING_URL_FORMAT;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class SwanOnboardingRepositoryTest {
  ProjectTokenManager projectTokenManager;
  SwanConf swanConf;
  OnboardingSwanRepositoryImpl onboardingSwanRepository;

  @BeforeEach
  void setUp() {
    projectTokenManager = mock(ProjectTokenManager.class);
    swanConf = mock(SwanConf.class);
    onboardingSwanRepository = new OnboardingSwanRepositoryImpl(swanConf, projectTokenManager);

    when(swanConf.getApiUrl()).thenReturn(API_URL);
    when(projectTokenManager.getSwanProjecToken()).thenReturn(PROJECT_TOKEN);
  }

  @Test
  void read_swan_onboarding_url_ok() {
    MockedConstruction<OnboardingResponse> mockedConstruction =
        mockConstruction(OnboardingResponse.class,
            (mock, context) -> when(mock.getData()).thenReturn(onboardingData()));
    String actual =
        onboardingSwanRepository.getOnboardingUrl(REDIRECT_SUCCESS_URL);

    assertNotNull(actual);
    mockedConstruction.close();
  }

  OnboardingResponse.Data onboardingData() {
    return OnboardingResponse.Data.builder()
        .onboardCompanyAccountHolder(new OnboardingResponse.OnboardCompanyAccountHolder(
            new OnboardingResponse.Onboarding(SWAN_ONBOARDING_URL_FORMAT)))
        .build();
  }
}
