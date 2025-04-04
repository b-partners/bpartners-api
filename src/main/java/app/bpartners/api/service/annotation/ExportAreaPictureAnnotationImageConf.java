package app.bpartners.api.service.annotation;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.Font.PLAIN;

import app.bpartners.api.model.annotation.IntXY;
import java.awt.*;

public record ExportAreaPictureAnnotationImageConf(
    Stroke stroke,
    int scale,
    int pointSize,
    Color pointColor,
    Color measurementBgColor,
    Color measurementTextColor,
    IntXY measurementOffset,
    Font measurementFont,
    boolean drawMeasurement) {
  public static final Stroke DEFAULT_STROKE = new BasicStroke(1.2f);
  public static final int DEFAULT_SCALE = 3;
  public static final int DEFAULT_POINT_SIZE = 8;
  public static final Color DEFAULT_POINT_COLOR = BLACK;
  public static final Color DEFAULT_MEASUREMENT_BG_COLOR = new Color(0, 0, 0, 150);
  public static final Color DEFAULT_MEASUREMENT_TEXT_COLOR = WHITE;
  public static final IntXY DEFAULT_MEASUREMENT_OFFSET = new IntXY(6, 4);
  public static final Font DEFAULT_MEASUREMENT_FONT = new Font("Arial", PLAIN, 15);
  public static final boolean DRAW_MEASUREMENT = false;

  public ExportAreaPictureAnnotationImageConf() {
    this(
        DEFAULT_STROKE,
        DEFAULT_SCALE,
        DEFAULT_POINT_SIZE,
        DEFAULT_POINT_COLOR,
        DEFAULT_MEASUREMENT_BG_COLOR,
        DEFAULT_MEASUREMENT_TEXT_COLOR,
        DEFAULT_MEASUREMENT_OFFSET,
        DEFAULT_MEASUREMENT_FONT,
        DRAW_MEASUREMENT);
  }

  public ExportAreaPictureAnnotationImageConf(boolean drawMeasurement) {
    this(
        DEFAULT_STROKE,
        DEFAULT_SCALE,
        DEFAULT_POINT_SIZE,
        DEFAULT_POINT_COLOR,
        DEFAULT_MEASUREMENT_BG_COLOR,
        DEFAULT_MEASUREMENT_TEXT_COLOR,
        DEFAULT_MEASUREMENT_OFFSET,
        DEFAULT_MEASUREMENT_FONT,
        drawMeasurement);
  }
}
