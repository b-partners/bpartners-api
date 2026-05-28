package app.bpartners.api.service.annotation.model;

import static java.awt.Color.WHITE;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum RoofSlopeBoundaryType {
  INCONNU("inconnu", WHITE, new BasicStroke(1.5f)),

  FAITAGE("faitage", new Color(220, 20, 60), new BasicStroke(3f)),

  ARETIER("aretier", new Color(255, 140, 0), new BasicStroke(2f)),

  NOUE("noue", new Color(148, 0, 211), new BasicStroke(2f)),

  RIVE(
      "rive",
      new Color(34, 139, 34),
      new BasicStroke(
          2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {8f, 6f}, 0f)),

  EGOUT("egout", new Color(30, 144, 255), new BasicStroke(2f)),

  GOUTTIERE("gouttiere", new Color(0, 191, 255), new BasicStroke(
      2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {8f, 6f}, 0f)),

  DESCENTE_EP(
      "descente_ep",
      new Color(0, 0, 139),
      new BasicStroke(
          2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[] {4f, 4f}, 0f)),

  SOLIN("solin", new Color(255, 215, 0), new BasicStroke(2f)),

  ABERGEMENT(
      "abergement",
      new Color(255, 165, 0),
      new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL)),

  LIGNE_DE_VIE(
      "ligne_de_vie",
      new Color(199, 21, 133),
      new BasicStroke(
          2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[] {2f, 6f}, 0f)),

  GARDE_CORPS_TEMPORAIRE(
      "garde_corps_temporaire",
      new Color(255, 105, 180),
      new BasicStroke(
          2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10f, new float[] {10f, 4f}, 0f));

  private final String label;
  private final Color color;
  private final Stroke stroke;

  public static final RoofSlopeBoundaryType DEFAULT = INCONNU;

  public static RoofSlopeBoundaryType fromLabel(String label) {
    for (RoofSlopeBoundaryType type : values()) {
      if (type.label.replace("_", "-").equalsIgnoreCase(label)) {
        return type;
      }
    }
    log.warn(
        "Unknown RoofSlopeBoundaryType label: {}. Using default label {}", label, DEFAULT.label);

    return DEFAULT;
  }
}
