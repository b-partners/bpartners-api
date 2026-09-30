package app.bpartners.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.mapper.AreaPictureMapLayerRestMapper;
import app.bpartners.api.endpoint.rest.mapper.AreaPictureRestMapper;
import app.bpartners.api.endpoint.rest.model.MapLayerActual;
import app.bpartners.api.endpoint.rest.model.MapLayersReachability;
import app.bpartners.api.service.areapicture.AreaPictureService;
import app.bpartners.api.service.wms.AreaPictureMapLayerService;
import org.junit.jupiter.api.Test;

class AreaPictureControllerTest {
  private static final Double LATITUDE = 43.71;
  private static final Double LONGITUDE = 7.26;

  AreaPictureService serviceMock = mock();
  AreaPictureRestMapper mapperMock = mock();
  AreaPictureMapLayerRestMapper layerMapperMock = mock();
  AreaPictureMapLayerService mapLayerServiceMock = mock();

  AreaPictureController subject =
      new AreaPictureController(serviceMock, mapperMock, layerMapperMock, mapLayerServiceMock);

  @Test
  void get_map_layers_delegates_to_map_layer_service() {
    var expected = mock(MapLayersReachability.class);
    when(mapLayerServiceMock.getMapLayers(LATITUDE, LONGITUDE, true)).thenReturn(expected);

    var actual = subject.getMapLayers(LATITUDE, LONGITUDE, true);

    assertEquals(expected, actual);
  }

  @Test
  void get_map_layers_forwards_the_only_reachable_flag_as_received() {
    subject.getMapLayers(LATITUDE, LONGITUDE, false);

    verify(mapLayerServiceMock).getMapLayers(LATITUDE, LONGITUDE, false);
  }

  @Test
  void get_actual_map_layer_delegates_to_map_layer_service() {
    var expected = mock(MapLayerActual.class);
    when(mapLayerServiceMock.getActualMapLayer(LATITUDE, LONGITUDE)).thenReturn(expected);

    var actual = subject.getActualMapLayer(LATITUDE, LONGITUDE);

    assertEquals(expected, actual);
  }
}
