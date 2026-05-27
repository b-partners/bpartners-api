package app.bpartners.api.service.annotation.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RoofSlopBoundary {
  private final RoofSlopeBoundaryType type;
  private final Coordinates coordinates;
}
