package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.exception.ServiceUnavailableException;
import app.bpartners.api.service.areapicture.AreaPictureZoomValidator;
import org.junit.jupiter.api.Test;

class AreaPictureZoomValidatorTest {
  AreaPictureZoomValidator subject = new AreaPictureZoomValidator();

  @Test
  void accept_without_exception() {
    var areaPictureMock = mock(AreaPicture.class);
    var areaPictureMapLayerMock = mock(AreaPictureMapLayer.class);
    var layerName = "PCRS.LAMB93";

    when(areaPictureMapLayerMock.getName()).thenReturn(layerName);
    when(areaPictureMapLayerMock.getPrecisionLevelInCm()).thenReturn(5);
    when(areaPictureMock.getCurrentLayer()).thenReturn(areaPictureMapLayerMock);

    assertDoesNotThrow(() -> subject.accept(areaPictureMock));
  }

  @Test
  void throw_service_unavailable_exception() {
    var areaPictureMock = mock(AreaPicture.class);
    var areaPictureMapLayerMock = mock(AreaPictureMapLayer.class);
    var layerName = "PCRS.LAMB93";

    when(areaPictureMapLayerMock.getName()).thenReturn(layerName);
    when(areaPictureMapLayerMock.getPrecisionLevelInCm()).thenReturn(20);
    when(areaPictureMock.getCurrentLayer()).thenReturn(areaPictureMapLayerMock);

    var actualException =
        assertThrows(ServiceUnavailableException.class, () -> subject.accept(areaPictureMock));

    assertEquals(
        "Layer " + layerName + " is temporarily unavailable", actualException.getMessage());
  }

  @Test
  void throw_not_implemented_exception() {
    var areaPictureMock = mock(AreaPicture.class);
    var areaPictureMapLayerMock = mock(AreaPictureMapLayer.class);
    var layerName = "PCRS";

    when(areaPictureMapLayerMock.getName()).thenReturn(layerName);
    when(areaPictureMapLayerMock.getPrecisionLevelInCm()).thenReturn(5);
    when(areaPictureMock.getCurrentLayer()).thenReturn(areaPictureMapLayerMock);

    var actualException =
        assertThrows(NotImplementedException.class, () -> subject.accept(areaPictureMock));

    assertEquals("Layer " + layerName + " is not yet supported", actualException.getMessage());
  }
}
