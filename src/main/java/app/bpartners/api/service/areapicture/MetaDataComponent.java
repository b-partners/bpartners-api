package app.bpartners.api.service.areapicture;

import java.time.LocalDate;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class MetaDataComponent {
  private IntXY xyOffset = new IntXY(0, 0);
  private int airbusYear = 2025;
  private LocalDate lastUpdatedAt;

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
