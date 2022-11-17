package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.LegalFileRestMapper;
import app.bpartners.api.endpoint.rest.model.LegalFile;
import app.bpartners.api.endpoint.rest.validator.LegalFileRestValidator;
import app.bpartners.api.service.LegalFileService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class LegalFileController {
  private final LegalFileService service;
  private final LegalFileRestMapper mapper;
  private final LegalFileRestValidator validator;

  @GetMapping("/users/{id}/legalFiles")
  public List<LegalFile> getLegalFiles(@PathVariable(name = "id") String userId) {
    return service.getLegalFiles(userId).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PutMapping("/users/{id}/legalFiles/{lId}")
  public LegalFile approveLegalFile(
      @PathVariable(name = "id") String userId,
      @PathVariable(name = "lId") String legalFileId) {
    validator.accept(legalFileId);
    return mapper.toRest(service.approveLegalFile(userId, legalFileId));
  }
}
