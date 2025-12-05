package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.ConverterAnnotation;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class ConverterValidator implements Consumer<Map<String, ConverterAnnotation>> {
  @Override
  public void accept(Map<String, ConverterAnnotation> converterPolygonAnnotations) {
    if (converterPolygonAnnotations.size() != 1) {
      throw new BadRequestException("Only one PolygonAnnotation is supported");
    }

    var converterPolygonAnnotation = converterPolygonAnnotations.values().iterator().next();
    if (converterPolygonAnnotation.getRegions().size() != 1) {
      throw new BadRequestException("Only one RegionAnnotation is supported");
    }

    var region = converterPolygonAnnotation.getRegions().entrySet().iterator().next().getValue();

    var allX = region.getShapeAttributes().getAllPointsX();
    var allY = region.getShapeAttributes().getAllPointsY();

    if (allX.size() != allY.size()) {
      throw new BadRequestException(
          "Mismatch between X and Y coordinates: "
              + "allX has "
              + allX.size()
              + " elements, "
              + "allY has "
              + allY.size()
              + " elements");
    }
  }
}
