package app.bpartners.api.service.wms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.MapLayerActual;
import app.bpartners.api.endpoint.rest.model.MapLayersReachability;
import app.bpartners.api.model.mapper.AreaPictureMapLayerMapper;
import app.bpartners.api.service.geodata.ImageryService;
import org.junit.jupiter.api.Test;

class AreaPictureMapLayerProxyTest {
  private static final Double LATITUDE = 43.71;
  private static final Double LONGITUDE = 7.26;

  ImageryService imageryServiceMock = mock();
  AreaPictureMapLayerService subject =
      new AreaPictureMapLayerService(imageryServiceMock, new AreaPictureMapLayerMapper());

  @Test
  void get_map_layers_returns_what_geodata_answers() {
    var expected = mock(MapLayersReachability.class);
    when(imageryServiceMock.getMapLayers(LATITUDE, LONGITUDE, true)).thenReturn(expected);

    var actual = subject.getMapLayers(LATITUDE, LONGITUDE, true);

    assertEquals(expected, actual);
  }

  @Test
  void get_map_layers_keeps_the_coordinates_order_expected_by_geodata() {
    subject.getMapLayers(LATITUDE, LONGITUDE, false);

    verify(imageryServiceMock).getMapLayers(LATITUDE, LONGITUDE, false);
  }

  @Test
  void get_actual_map_layer_returns_what_geodata_answers() {
    var expected = mock(MapLayerActual.class);
    when(imageryServiceMock.getActualMapLayer(LATITUDE, LONGITUDE)).thenReturn(expected);

    var actual = subject.getActualMapLayer(LATITUDE, LONGITUDE);

    assertEquals(expected, actual);
  }
}
