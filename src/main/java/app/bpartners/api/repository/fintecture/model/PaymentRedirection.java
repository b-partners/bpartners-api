package app.bpartners.api.repository.fintecture.model;

public class PaymentRedirection {
  public Meta meta;

  public static class Meta {
    public String session_id;
    public String url;
  }
}
