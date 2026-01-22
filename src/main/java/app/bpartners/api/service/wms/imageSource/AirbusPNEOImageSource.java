package app.bpartners.api.service.wms.imageSource;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.validator.AreaPictureValidator;
import app.bpartners.api.service.wms.Tile;
import app.bpartners.api.service.wms.model.AirbusFeature;
import app.bpartners.api.service.wms.model.AirbusPNEOResponse;
import app.bpartners.api.service.wms.model.AirbusRequestBody;
import app.bpartners.api.service.wms.model.Geometry;
import app.bpartners.api.service.wms.service.AirbusPNEOService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public final class AirbusPNEOImageSource extends AbstractWmsImageSource {
  private final UriComponents baseUrl;
  private final AreaPictureValidator areaPictureValidator;
  private final AirbusPNEOService airbusPNEOService;
  private final RestTemplate restTemplate;

  public AirbusPNEOImageSource(
      @Value("${airbus.searchapi.baseurl}") String baseUrl,
      @Value("${airbus.authentication.baseurl}") String airbusAuthenticationBaseUrl,
      @Value("${airbus.api.key}") String airbusApiKey,
      FileDownloader fileDownloader,
      AreaPictureValidator areaPictureValidator,
      RestTemplate restTemplate) {
    super(fileDownloader);
    this.baseUrl = UriComponentsBuilder.fromHttpUrl(baseUrl).build();
    this.areaPictureValidator = areaPictureValidator;
    this.airbusPNEOService =
        new AirbusPNEOService(restTemplate, airbusAuthenticationBaseUrl, airbusApiKey);
    this.restTemplate = restTemplate;
  }

  @Override
  protected URI getURI(Tile tile, AreaPictureMapLayer areaPictureMapLayer) {
    return this.baseUrl.toUri();
  }

  @SneakyThrows
  @Override
  public File downloadImage(AreaPicture areaPicture) {
    String bearerToken = airbusPNEOService.authenticateAirbus();
    Tile tile = areaPicture.getCurrentTile();
    double latitude =
        areaPicture.getCurrentGeoPosition().getLatitude() != null
            ? areaPicture.getCurrentGeoPosition().getLatitude()
            : 0;
    double longitude =
        areaPicture.getCurrentGeoPosition().getLongitude() != null
            ? areaPicture.getCurrentGeoPosition().getLongitude()
            : 0;
    Geometry geometry = airbusPNEOService.convertLatLonToGeometry(latitude, longitude);
    AirbusRequestBody requestBody =
        AirbusRequestBody.builder()
            .constellation("PNEO")
            .workspace("public-pneo")
            .cloudCover("[0,10]")
            .itemsPerPage(10)
            .startPage(1)
            .processingLevel("SENSOR")
            .relation("intersects")
            .sortBy("-acquisitionDate")
            .geometry(geometry)
            .build();
    HttpHeaders headersWithBearer = customizeHeaders(bearerToken);
    headersWithBearer.setAccept(List.of(MediaType.APPLICATION_JSON));
    headersWithBearer.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
    HttpEntity<AirbusRequestBody> entity = new HttpEntity<>(requestBody, headersWithBearer);
    ObjectMapper mapper = new ObjectMapper();
    log.warn("RAW JSON = {}", mapper.writeValueAsString(requestBody));

    ResponseEntity<AirbusPNEOResponse> response =
        restTemplate.exchange(baseUrl.toUri(), HttpMethod.POST, entity, AirbusPNEOResponse.class);
    log.info("Searched image successfully");

    log.info("Search results = {}", mapper.writeValueAsString(response.getBody()));
    AirbusFeature feature = Objects.requireNonNull(response.getBody()).getFeatures().getFirst();
    String wmtsLink =
        String.format(
            "%s/tiles/1.0.0/default/rgb/EPSG3857/%s/%d/%d.png",
            feature.getLinks().getWmts().getHref(),
            areaPicture.getArcgisZoom().getZoomLevel(),
            tile.getX(),
            tile.getY());

    HttpEntity<Void> wmtsEntityHeaders = new HttpEntity<>(customizeHeaders(bearerToken));
    ResponseEntity<byte[]> pneoImage =
        restTemplate.exchange(wmtsLink, HttpMethod.GET, wmtsEntityHeaders, byte[].class);

    byte[] result = pneoImage.getBody();
    if (result == null) {
      throw new Exception("Image is null");
    }
    Path outputPath = Paths.get(areaPicture.getFilename());
    return Files.write(outputPath, result).toFile();
  }

  public HttpHeaders customizeHeaders(String bearerToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(bearerToken);
    return headers;
  }

  @Override
  public boolean supports(AreaPicture areaPicture) {
    return false;
  }
}
