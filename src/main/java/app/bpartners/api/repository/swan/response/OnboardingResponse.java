package app.bpartners.api.repository.swan.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OnboardingResponse {
  private Data data;
  private static final String JSON_PROPERTY_DATA = "data";

  @JsonProperty(JSON_PROPERTY_DATA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }

  public static class Data {
    private OnboardCompanyAccountHolder onboardCompanyAccountHolder;
    private static final String JSON_PROPERTY_ACCOUNT_HOLDER = "onboardCompanyAccountHolder";

    @JsonProperty(JSON_PROPERTY_ACCOUNT_HOLDER)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public OnboardCompanyAccountHolder getOnboardCompanyAccountHolder() {
      return onboardCompanyAccountHolder;
    }
  }

  public static class OnboardCompanyAccountHolder {
    private Onboarding onboarding;
    private static final String JSON_PROPERTY_ONBOARD = "onboarding";

    @JsonProperty(JSON_PROPERTY_ONBOARD)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Onboarding getOnboarding() {
      return onboarding;
    }
  }

  public static class Onboarding {
    private String onboardingUrl;
    private static final String JSON_PROPERTY_ONBOARDING_URL = "onboardingUrl";

    @JsonProperty(JSON_PROPERTY_ONBOARDING_URL)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getOnboardingUrl() {
      return onboardingUrl;
    }
  }
}
