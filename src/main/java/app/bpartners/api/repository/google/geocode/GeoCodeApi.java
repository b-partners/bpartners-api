package app.bpartners.api.repository.google.geocode;

import app.bpartners.api.endpoint.rest.model.GeoPosition;
import app.bpartners.api.repository.validator.AddressValidator;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeoCodeApi {
  private final GeoApiContext geoApiContext;
  private final AddressValidator addressValidator;
  private final Double DEFAULT_GEOCODE_SCORE = 0.0;

  public GeoCodeApi(
      @Value("${google.geocode.api.key}") String apiKey, AddressValidator addressValidator) {
    this.geoApiContext = new GeoApiContext.Builder().apiKey(apiKey).build();
    this.addressValidator = addressValidator;
  }

  public GeoPosition searchGeoPositionFromAddress(String address)
      throws IOException, InterruptedException, ApiException {
    addressValidator.accept(address);
    GeocodingResult[] geocodingResults = GeocodingApi.geocode(this.geoApiContext, address).await();
    GeocodingResult response = geocodingResults[1];
    LatLng location = response.geometry.location;
    GeoPosition position =
        new GeoPosition()
            .score(DEFAULT_GEOCODE_SCORE)
            .latitude(location.lat)
            .longitude(location.lng);
    return position;
  }
}
