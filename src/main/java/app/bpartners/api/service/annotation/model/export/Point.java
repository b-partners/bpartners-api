package app.bpartners.api.service.annotation.model.export;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class Point {
  private Double x;
  private Double y;
}
