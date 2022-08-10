package app.bpartners.api.endpoint.rest.controller;


import app.bpartners.api.endpoint.rest.mapper.PreRegistrationMapper;
import app.bpartners.api.endpoint.rest.model.CreatePreRegistration;
import app.bpartners.api.endpoint.rest.model.PreRegistration;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.PreRegistrationService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
public class PreRegistrationController {

  private final PreRegistrationService preRegistrationService;

  private final PreRegistrationMapper preRegistrationMapper;

  @PostMapping("/pre-registration")
  public PreRegistration createEmail(@RequestBody CreatePreRegistration toCreate) {
     return preRegistrationMapper
             .toRestRegistration(preRegistrationService.createEmail(preRegistrationMapper
                     .toDomain(toCreate)));
  }
  @GetMapping("/pre-registration")
  public ArrayList<PreRegistration> getEmails(
          @RequestParam PageFromOne page,
          @RequestParam("page_size") BoundedPageSize pageSize) {
    return (ArrayList<PreRegistration>) preRegistrationService.getPreRegistrations(page,pageSize).stream()
            .map(preRegistrationMapper::toRestRegistration)
            .collect(Collectors.toUnmodifiableList());
  }
}
