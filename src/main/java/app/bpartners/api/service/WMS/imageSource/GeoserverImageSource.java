package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public final class GeoserverImageSource extends AbstractWmsImageSource {
  private final UriComponents baseUrl;
  private final XYZToBoundingBox xyzToBoundingBox;

  public GeoserverImageSource(
      FileDownloader fileDownloader,
      @Value("${geoserver.baseurl}") String geoserverBaseUrl,
      XYZToBoundingBox xyzToBoundingBox) {
    super(fileDownloader);
    this.baseUrl =
        UriComponentsBuilder.fromHttpUrl(geoserverBaseUrl)
            .query("layers={layers}")
            .query("format=image/jpeg")
            .query("width=1024")
            .query("height=1024")
            .query("bbox={bbox}")
            .query("SRS=EPSG:3857")
            .query("transparent=true")
            .query("service=WMS")
            .query("request=GetMap")
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
    Map<String, String> uriVariables =
        Map.of(
            "layers",
            mapLayer.getName(),
            "bbox",
            String.format("%s, %s, %s, %s", minx, miny, maxx, maxy));
    log.info("Geoserver expected url: {}", baseUrl.expand(uriVariables).toUri());
    return baseUrl.expand(uriVariables).toUri();
  }

  @Override
  public File downloadImage(AreaPicture areaPicture) {
    if (!supports(areaPicture)) {
      throw new ApiException(
          SERVER_EXCEPTION,
          "cannot download " + areaPicture + " from " + this.getClass().getTypeName());
    }
    return fileDownloaderImpl.get(
        areaPicture.getFilename(),
        getURI(areaPicture.getCurrentTile(), areaPicture.getCurrentLayer()));
  }

  @Override
  public boolean supports(AreaPicture areaPicture) {
    log.info("GEOSERVER is the expected layer: {}", areaPicture.getCurrentLayer().getSource());
    return GEOSERVER.equals(areaPicture.getCurrentLayer().getSource());
  }
}
