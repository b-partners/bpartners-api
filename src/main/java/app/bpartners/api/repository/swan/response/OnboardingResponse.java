package app.bpartners.api.repository.swan.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingResponse {
  @JsonProperty
  private Data data;

  @Getter
  @Setter
  public static class Data {
    @JsonProperty
    private OnboardCompanyAccountHolder onboardCompanyAccountHolder;
  }

  @Getter
  @Setter
  public static class OnboardCompanyAccountHolder {
    @JsonProperty
    private Onboarding onboarding;
  }

  @Getter
  @Setter
  public static class Onboarding {
    @JsonProperty
    private String onboardingUrl;
  }
}
