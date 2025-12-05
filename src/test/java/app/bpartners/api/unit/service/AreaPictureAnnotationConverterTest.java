package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.api.endpoint.rest.model.ConverterAnnotation;
import app.bpartners.api.endpoint.rest.model.ConverterAnnotationRegion;
import app.bpartners.api.endpoint.rest.model.ConverterAnnotationShapeAttributes;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.annotation.AreaPictureAnnotationConverter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AreaPictureAnnotationConverterTest {
  private static final AreaPictureAnnotationConverter subject =
      new AreaPictureAnnotationConverter();
  private static final String FILE_NAME = "4f0df528c51644f8a5050f1e3a4ee2b8_20_523561_370292.jpg";

  private static ConverterAnnotation latLongConverterAnnotation() {
    List<BigDecimal> allX =
        List.of(
            BigDecimal.valueOf(-0.24943102151155472),
            BigDecimal.valueOf(-0.24946454912424088),
            BigDecimal.valueOf(-0.24947427213191986),
            BigDecimal.valueOf(-0.24945851415395737),
            BigDecimal.valueOf(-0.24939380586147308),
            BigDecimal.valueOf(-0.24929959326982498),
            BigDecimal.valueOf(-0.24922482669353485),
            BigDecimal.valueOf(-0.24920538067817688),
            BigDecimal.valueOf(-0.2492033690214157),
            BigDecimal.valueOf(-0.24925164878368378),
            BigDecimal.valueOf(-0.24923957884311676),
            BigDecimal.valueOf(-0.24932239204645157),
            BigDecimal.valueOf(-0.24943102151155472));

    List<BigDecimal> allY =
        List.of(
            BigDecimal.valueOf(46.65204075081879),
            BigDecimal.valueOf(46.651965724251774),
            BigDecimal.valueOf(46.651922227206676),
            BigDecimal.valueOf(46.6519070377541),
            BigDecimal.valueOf(46.65189184829728),
            BigDecimal.valueOf(46.651887475574064),
            BigDecimal.valueOf(46.65188149184699),
            BigDecimal.valueOf(46.65191785448591),
            BigDecimal.valueOf(46.65194731280838),
            BigDecimal.valueOf(46.65196733525277),
            BigDecimal.valueOf(46.65200001554839),
            BigDecimal.valueOf(46.65202694225599),
            BigDecimal.valueOf(46.65204075081879));

    return toConverterAnnotation(allX, allY);
  }

  private static ConverterAnnotation pixelConverterAnnotation() {
    List<BigDecimal> allX =
        List.of(
            BigDecimal.valueOf(491.0),
            BigDecimal.valueOf(391.0),
            BigDecimal.valueOf(362.0),
            BigDecimal.valueOf(409.0),
            BigDecimal.valueOf(602.0),
            BigDecimal.valueOf(883.0),
            BigDecimal.valueOf(1106.0),
            BigDecimal.valueOf(1164.0),
            BigDecimal.valueOf(1170.0),
            BigDecimal.valueOf(1026.0),
            BigDecimal.valueOf(1062.0),
            BigDecimal.valueOf(815.0),
            BigDecimal.valueOf(491.0));

    List<BigDecimal> allY =
        List.of(
            BigDecimal.valueOf(971.0),
            BigDecimal.valueOf(1297.0),
            BigDecimal.valueOf(1486.0),
            BigDecimal.valueOf(1552.0),
            BigDecimal.valueOf(1618.0),
            BigDecimal.valueOf(1637.0),
            BigDecimal.valueOf(1663.0),
            BigDecimal.valueOf(1505.0000000596046),
            BigDecimal.valueOf(1377.0),
            BigDecimal.valueOf(1290.0),
            BigDecimal.valueOf(1148.0),
            BigDecimal.valueOf(1031.0),
            BigDecimal.valueOf(971.0));
    return toConverterAnnotation(allX, allY);
  }

  private static ConverterAnnotation toConverterAnnotation(
      List<BigDecimal> allX, List<BigDecimal> allY) {
    var shape = new ConverterAnnotationShapeAttributes().allPointsX(allX).allPointsY(allY);
    var region = new ConverterAnnotationRegion().shapeAttributes(shape);
    return new ConverterAnnotation()
        .filename(FILE_NAME)
        .size(1024)
        .zoom(20)
        .putRegionsItem(FILE_NAME, region);
  }

  @Test
  void convert_to_pixel_ok() {
    var payload = latLongConverterAnnotation();
    var expected = pixelConverterAnnotation();

    var actualMapOfConvertAnnotation = subject.toPixel(Map.of("id", payload));

    assertEquals(1, actualMapOfConvertAnnotation.size());

    var actual = actualMapOfConvertAnnotation.get(FILE_NAME);
    assertEquals(expected, actual);
  }

  @Test
  void convert_to_lon_lat_ok() {
    var payload = pixelConverterAnnotation();
    var expected = latLongConverterAnnotation();

    var actualMapOfConvertAnnotation = subject.toLatLong(Map.of("id", payload));

    assertEquals(1, actualMapOfConvertAnnotation.size());

    var actual = actualMapOfConvertAnnotation.get(FILE_NAME);
    assertEquals(expected.getFilename(), actual.getFilename());
    assertEquals(expected.getZoom(), actual.getZoom());
  }

  @Test
  void apply_withInvalidFilename_shouldThrowBadRequestException() {
    var latLongConverterAnnotation = latLongConverterAnnotation().filename("invalid_file_name");

    var input = Map.of("id", latLongConverterAnnotation);

    var ex = assertThrows(BadRequestException.class, () -> subject.toPixel(input));
    assertTrue(ex.getMessage().contains("Wrong filename received"));
  }
}
