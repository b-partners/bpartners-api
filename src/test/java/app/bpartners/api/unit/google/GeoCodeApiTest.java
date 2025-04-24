package app.bpartners.api.unit.google;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.api.endpoint.rest.model.GeoPosition;
import app.bpartners.api.repository.google.geocode.GeoCodeApi;
import app.bpartners.api.repository.validator.AddressValidator;
import com.google.maps.errors.ApiException;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class GeoCodeApiTest {
  AddressValidator addressValidator = new AddressValidator();
  GeoCodeApi geoCodeApi = new GeoCodeApi(System.getenv("GOOGLE_GEOCODE_API_KEY"), addressValidator);

  @Test
  void retrieve_geoposition_from_address() throws IOException, InterruptedException, ApiException {
    GeoPosition geoPosition =
        geoCodeApi.searchGeoPositionFromAddress("12 Boulevard de la Croisette, 06400 Cannes");
    assertNotNull(geoPosition);
  }

  @Test
  void autocompletion_from_address() {
    var actual = geoCodeApi.autoCompleteAddress("12 Boulevard de la Croisette, 06400 Cannes");

    assertNotNull(actual);
  }
}
