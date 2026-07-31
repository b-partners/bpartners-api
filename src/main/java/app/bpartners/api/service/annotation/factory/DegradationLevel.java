package app.bpartners.api.service.annotation.factory;

import app.bpartners.api.service.annotation.AreaAnnotationExportPayload;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DegradationLevel {
  A("Toiture en bon état", "Aucune intervention visible nécessaire", "ti-shield-check", "#3B6D11"),
  B("Entretien préventif", "Pour garder la toiture en bonne santé", "ti-tool", "#639922"),
  C("Intervention nécessaire", "Pour ralentir le vieillissement", "ti-adjustments", "#BA7517"),
  D(
      "Réparation prioritaire",
      "Dégradation visible, risque à traiter",
      "ti-alert-triangle",
      "#D85A30"),
  E("Risque critique", "Intervention urgente à prévoir", "ti-flame", "#A32D2D");

  private final String label;
  private final String subtitle;
  private final String icon;
  private final String color;

  public static DegradationLevel valueOf(AreaAnnotationExportPayload annotation) {
    var type = annotation.getGlobalRateType();

    if (type == null) return null;

    return DegradationLevel.valueOf(type.toUpperCase());
  }
}
