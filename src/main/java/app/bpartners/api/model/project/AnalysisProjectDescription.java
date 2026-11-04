package app.bpartners.api.model.project;

public record AnalysisProjectDescription(
    String detectionIdentifier,
    String zone,
    String address,
    String initiatorName,
    String initiatorEmail,
    String initiatorPhoneNumber) {

  public static AnalysisProjectDescription ofAddress(String address) {
    return new AnalysisProjectDescription(null, null, address, null, null, null);
  }
}
