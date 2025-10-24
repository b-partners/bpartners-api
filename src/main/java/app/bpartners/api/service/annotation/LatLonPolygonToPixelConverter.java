package app.bpartners.api.service.annotation;

import static java.lang.Integer.parseInt;

import app.bpartners.api.endpoint.rest.model.ConverterAnnotation;
import app.bpartners.api.endpoint.rest.model.ConverterAnnotationRegion;
import app.bpartners.api.endpoint.rest.model.ConverterAnnotationShapeAttributes;
import app.bpartners.api.model.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LatLonPolygonToPixelConverter
    implements Function<Map<String, ConverterAnnotation>, Map<String, ConverterAnnotation>> {
  private static final Pattern FILENAME_PATTERN =
      Pattern.compile("^[a-f0-9]+_(\\d+)_(\\d+)_(\\d+)(?:\\.[^.]+)?$");
  private static final GeometryFactory geometryFactory = new GeometryFactory();

  @Override
  public Map<String, ConverterAnnotation> apply(Map<String, ConverterAnnotation> converterData) {
    var annotation = converterData.entrySet().iterator().next().getValue();

    var regions = annotation.getRegions();
    var region = regions.entrySet().iterator().next().getValue();
    var allX = region.getShapeAttributes().getAllPointsX();
    var allY = region.getShapeAttributes().getAllPointsY();
    var polygon = toPolygon(allX, allY);

    return convertPolygonAndMapToConverterAnnotation(polygon, annotation);
  }

  private static Map<String, ConverterAnnotation> convertPolygonAndMapToConverterAnnotation(
      Polygon polygon, ConverterAnnotation annotation) {
    var filename = annotation.getFilename();
    var zoom = annotation.getZoom();
    var x = getX(filename);
    var y = getY(filename);
    var tileSize = annotation.getSize();
    var projectedPolygon = projectPolygon(polygon, x, y, zoom, tileSize);

    var newShape =
        new ConverterAnnotationShapeAttributes()
            .allPointsX(getAllXFromPolygon(projectedPolygon))
            .allPointsY(getAllYFromPolygon(projectedPolygon));

    return Map.of(
        filename,
        new ConverterAnnotation()
            .zoom(zoom)
            .filename(filename)
            .size(tileSize)
            .regions(Map.of(filename, new ConverterAnnotationRegion().shapeAttributes(newShape))));
  }

  private static List<List<BigDecimal>> projectPolygon(
      Polygon polygon, int tileX, int tileY, int zoom, int tileSizePx) {
    var pixelRings =
        new ArrayList<>(projectRing(polygon.getExteriorRing(), tileX, tileY, zoom, tileSizePx));

    for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
      pixelRings.addAll(projectRing(polygon.getInteriorRingN(j), tileX, tileY, zoom, tileSizePx));
    }

    return pixelRings;
  }

  private static List<List<BigDecimal>> projectRing(
      LineString ring, int tileX, int tileY, int zoom, int tileSizePx) {
    return Arrays.stream(ring.getCoordinates())
        .map(
            coordinate -> {
              var px =
                  lonLatToPixelInTile(coordinate.x, coordinate.y, tileX, tileY, zoom, tileSizePx);
              return List.of(px[0], px[1]);
            })
        .toList();
  }

  private static BigDecimal[] lonLatToPixelInTile(
      double lon, double lat, int tileX, int tileY, int zoom, int tileSizePx) {
    double n = Math.pow(2.0, zoom);

    double x = (lon + 180.0) / 360.0 * n;
    double latRad = Math.toRadians(lat);
    double y = (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n;

    double pixelX = (x - tileX) * tileSizePx;
    double pixelY = (y - tileY) * tileSizePx;

    return new BigDecimal[] {BigDecimal.valueOf(pixelX), BigDecimal.valueOf(pixelY)};
  }

  private static int getX(String filename) {
    return extractValueFromFilename(filename, 2);
  }

  private static int getY(String filename) {
    return extractValueFromFilename(filename, 3);
  }

  private static int extractValueFromFilename(String filename, int groupId) {
    var matcher = FILENAME_PATTERN.matcher(filename);
    if (!matcher.matches()) {
      throw new BadRequestException(
          "Wrong filename received. Expected format: <hash>_<zoom>_<x>_<y>, e.g. "
              + "4f0df528c51644f8a5050f1e3a4ee2b8_20_523561_370292");
    }
    return parseInt(matcher.group(groupId));
  }

  private static Polygon toPolygon(List<BigDecimal> allX, List<BigDecimal> allY) {
    var coordinates = new Coordinate[allX.size()];
    for (int i = 0; i < allX.size(); i++) {
      coordinates[i] = new Coordinate(allX.get(i).doubleValue(), allY.get(i).doubleValue());
    }
    return geometryFactory.createPolygon(coordinates);
  }

  private static List<BigDecimal> getAllXFromPolygon(List<List<BigDecimal>> polygon) {
    return polygon.stream().map(List::getFirst).toList();
  }

  private static List<BigDecimal> getAllYFromPolygon(List<List<BigDecimal>> polygon) {
    return polygon.stream().map(List::getLast).toList();
  }
}
