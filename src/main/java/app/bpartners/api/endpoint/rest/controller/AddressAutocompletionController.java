package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.AutoCompletePrediction;
import app.bpartners.api.service.google.maps.AddressAutoCompleteService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AddressAutocompletionController {
  private final AddressAutoCompleteService service;

  @PostMapping("/address/autocomplete")
  public List<AutoCompletePrediction> autoCompleteAddress(@RequestParam("address") String address) {
    return service.autoCompleteAddress(address);
  }
}
