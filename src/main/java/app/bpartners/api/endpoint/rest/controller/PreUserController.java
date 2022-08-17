package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.PreUserMapper;
import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.PreUser;
import app.bpartners.api.service.PreUserService;
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
public class PreUserController {
  private final PreUserService service;
  private final PreUserMapper mapper;

  @GetMapping("/preUsers")
  public List<app.bpartners.api.endpoint.rest.model.PreUser> getPreUsers(
      @RequestParam PageFromOne page,
      @RequestParam("page_size") BoundedPageSize pageSize,
      @RequestParam String firstname,
      @RequestParam String lastname,
      @RequestParam String society,
      @RequestParam String email
  ) {
    return service.getByCriteria(page,pageSize,firstname,lastname,society,email).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping("/preUsers")
  public List<app.bpartners.api.endpoint.rest.model.PreUser> createPreUser(
      @RequestBody List<CreatePreUser> createPreUsers) {
    List<PreUser> toCreate =
        mapper.toDomain(createPreUsers);
    return mapper.toRest(service.createPreUsers(toCreate));
  }
}
