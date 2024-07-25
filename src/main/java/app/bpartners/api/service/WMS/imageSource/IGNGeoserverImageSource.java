package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER_IGN;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.WMS.Tile;
import app.bpartners.api.service.converter.XYZToBoundingBox;
import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
final class IGNGeoserverImageSource extends AbstractWmsImageSource {
  private final UriComponents baseUrl;
  private final XYZToBoundingBox xyzToBoundingBox;

  public IGNGeoserverImageSource(
      FileDownloader fileDownloader,
      @Value("${ign.geoserver.baseurl}") String geoserverBaseUrl,
      XYZToBoundingBox xyzToBoundingBox) {
    super(fileDownloader);
    this.baseUrl =
        UriComponentsBuilder.fromHttpUrl(geoserverBaseUrl)
            .query("layers={layer}")
            .query("format=image/jpeg")
            .query("width=1024")
            .query("height=1024")
            .query("bbox={bbox}")
            .query("CRS=EPSG:3857")
            .build();
    this.xyzToBoundingBox = xyzToBoundingBox;
  }

  @Override
  public URI getURI(Tile tile, AreaPictureMapLayer mapLayer) {
    XYZToBoundingBox.BBOX boundingBox = xyzToBoundingBox.apply(tile);
    BigDecimal minx = boundingBox.minx();
    BigDecimal miny = boundingBox.miny();
    BigDecimal maxx = boundingBox.maxx();
    BigDecimal maxy = boundingBox.maxy();
    Map<String, Object> uriVariables =
        Map.of(
            "layer",
            mapLayer.getName(),
            "bbox",
            String.format("%s, %s, %s, %s", minx, miny, maxx, maxy));
    return baseUrl.expand(uriVariables).toUri();
  }

  @Override
  public File downloadImage(AreaPicture areaPicture) {
    if (!supports(areaPicture)) {
      throw new ApiException(
          SERVER_EXCEPTION,
          "cannot download " + areaPicture + " from " + this.getClass().getTypeName());
    }

    return fileDownloaderImpl.getFromS3(
        areaPicture.getFilename(),
        getURI(areaPicture.getCurrentTile(), areaPicture.getCurrentLayer()));
  }

  @Override
  public boolean supports(AreaPicture areaPicture) {
    return GEOSERVER_IGN.equals(areaPicture.getCurrentLayer().getSource());
  }
}
