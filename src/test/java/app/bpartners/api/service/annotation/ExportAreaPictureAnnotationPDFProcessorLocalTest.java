package app.bpartners.api.service.annotation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ExportAreaPictureAnnotationPDFProcessorLocalTest {

  private ExportAreaPictureAnnotationPDFProcessor processor;
  private BufferedImage mockImage;

  @BeforeEach
  void setUp() throws IOException {
    ExportAreaPictureAnnotationPDFGenerator pdfGenerator =
        new ExportAreaPictureAnnotationPDFGenerator(new TemplateResolverEngine());
    ExportAreaPictureAnnotationImageGenerator imageGenerator =
        new ExportAreaPictureAnnotationImageGenerator();
    ExportAreaPictureAnnotationImage3DGenerator image3DGenerator =
        new ExportAreaPictureAnnotationImage3DGenerator();

    processor =
        new ExportAreaPictureAnnotationPDFProcessor(
            pdfGenerator, imageGenerator, image3DGenerator);

    mockImage =
        ImageIO.read(
            new ClassPathResource("files/downloaded-annotation-image.jpeg").getInputStream());
  }

  @Test
  void generate_pdf_locally() throws IOException {
    ExportAreaPictureAnnotation annotation = createMockAnnotation();
    byte[] globalImage3D = pdfBytesFromResource("files/downloaded-annotation-image.jpeg");

    byte[] pdfBytes = processor.process(annotation, mockImage, globalImage3D);

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);

    File outputDir = new File("build/test-output");
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    File outputFile = new File(outputDir, "annotation.pdf");
    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      fos.write(pdfBytes);
    }

    System.out.println("PDF generated at: " + outputFile.getAbsolutePath());
    assertTrue(outputFile.exists());
  }

  private byte[] pdfBytesFromResource(String path) throws IOException {
    return new ClassPathResource(path).getInputStream().readAllBytes();
  }

  private ExportAreaPictureAnnotation createMockAnnotation() {
    return new ExportAreaPictureAnnotation()
        .address("123 Test Street, Paris, France")
        .imageUrl("https://example.com/image.jpg")
        .globalRateValue(12.5)
        .globalRateType("Area")
        .llm("Some AI analysis here")
        ._3d(createMock3DAnnotation())
        .annotations(
            List.of(
                createAnnotationInstance("Roof", "Slate"),
                createAnnotationInstance("Wall", "Brick")));
  }

  private ExportAreaPictureAnnotation3D createMock3DAnnotation() {
    return new ExportAreaPictureAnnotation3D()
        .pans(
            List.of(
                new ExportAreaPictureAnnotation3DPan()
                    .name("Pan 1")
                    .polygon(
                        new Polygon()
                            .points(
                                List.of(
                                    new Point().x(50.0).y(50.0),
                                    new Point().x(150.0).y(50.0),
                                    new Point().x(150.0).y(150.0),
                                    new Point().x(50.0).y(150.0))))
                    .measurements(
                        List.of(
                            new ExportAreaPictureAnnotationMeasurement()
                                .value(5.0)
                                .unit("m")
                                .isInvisible(false),
                            new ExportAreaPictureAnnotationMeasurement()
                                .value(5.0)
                                .unit("m")
                                .isInvisible(false),
                            new ExportAreaPictureAnnotationMeasurement()
                                .value(5.0)
                                .unit("m")
                                .isInvisible(false),
                            new ExportAreaPictureAnnotationMeasurement()
                                .value(5.0)
                                .unit("m")
                                .isInvisible(false)))));
  }

  private ExportAreaPictureAnnotationInstance createAnnotationInstance(String key, String type) {
    return new ExportAreaPictureAnnotationInstance()
        .polygon(
            new Polygon()
                .points(
                    List.of(
                        new Point().x(100.0).y(100.0),
                        new Point().x(200.0).y(100.0),
                        new Point().x(200.0).y(200.0),
                        new Point().x(100.0).y(200.0))))
        .fillColor("#FF000055")
        .strokeColor("#FF0000")
        .measurements(
            List.of(
                new ExportAreaPictureAnnotationMeasurement()
                    .value(10.0)
                    .unit("m")
                    .isInvisible(false),
                new ExportAreaPictureAnnotationMeasurement()
                    .value(10.0)
                    .unit("m")
                    .isInvisible(false),
                new ExportAreaPictureAnnotationMeasurement()
                    .value(10.0)
                    .unit("m")
                    .isInvisible(false),
                new ExportAreaPictureAnnotationMeasurement()
                    .value(10.0)
                    .unit("m")
                    .isInvisible(false)))
        .infos(
            List.of(
                new ExportAreaPictureAnnotationInstanceInfo().label("key").value(key),
                new ExportAreaPictureAnnotationInstanceInfo().label("Type").value(type),
                new ExportAreaPictureAnnotationInstanceInfo().label("Surface").value("100 m²")));
  }
}
