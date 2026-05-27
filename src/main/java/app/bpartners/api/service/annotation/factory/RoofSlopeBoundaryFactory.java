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

  public static List<RoofSlopBoundary> create(
      Transform transform, ExportAreaPictureAnnotation3DPan pan) {
    var boundaries = new ArrayList<RoofSlopBoundary>();

    for (int i = 1; i < pan.getPolygon().getPoints().size(); i++) {
      var startPoint = pan.getPolygon().getPoints().get(i - 1);
      var endPoint = pan.getPolygon().getPoints().get(i);
      var boundaryCoordinates =
          new Coordinates(
              new int[] {startPoint.getX().intValue(), endPoint.getX().intValue()},
              new int[] {startPoint.getY().intValue(), endPoint.getY().intValue()});
      var transformedBoundaryCoordinates = transform.apply(boundaryCoordinates);
      var boundary =
          new RoofSlopBoundary(
              getRoofSlopeBoundaryType(pan, i - 1), transformedBoundaryCoordinates);

      boundaries.add(boundary);
    }

    return boundaries;
  }

  private static RoofSlopeBoundaryType getRoofSlopeBoundaryType(
      ExportAreaPictureAnnotation3DPan pan, int boundaryIndex) {
    try {
      var mapper = new ObjectMapper();
      var rawTypeNames =
          pan.getInfos().stream()
              .filter(info -> TYPE_NAME_LABEL.equals(info.getLabel()))
              .findFirst();
      if (rawTypeNames.isEmpty()) {
        return RoofSlopeBoundaryType.DEFAULT;
      }
      var typeNames =
          mapper.readValue(
              rawTypeNames.get().getValue(),
              new TypeReference<List<String>>() {}); // TODO: get from upper level
      var typeName = typeNames.get(boundaryIndex);

      return RoofSlopeBoundaryType.fromLabel(typeName);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to read types from pan info. " + e);
    }
  }
}
