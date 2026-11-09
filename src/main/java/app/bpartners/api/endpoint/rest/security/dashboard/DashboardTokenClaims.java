package app.bpartners.api.endpoint.rest.security.dashboard;

public final class DashboardTokenClaims {
  public static final String ISSUER = "bpartners-dashboard-api";
  public static final String SCOPE_CLAIM = "scope";
  public static final String API_BRIDGE_SCOPE = "api-bridge";
  public static final String API_BRIDGE_AUTHORITY = "SCOPE_API_BRIDGE";

  private DashboardTokenClaims() {}
}
