package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.PreRegistrationMapper;
import app.bpartners.api.endpoint.rest.model.CreatePreRegistration;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.PreRegistration;
import app.bpartners.api.service.PreRegistrationService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PreRegistrationController {
  private final PreRegistrationService service;
  private final PreRegistrationMapper mapper;

  @GetMapping("/pre-registration")
  public List<app.bpartners.api.endpoint.rest.model.PreRegistration> getPreRegistrations(
      @RequestParam PageFromOne page,
      @RequestParam("page_size") BoundedPageSize pageSize
  ) {
    return service.getAll(page, pageSize).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping("/pre-registration")
  public app.bpartners.api.endpoint.rest.model.PreRegistration createEmail(
      @RequestBody CreatePreRegistration createPreRegistration) {
    PreRegistration toCreate =
        mapper.toDomain(createPreRegistration);
    return mapper.toRest(service.createPreRegistration(toCreate));
  }
}
