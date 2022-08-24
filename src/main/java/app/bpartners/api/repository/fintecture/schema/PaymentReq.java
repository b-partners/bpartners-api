package app.bpartners.api.repository.fintecture.schema;

public class PaymentReq {
  public Meta meta;
  public Data data;

  public static class Meta {
    public String psu_name;
    public String psu_email;
  }


  public static class Data {
    public final String type = "payments";
    public Attributes attributes;
  }

  public static class Attributes {
    public String amount;
    public final String currency = "EUR";
    public String communication;
    public Beneficiary beneficiary;
  }
}
