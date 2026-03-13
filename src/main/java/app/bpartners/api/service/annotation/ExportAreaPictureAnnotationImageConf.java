package app.bpartners.api.service.annotation;

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
public class ExportAreaPictureAnnotationImageConf {
  private int scale;
  private int pointSize;
  private Stroke stroke;
  private Color pointColor;
  private Color measurementBgColor;
  private Color measurementTextColor;
  private IntXY measurementOffset;
  private Font measurementFont;

  public ExportAreaPictureAnnotationImageConf() {
    this.stroke = DEFAULT_STROKE;
    this.scale = DEFAULT_SCALE;
    this.pointSize = DEFAULT_POINT_SIZE;
    this.pointColor = DEFAULT_POINT_COLOR;
    this.measurementBgColor = DEFAULT_MEASUREMENT_BG_COLOR;
    this.measurementTextColor = DEFAULT_MEASUREMENT_TEXT_COLOR;
    this.measurementOffset = DEFAULT_MEASUREMENT_OFFSET;
    this.measurementFont = DEFAULT_MEASUREMENT_FONT;
  }

  public ExportAreaPictureAnnotationImageConf rescale(double factor) {
    System.out.println("factor : " + factor);
    this.stroke = new BasicStroke((float) (((BasicStroke) this.stroke).getLineWidth() * factor));
    this.scale = (int) round(this.scale * factor);
    this.pointSize = (int) round(this.pointSize * factor);
    this.measurementFont =
        new Font(
            this.measurementFont.getName(),
            this.measurementFont.getStyle(),
            (int) Math.round(this.measurementFont.getSize() * factor));
    return this;
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
