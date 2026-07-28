package app.bpartners.api.service.annotation.export;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.Font.PLAIN;
import static java.lang.Math.round;

import app.bpartners.api.model.annotation.IntXY;
import java.awt.*;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AreaAnnotationImageConf {
  private int scale;
  private int pointSize;
  private Stroke stroke;
  private Color pointColor;
  private Color measurementBgColor;
  private Color measurementTextColor;
  private IntXY measurementOffset;
  private Font measurementFont;

  public AreaAnnotationImageConf() {
    this.stroke = DEFAULT_STROKE;
    this.scale = DEFAULT_SCALE;
    this.pointSize = DEFAULT_POINT_SIZE;
    this.pointColor = DEFAULT_POINT_COLOR;
    this.measurementBgColor = DEFAULT_MEASUREMENT_BG_COLOR;
    this.measurementTextColor = DEFAULT_MEASUREMENT_TEXT_COLOR;
    this.measurementOffset = DEFAULT_MEASUREMENT_OFFSET;
    this.measurementFont = DEFAULT_MEASUREMENT_FONT;
  }

  public AreaAnnotationImageConf rescale(double xFactor, double yFactor) {
    var currentStroke = (BasicStroke) this.stroke;
    var factor = (xFactor + yFactor) / 2;
    return new AreaAnnotationImageConf(
        this.scale,
        Math.max(1, (int) round(this.pointSize * factor)),
        new BasicStroke(Math.max(1f, currentStroke.getLineWidth() * (float) factor)),
        this.pointColor,
        this.measurementBgColor,
        this.measurementTextColor,
        new IntXY(
            Math.max(1, (int) round(this.measurementOffset.x() * xFactor)),
            Math.max(1, (int) round(this.measurementOffset.y() * yFactor))),
        new Font(
            this.measurementFont.getName(),
            this.measurementFont.getStyle(),
            Math.max(1, (int) round(this.measurementFont.getSize() * factor))));
  }

  public static final int DEFAULT_SCALE = 3;
  public static final int DEFAULT_POINT_SIZE = 30;
  public static final float DEFAULT_STROKE_WIDTH = 3.2f;
  public static final Stroke DEFAULT_STROKE = new BasicStroke(DEFAULT_STROKE_WIDTH);
  public static final Color DEFAULT_POINT_COLOR = BLACK;
  public static final Color DEFAULT_MEASUREMENT_BG_COLOR = new Color(0, 0, 0, 150);
  public static final Color DEFAULT_MEASUREMENT_TEXT_COLOR = WHITE;
  public static final IntXY DEFAULT_MEASUREMENT_OFFSET = new IntXY(20, 18);
  public static final Font DEFAULT_MEASUREMENT_FONT = new Font("Arial", PLAIN, 70);
}
