package app.bpartners.api.unit.service.annotation.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo;
import app.bpartners.api.endpoint.rest.model.Point;
import app.bpartners.api.endpoint.rest.model.Polygon;
import app.bpartners.api.model.annotation.IntXY;
import app.bpartners.api.service.annotation.factory.RoofSlopeBoundaryFactory;
import app.bpartners.api.service.annotation.model.RoofSlopeBoundaryType;
import app.bpartners.api.service.annotation.model.Transform;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoofSlopeBoundaryFactoryTest {

  @Test
  void create_should_return_boundaries_with_correct_types() {
    var pan =
        new ExportAreaPictureAnnotation3DPan()
            .polygon(
                new Polygon()
                    .points(
                        List.of(
                            new Point().x(0d).y(0d),
                            new Point().x(10d).y(0d),
                            new Point().x(10d).y(10d),
                            new Point().x(0d).y(10d),
                            new Point().x(0d).y(0d))))
            .infos(
                List.of(
                    new ExportAreaPictureAnnotationInstanceInfo()
                        .label("edgeTypes")
                        .value("[\"faitage\", \"egout\", \"rive\", \"noue\"]")));
    var transform =
        Transform.builder()
            .min(new IntXY(0, 0))
            .max(new IntXY(10, 10))
            .offset(new IntXY(0, 0))
            .scale(1.0)
            .build();

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
        new ExportAreaPictureAnnotation3DPan()
            .polygon(
                new Polygon()
                    .points(
                        List.of(
                            new Point().x(0d).y(0d),
                            new Point().x(10d).y(0d),
                            new Point().x(0d).y(0d))))
            .infos(List.of());
    var transform =
        Transform.builder()
            .min(new IntXY(0, 0))
            .max(new IntXY(10, 0))
            .offset(new IntXY(0, 0))
            .scale(1.0)
            .build();

    var boundaries = RoofSlopeBoundaryFactory.create(transform, pan, false);

    assertEquals(2, boundaries.size());
    assertEquals(RoofSlopeBoundaryType.DEFAULT, boundaries.get(0).getType());
    assertEquals(RoofSlopeBoundaryType.DEFAULT, boundaries.get(1).getType());
  }
}
