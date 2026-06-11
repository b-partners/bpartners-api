package app.bpartners.api.unit.service.annotation.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.model.annotation.IntXY;
import app.bpartners.api.service.annotation.factory.RoofSlopeBoundaryFactory;
import app.bpartners.api.service.annotation.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.service.annotation.model.ExportAreaPictureAnnotationInstanceInfo;
import app.bpartners.api.service.annotation.model.Point;
import app.bpartners.api.service.annotation.model.Polygon;
import app.bpartners.api.service.annotation.model.RoofSlopeBoundaryType;
import app.bpartners.api.service.annotation.model.Transform;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoofSlopeBoundaryFactoryTest {

  @Test
  void create_should_return_boundaries_with_correct_types() {
    var pan = new ExportAreaPictureAnnotation3DPan();
    var polygon = new Polygon();
    polygon.setPoints(
        List.of(
            new Point().setX(0d).setY(0d),
            new Point().setX(10d).setY(0d),
            new Point().setX(10d).setY(10d),
            new Point().setX(0d).setY(10d),
            new Point().setX(0d).setY(0d)));
    pan.setPolygon(polygon);
    var info = new ExportAreaPictureAnnotationInstanceInfo();
    info.setLabel("edgeTypes");
    info.setValue("[\"faitage\", \"egout\", \"rive\", \"noue\"]");
    pan.setInfos(List.of(info));

    var transform =
        Transform.builder().min(new IntXY(0, 0)).offset(new IntXY(0, 0)).scale(1.0).build();

    var boundaries = RoofSlopeBoundaryFactory.create(transform, pan);

    assertEquals(4, boundaries.size());
    assertEquals(RoofSlopeBoundaryType.FAITAGE, boundaries.get(0).getType());
    assertEquals(RoofSlopeBoundaryType.EGOUT, boundaries.get(1).getType());
    assertEquals(RoofSlopeBoundaryType.RIVE, boundaries.get(2).getType());
    assertEquals(RoofSlopeBoundaryType.NOUE, boundaries.get(3).getType());
  }

  @Test
  void create_should_use_default_type_when_edgeTypes_info_is_missing() {
    var pan = new ExportAreaPictureAnnotation3DPan();
    var polygon = new Polygon();
    polygon.setPoints(
        List.of(
            new Point().setX(0d).setY(0d),
            new Point().setX(10d).setY(0d),
            new Point().setX(0d).setY(0d)));
    pan.setPolygon(polygon);
    pan.setInfos(List.of());

    var transform =
        Transform.builder().min(new IntXY(0, 0)).offset(new IntXY(0, 0)).scale(1.0).build();

    var boundaries = RoofSlopeBoundaryFactory.create(transform, pan);

    assertEquals(2, boundaries.size());
    assertEquals(RoofSlopeBoundaryType.DEFAULT, boundaries.get(0).getType());
    assertEquals(RoofSlopeBoundaryType.DEFAULT, boundaries.get(1).getType());
  }
}
