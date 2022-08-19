package app.bpartners.api.endpoint.rest.controller;


import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.model.entity.BoundedPageSize;
import app.bpartners.api.model.entity.PageFromOne;
import app.bpartners.api.model.mapper.PreUserMapper;
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
  private final PreUserMapper mapper;
  private final app.bpartners.api.endpoint.rest.mapper.PreUserMapper createMapper;

  @GetMapping("/preUsers")
  public List<app.bpartners.api.endpoint.rest.model.PreUser> getPreUsers(
      @RequestParam PageFromOne page,
      @RequestParam("page_size") BoundedPageSize pageSize
  ) {
    return service.getPreUsers(page, pageSize).stream().map(mapper::DtoRest).toList();
  }

  @PostMapping("/preUsers")
  public List<app.bpartners.api.endpoint.rest.model.PreUser> createPreUser(
      @RequestBody List<CreatePreUser> createPreUsers) {

    return service.createPreUsers(createPreUsers.stream().map(createMapper::toDomain).toList())
        .stream().map(mapper::DtoRest).toList();
  }
}
