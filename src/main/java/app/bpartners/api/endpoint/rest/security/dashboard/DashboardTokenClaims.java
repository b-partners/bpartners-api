package app.bpartners.api.endpoint.rest.security.dashboard;

public final class DashboardTokenClaims {
  public static final String ISSUER = "bpartners-dashboard-api";
  public static final String SCOPE_CLAIM = "scope";
  public static final String BRIDGE_SCOPE = "bridge";
  public static final String BRIDGE_AUTHORITY = "SCOPE_BRIDGE";

  private DashboardTokenClaims() {}
}
