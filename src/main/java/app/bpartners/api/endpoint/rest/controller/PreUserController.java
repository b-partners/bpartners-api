package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.PreUserMapper;
import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.model.entity.BoundedPageSize;
import app.bpartners.api.model.entity.PageFromOne;
import app.bpartners.api.model.entity.HPreUser;
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
      @RequestParam("page_size") BoundedPageSize pageSize
  ) {
    return service.getPreUsers(page, pageSize).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping("/preUsers")
  public List<app.bpartners.api.endpoint.rest.model.PreUser> createPreUser(
      @RequestBody List<CreatePreUser> createPreUsers) {
    List<HPreUser> toCreate =
        mapper.toDomain(createPreUsers);
    return mapper.toRest(service.createPreUsers(toCreate));
  }
}
