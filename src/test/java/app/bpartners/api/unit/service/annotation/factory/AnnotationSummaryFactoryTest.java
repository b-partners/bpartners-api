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
import app.bpartners.api.service.annotation.model.summary.AnnotationWaste;
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
        new ExportAreaPictureAnnotation()
            ._3d(new ExportAreaPictureAnnotation3D().pans(List.of(pan1, pan2)));

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

    var egout = summary.measurements().stream().filter(m -> m.label().equals("Egout")).findFirst();
    assertThat(egout).isPresent();
    assertThat(egout.get().value()).isEqualTo("20.00 m (2)");

    var aretier =
        summary.measurements().stream().filter(m -> m.label().equals("Aretier")).findFirst();
    assertThat(aretier).isPresent();
    assertThat(aretier.get().value()).isEqualTo("18.25 m (2)");
  }

  @Test
  void create_should_build_waste_ladder_from_total_roof_area() {
    // Two pans of 120 m² and 80 m² of rampant area, so the ladder is based on 200 m² and every
    // step lands on an exact two-decimal value.
    var pan1 = wastePan(0d, "120");
    var pan2 = wastePan(10d, "80");

    var annotation =
        new ExportAreaPictureAnnotation()
            ._3d(new ExportAreaPictureAnnotation3D().pans(List.of(pan1, pan2)));

    var summary = AnnotationSummaryFactory.create(annotation, mockedGenerator());

    assertThat(summary.wasteTable()).hasSize(10);
    assertThat(summary.wasteTable())
        .extracting(AnnotationWaste::percent)
        .containsExactly(
            "0.0 %",
            "2.5 %", "5.0 %", "7.5 %", "10.0 %", "12.5 %", "15.0 %", "17.5 %", "20.0 %", "22.5 %");
    assertThat(summary.wasteTable())
        .extracting(AnnotationWaste::area)
        .containsExactly(
            "200.00", "205.00", "210.00", "215.00", "220.00", "225.00", "230.00", "235.00",
            "240.00", "245.00");
    assertThat(summary.wasteTable())
        .filteredOn(AnnotationWaste::suggested)
        .extracting(AnnotationWaste::percent)
        .containsExactly("10.0 %");
    assertThat(summary.suggestedWastePercent()).isEqualTo("10.0 %");
  }

  private static ExportAreaPictureAnnotation3DPan wastePan(double xOffset, String rampantArea) {
    return new ExportAreaPictureAnnotation3DPan()
        .polygon(
            new Polygon()
                .points(
                    List.of(
                        new Point().x(xOffset).y(0d),
                        new Point().x(xOffset + 10d).y(0d),
                        new Point().x(xOffset + 5d).y(5d),
                        new Point().x(xOffset).y(0d))))
        .measurements(
            List.of(
                new ExportAreaPictureAnnotationMeasurement().value(10.0),
                new ExportAreaPictureAnnotationMeasurement().value(7.07),
                new ExportAreaPictureAnnotationMeasurement().value(7.07)))
        .infos(
            List.of(
                new ExportAreaPictureAnnotationInstanceInfo()
                    .label("edgeTypes")
                    .value("[\"egout\", \"faitage\", \"aretier\"]"),
                new ExportAreaPictureAnnotationInstanceInfo().label("Pente").value("30"),
                new ExportAreaPictureAnnotationInstanceInfo()
                    .label("Surface rampante")
                    .value(rampantArea)));
  }

  private static ExportAreaPictureAnnotationImage3DGenerator mockedGenerator() {
    var generator = mock(ExportAreaPictureAnnotationImage3DGenerator.class);
    var dummyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    when(generator.generateBaseImageWithSlopeBoundariesWithMeasurement(anyList()))
        .thenReturn(Pair.of(Transform.builder().build(), dummyImage));
    when(generator.generateBaseImageWithAreas(anyList())).thenReturn(dummyImage);
    when(generator.generateBaseImageWithNames(anyList())).thenReturn(dummyImage);
    when(generator.generateBaseImageWithPitches(anyList())).thenReturn(dummyImage);
    return generator;
  }
}
