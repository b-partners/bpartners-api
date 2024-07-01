package app.bpartners.api.endpoint.rest.controller.health;

import static app.bpartners.api.endpoint.rest.controller.health.PingController.KO;
import static app.bpartners.api.endpoint.rest.controller.health.PingController.OK;

import app.bpartners.api.PojaGenerated;
import app.bpartners.api.repository.DummyRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@PojaGenerated
@SuppressWarnings("all")
@RestController
@AllArgsConstructor
public class HealthDbController {

  DummyRepository dummyRepository;

  @GetMapping("/health/db")
  public ResponseEntity<String> dummyTable_should_not_be_empty() {
    return dummyRepository.findAll().isEmpty() ? KO : OK;
  }
}
