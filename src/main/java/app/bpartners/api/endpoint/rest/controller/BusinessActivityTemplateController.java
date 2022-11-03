package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.BusinessActivityTemplateRestMapper;
import app.bpartners.api.endpoint.rest.model.BusinessActivity;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.BusinessActivityTemplateService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class BusinessActivityTemplateController {
  private final BusinessActivityTemplateRestMapper mapper;
  private final BusinessActivityTemplateService templateService;

  @GetMapping("/businessActivities")
  public List<BusinessActivity> getBusinessActivities(
      @RequestParam PageFromOne page,
      @RequestParam BoundedPageSize pageSize) {
    return templateService.getBusinessActivities(page, pageSize).stream()
        .map(mapper::toRest).collect(Collectors.toUnmodifiableList());
  }

}
