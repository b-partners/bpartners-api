package app.bpartners.api.service.annotation.factory;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.service.annotation.model.Coordinates;
import app.bpartners.api.service.annotation.model.RoofSlopBoundary;
import app.bpartners.api.service.annotation.model.RoofSlopeBoundaryType;
import app.bpartners.api.service.annotation.model.Transform;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

public class RoofSlopeBoundaryFactory {
  private static final String TYPE_NAME_LABEL = "edgeTypes";

  private RoofSlopeBoundaryFactory() {}

  public static List<RoofSlopBoundary> create(
      Transform transform, ExportAreaPictureAnnotation3DPan pan) {
    var boundaries = new ArrayList<RoofSlopBoundary>();
    var boundariesTypesNames = getRoofSlopeBoundaryTypeNames(pan);
    var polygon = pan.getOrientedPolygon() == null ? pan.getPolygon() : pan.getOrientedPolygon();

    for (int i = 1; i < polygon.getPoints().size(); i++) {
      var startPoint = polygon.getPoints().get(i - 1);
      var endPoint = polygon.getPoints().get(i);
      var boundaryCoordinates =
          new Coordinates(
              new int[] {startPoint.getX().intValue(), endPoint.getX().intValue()},
              new int[] {startPoint.getY().intValue(), endPoint.getY().intValue()});
      var transformedBoundaryCoordinates = transform.apply(boundaryCoordinates);
      var boundary =
          new RoofSlopBoundary(
              getRoofSlopeBoundaryType(boundariesTypesNames, i - 1),
              transformedBoundaryCoordinates);

      boundaries.add(boundary);
    }

    return boundaries;
  }

  private static RoofSlopeBoundaryType getRoofSlopeBoundaryType(
      List<String> typeNames, int boundaryIndex) {
    if (typeNames.isEmpty()) {
      return RoofSlopeBoundaryType.DEFAULT;
    }
    var typeName = typeNames.get(boundaryIndex);

    return RoofSlopeBoundaryType.fromLabel(typeName);
  }

  public static List<String> getRoofSlopeBoundaryTypeNames(ExportAreaPictureAnnotation3DPan pan) {
    try {
      var mapper = new ObjectMapper();
      var rawTypeNames =
          pan.getInfos().stream()
              .filter(info -> TYPE_NAME_LABEL.equals(info.getLabel()))
              .findFirst();
      if (rawTypeNames.isEmpty()) {
        return List.of();
      }
      return mapper.readValue(rawTypeNames.get().getValue(), new TypeReference<List<String>>() {});
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to read types from pan info. " + e);
    }
  }
}
