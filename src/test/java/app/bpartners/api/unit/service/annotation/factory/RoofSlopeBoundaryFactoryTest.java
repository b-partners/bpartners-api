package app.bpartners.api.unit.service.annotation.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.model.annotation.IntXY;
import app.bpartners.api.service.annotation.AreaAnnotation3DPan;
import app.bpartners.api.service.annotation.AreaAnnotationInstanceInfo;
import app.bpartners.api.service.annotation.Point;
import app.bpartners.api.service.annotation.Polygon;
import app.bpartners.api.service.annotation.factory.RoofSlopeBoundaryFactory;
import app.bpartners.api.service.annotation.model.RoofSlopeBoundaryType;
import app.bpartners.api.service.annotation.model.Transform;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoofSlopeBoundaryFactoryTest {

  @Test
  void create_should_return_boundaries_with_correct_types() {
    var pan =
        AreaAnnotation3DPan.builder()
            .polygon(
                new Polygon(
                    List.of(
                        new Point(0, 0),
                        new Point(10, 0),
                        new Point(10, 10),
                        new Point(0, 10),
                        new Point(0, 0))))
            .infos(
                List.of(
                    new AreaAnnotationInstanceInfo(
                        "edgeTypes", "[\"faitage\", \"egout\", \"rive\", \"noue\"]")))
            .measurements(List.of())
            .build();
    var transform =
        Transform.builder().min(new IntXY(0, 0)).offset(new IntXY(0, 0)).scale(1.0).build();

    var boundaries = RoofSlopeBoundaryFactory.create(transform, pan, false);

    assertEquals(4, boundaries.size());
    assertEquals(RoofSlopeBoundaryType.FAITAGE, boundaries.get(0).getType());
    assertEquals(RoofSlopeBoundaryType.EGOUT, boundaries.get(1).getType());
    assertEquals(RoofSlopeBoundaryType.RIVE, boundaries.get(2).getType());
    assertEquals(RoofSlopeBoundaryType.NOUE, boundaries.get(3).getType());
  }

  @Test
  void create_should_use_default_type_when_edgeTypes_info_is_missing() {
    var pan =
        AreaAnnotation3DPan.builder()
            .polygon(new Polygon(List.of(new Point(0, 0), new Point(10, 0), new Point(0, 0))))
            .infos(List.of())
            .measurements(List.of())
            .build();
    var transform =
        Transform.builder().min(new IntXY(0, 0)).offset(new IntXY(0, 0)).scale(1.0).build();

    var boundaries = RoofSlopeBoundaryFactory.create(transform, pan, false);

    assertEquals(2, boundaries.size());
    assertEquals(RoofSlopeBoundaryType.DEFAULT, boundaries.get(0).getType());
    assertEquals(RoofSlopeBoundaryType.DEFAULT, boundaries.get(1).getType());
  }
}
