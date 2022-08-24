package app.bpartners.api.repository.fintecture.schema;

public class PaymentUrl {
  public Meta meta;

  public static class Meta {
    public String session_id;
    public String url;
  }
}
