package app.bpartners.api.repository.swan.response;



public class OnboardingResponse {
  public Data data;


  public static class Data {
    public OnboardCompanyAccountHolder onboardCompanyAccountHolder;
  }


  public static class OnboardCompanyAccountHolder {
    public Onboarding onboarding;
  }


  public static class Onboarding {
    public String onboardingUrl;
  }
}
