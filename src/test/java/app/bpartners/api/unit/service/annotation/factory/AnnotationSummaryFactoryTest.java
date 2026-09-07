package app.bpartners.api.unit.service.annotation.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3D;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationMeasurement;
import app.bpartners.api.endpoint.rest.model.Point;
import app.bpartners.api.endpoint.rest.model.Polygon;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.factory.AnnotationSummaryFactory;
import app.bpartners.api.service.annotation.model.Pair;
import app.bpartners.api.service.annotation.model.Transform;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnotationSummaryFactoryTest {

  @Test
  void create_should_count_edge_shared_by_two_pans_only_once() {
    // Pan1 and Pan2 are two triangular roof faces sharing a ridge (faitage) between (10,0) and
    // (5,5). Pan2 stores that same edge in reverse point order, as would happen when each pan is
    // triangulated independently.
    var pan1 =
        new ExportAreaPictureAnnotation3DPan()
            .polygon(
                new Polygon()
                    .points(
                        List.of(
                            new Point().x(0d).y(0d),
                            new Point().x(10d).y(0d),
                            new Point().x(5d).y(5d),
                            new Point().x(0d).y(0d))))
            .measurements(
                List.of(
                    new ExportAreaPictureAnnotationMeasurement().value(10.0),
                    new ExportAreaPictureAnnotationMeasurement().value(7.07),
                    new ExportAreaPictureAnnotationMeasurement().value(7.07)))
            .infos(
                List.of(
                    new ExportAreaPictureAnnotationInstanceInfo()
                        .label("edgeTypes")
                        .value("[\"egout\", \"faitage\", \"aretier\"]")));

    var pan2 =
        new ExportAreaPictureAnnotation3DPan()
            .polygon(
                new Polygon()
                    .points(
                        List.of(
                            new Point().x(10d).y(0d),
                            new Point().x(20d).y(0d),
                            new Point().x(5d).y(5d),
                            new Point().x(10d).y(0d))))
            .measurements(
                List.of(
                    new ExportAreaPictureAnnotationMeasurement().value(10.0),
                    new ExportAreaPictureAnnotationMeasurement().value(11.18),
                    new ExportAreaPictureAnnotationMeasurement().value(7.07)))
            .infos(
                List.of(
                    new ExportAreaPictureAnnotationInstanceInfo()
                        .label("edgeTypes")
                        .value("[\"egout\", \"aretier\", \"faitage\"]")));

    var annotation =
        new ExportAreaPictureAnnotation()._3d(new ExportAreaPictureAnnotation3D().pans(List.of(pan1, pan2)));

    var generator = mock(ExportAreaPictureAnnotationImage3DGenerator.class);
    var dummyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    when(generator.generateBaseImageWithSlopeBoundariesWithMeasurement(anyList()))
        .thenReturn(Pair.of(Transform.builder().build(), dummyImage));
    when(generator.generateBaseImageWithAreas(anyList())).thenReturn(dummyImage);
    when(generator.generateBaseImageWithNames(anyList())).thenReturn(dummyImage);
    when(generator.generateBaseImageWithPitches(anyList())).thenReturn(dummyImage);

    var summary = AnnotationSummaryFactory.create(annotation, generator);

    var faitage =
        summary.measurements().stream().filter(m -> m.label().equals("Faitage")).findFirst();
    assertThat(faitage).isPresent();
    assertThat(faitage.get().value()).isEqualTo("7.07 m (1)");

    var egout =
        summary.measurements().stream().filter(m -> m.label().equals("Egout")).findFirst();
    assertThat(egout).isPresent();
    assertThat(egout.get().value()).isEqualTo("20.00 m (2)");

    var aretier =
        summary.measurements().stream().filter(m -> m.label().equals("Aretier")).findFirst();
    assertThat(aretier).isPresent();
    assertThat(aretier.get().value()).isEqualTo("18.25 m (2)");
  }
}
