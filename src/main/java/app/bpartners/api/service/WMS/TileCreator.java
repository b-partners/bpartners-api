package app.bpartners.api.service.WMS;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class TileCreator implements Function<AreaPicture, Tile> {
  private final BanApi banApi;
  private final MapLayerGuesser layerGuesser;

  @Override
  public Tile apply(AreaPicture areaPicture) {
    var refreshed = computeGeoPosition(areaPicture);
    List<MapLayer> guessedLayers = layerGuesser.apply(areaPicture);
    refreshed.setLayers(guessedLayers);
    if (areaPicture.getCurrentLayer() == null) {
      refreshed.setCurrentLayer(layerGuesser.getLatestOrDefault(guessedLayers));
    }
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
