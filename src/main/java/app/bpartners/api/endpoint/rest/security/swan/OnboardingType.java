package app.bpartners.api.endpoint.rest.security.swan;

public enum OnboardingType {
  INDIVIDUAL, COMPANY;

  public String getType() {
    return name();
  }
}
