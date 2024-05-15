package app.bpartners.api.service.WMS;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class TileCreator implements Function<AreaPicture, Tile> {
  private final BanApi banApi;

  @Override
  public Tile apply(AreaPicture areaPicture) {
    var refreshed = computeGeoPosition(areaPicture);
    refreshed.setTile(Tile.from(areaPicture));
    return refreshed.getTile();
  }

  public AreaPicture computeGeoPosition(AreaPicture areaPicture) {
    GeoPosition geoPosition = banApi.fSearch(areaPicture.getAddress());
    areaPicture.setLongitude(geoPosition.getCoordinates().getLongitude());
    areaPicture.setLatitude(geoPosition.getCoordinates().getLatitude());
    return areaPicture;
  }
}
