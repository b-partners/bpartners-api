package app.bpartners.api.service.google.maps;

import app.bpartners.api.endpoint.rest.model.AutoCompletePrediction;
import app.bpartners.api.repository.google.geocode.GeoCodeApi;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AddressAutoCompleteService {
  private final GeoCodeApi geoCodeApi;

  public List<AutoCompletePrediction> autoCompleteAddress(String address, String sessionId) {
    return geoCodeApi.autoCompleteAddress(address, sessionId).stream()
        .map(
            pred ->
                new AutoCompletePrediction()
                    .placeId(pred.placeId)
                    .description(pred.description)
                    .distance(pred.distanceMeters))
        .toList();
  }
}
