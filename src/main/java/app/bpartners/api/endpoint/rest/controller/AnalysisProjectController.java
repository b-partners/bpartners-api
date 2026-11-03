package app.bpartners.api.endpoint.rest.controller;

import static app.bpartners.api.model.project.AnalysisProjectStep.MODEL_3D;

import app.bpartners.api.endpoint.rest.mapper.AnalysisProjectRestMapper;
import app.bpartners.api.endpoint.rest.model.AnalysisProject;
import app.bpartners.api.endpoint.rest.model.RegisterModel3d;
import app.bpartners.api.service.project.AnalysisProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnalysisProjectController {
  private final AnalysisProjectService analysisProjectService;
  private final AnalysisProjectRestMapper mapper;

  @GetMapping("/users/{userId}/analysisProjects")
  public AnalysisProject getAnalysisProject(
      @PathVariable String userId,
      @RequestParam(required = false) String projectKey,
      @RequestParam(required = false) String externalReference) {
    return mapper.toRest(analysisProjectService.getStatus(userId, projectKey, externalReference));
  }

  @PutMapping("/users/{userId}/analysisProjects/{projectKey}/model3d")
  public AnalysisProject registerAnalysisProjectModel3d(
      @PathVariable String userId,
      @PathVariable String projectKey,
      @RequestBody(required = false) RegisterModel3d body) {
    var externalReference = body == null ? null : body.getExternalReference();
    var address = body == null ? null : body.getAddress();
    analysisProjectService.register(
        userId,
        projectKey,
        externalReference,
        MODEL_3D,
        "Modèle 3D : " + (address == null ? projectKey : address));
    return mapper.toRest(analysisProjectService.getStatus(userId, projectKey, externalReference));
  }
}
