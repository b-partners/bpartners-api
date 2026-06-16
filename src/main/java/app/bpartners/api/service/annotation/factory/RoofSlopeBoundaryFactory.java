package app.bpartners.api.service.annotation.factory;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.endpoint.rest.model.Point;
import app.bpartners.api.service.annotation.model.*;
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
    var validePan = validate(pan);
    var boundariesTypesNames = validePan.first();
    var points = validePan.second();

    for (int i = 1; i < points.size(); i++) {
      var startPoint = points.get(i - 1);
      var endPoint = points.get(i);
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

  private static Pair<List<String>, List<Point>> validate(ExportAreaPictureAnnotation3DPan pan) {
    var boundariesTypesNames = getRoofSlopeBoundaryTypeNames(pan);
    var points = pan.getPolygon().getPoints();
    if (points == null) {
      throw new IllegalStateException("No points provided for pan " + pan.getName());
    }
    if (points.size() - 1 != boundariesTypesNames.size()) {
      throw new IllegalStateException(
          "Number of edges and number of edge types should be equal. "
              + "Edges: "
              + (points.size() - 1)
              + ", edge types: "
              + boundariesTypesNames.size());
    }

    return new Pair<>(boundariesTypesNames, points);
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
