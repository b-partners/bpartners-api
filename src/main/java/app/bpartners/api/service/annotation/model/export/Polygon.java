package app.bpartners.api.service.annotation.model.export;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class Polygon {
  private List<Point> points;
}
