package app.bpartners.api.repository.swan.model;

public class SwanAccountHolder {
  private String id;
  public Info info;
  public ResidencyAddress residencyAddress;

  public static class Info {
    public String name;
  }

  public static class ResidencyAddress {
    public String addressLine1;
    public String city;
    public String country;
    public String postalCode;
  }
}
