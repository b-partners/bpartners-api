package app.bpartners.api.service;

import org.springframework.stereotype.Component;

@Component
public class MetaDataComponent {
  private IntXY xyOffset = new IntXY(0, 0);

  public void setOffsets(int xOffset, int yOffset) {
    this.xyOffset = new IntXY(xOffset, yOffset);
  }

  public int getXOffset() {
    return this.xyOffset.x;
  }

  public int getYOffset() {
    return this.xyOffset.y;
  }

  public record IntXY(int x, int y) {}
}
