package app.bpartners.api.service.converter;

import app.bpartners.api.service.WMS.Tile;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class XYZToBoundingBox implements XYZConverter {
  private final UriComponents baseUrl;
  private final RestTemplate restTemplate;
  private final String converterUrl;

  public XYZToBoundingBox(
      RestTemplate restTemplate, @Value("${xyz.to.bbox.converter.url}") String converterUrl) {
    this.restTemplate = restTemplate;
    this.baseUrl =
        UriComponentsBuilder.fromHttpUrl(converterUrl)
            .query("x={x}")
            .query("y={y}")
            .query("z={z}")
            .build();
    this.converterUrl = converterUrl;
  }

  @Override
  public BBOX apply(Tile tile) {
    Map<String, Object> uriVariables =
        Map.of("x", tile.getX(), "y", tile.getArcgisZoom().getZoomLevel(), "z", tile.getY());
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUri(baseUrl.expand(uriVariables).toUri());
    String url = builder.toUriString();
    BBOX bbox = restTemplate.getForObject(url, BBOX.class);
    return bbox;
  }

  public record BBOX(BigDecimal minx, BigDecimal miny, BigDecimal maxx, BigDecimal maxy) {}
}
