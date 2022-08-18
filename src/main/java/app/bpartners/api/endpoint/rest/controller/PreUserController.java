package app.bpartners.api.endpoint.rest.controller;


import app.bpartners.api.endpoint.rest.mapper.PreUserRestMapper;
import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.model.PreUser;
import app.bpartners.api.model.entity.BoundedPageSize;
import app.bpartners.api.model.entity.PageFromOne;
import app.bpartners.api.service.PreUserService;
import java.util.List;
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
  private final PreUserRestMapper mapper;

  @GetMapping("/preUsers")
  public List<app.bpartners.api.endpoint.rest.model.PreUser> getPreUsers(
      @RequestParam PageFromOne page,
      @RequestParam("page_size") BoundedPageSize pageSize,
      @RequestParam(required = false) String firstName,
      @RequestParam(required = false) String lastName,
      @RequestParam(required = false) String society,
      @RequestParam(required = false) String email
  ) {
    return service.getPreUsers(page, pageSize).stream()
        .map(mapper::toRest)
        .toList();
  }

  @PostMapping("/preUsers")
  public List<app.bpartners.api.endpoint.rest.model.PreUser> createPreUsers(
      @RequestBody List<CreatePreUser> createPreUsers) {
    List<PreUser> toCreate = createPreUsers.stream()
        .map(mapper::toDomain)
        .toList();
    return service.createPreUsers(toCreate).stream()
        .map(mapper::toRest)
        .toList();
  }
}
