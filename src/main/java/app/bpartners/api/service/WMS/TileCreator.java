package app.bpartners.api.service.WMS;

import static app.bpartners.api.repository.ban.BanApi.getHighestFeatGeoPosition;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.service.utils.GeoUtils;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class TileCreator implements Function<AreaPicture, Tile> {
  private final BanApi banApi;

  @Override
  public Tile apply(AreaPicture areaPicture) {
    var refreshed = refreshAreaPictureGeoPosition(areaPicture);
    return Tile.from(refreshed);
  }

  private AreaPicture refreshAreaPictureGeoPosition(AreaPicture areaPicture) {
    var geoFeatures = banApi.searchMultiplePos(areaPicture.getAddress());
    var geoPositions = geoFeatures.getFeatures().stream().map(BanApi::mapToGeoPosition).toList();
    return updateAreaPictureGeoCoordinates(areaPicture, geoPositions);
  }

  private static AreaPicture updateAreaPictureGeoCoordinates(
      AreaPicture areaPicture, List<GeoPosition> geoPositions) {
    NotFoundException notFoundException =
        new NotFoundException(
            "Given address "
                + areaPicture.getAddress()
                + " is not found."
                + " Check if it's not mal formed.");

    var highestFeatGeoPosition =
        getHighestFeatGeoPosition(geoPositions).orElseThrow(() -> notFoundException);
    areaPicture.setCurrentGeoPosition(toDomain(highestFeatGeoPosition));
    areaPicture.setGeoPositions(geoPositions.stream().map(TileCreator::toDomain).toList());

    return areaPicture;
  }

  private static app.bpartners.api.endpoint.rest.model.GeoPosition toDomain(
      GeoPosition geoPosition) {
    GeoUtils.Coordinate geoPositionCoordinates = geoPosition.getCoordinates();
    return new app.bpartners.api.endpoint.rest.model.GeoPosition()
        .longitude(geoPositionCoordinates.getLongitude())
        .latitude(geoPositionCoordinates.getLatitude())
        .score(geoPosition.getScore());
  }
}
