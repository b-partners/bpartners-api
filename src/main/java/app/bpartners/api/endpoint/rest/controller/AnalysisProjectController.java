package app.bpartners.api.endpoint.rest.controller;

import static app.bpartners.api.model.project.AnalysisProjectStep.MODEL_3D;

import app.bpartners.api.endpoint.rest.mapper.AnalysisProjectRestMapper;
import app.bpartners.api.endpoint.rest.model.AnalysisProject;
import app.bpartners.api.endpoint.rest.model.Register2d;
import app.bpartners.api.endpoint.rest.model.Register3d;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.project.AnalysisProjectDescription;
import app.bpartners.api.service.detection.DetectionTrackingService;
import app.bpartners.api.service.project.AnalysisProjectService;
import java.util.List;
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
  private final DetectionTrackingService detectionTrackingService;
  private final AnalysisProjectRestMapper mapper;

  @GetMapping("/users/{userId}/analysisProjects")
  public List<AnalysisProject> listAnalysisProjects(
      @PathVariable String userId,
      @RequestParam(required = false) String projectKey,
      @RequestParam(required = false) String externalReference,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) PageFromOne page,
      @RequestParam(required = false) BoundedPageSize pageSize) {
    return analysisProjectService
        .list(userId, projectKey, externalReference, search, page, pageSize)
        .stream()
        .map(mapper::toRest)
        .toList();
  }

  @GetMapping("/users/{userId}/analysisProjects/{projectKey}")
  public AnalysisProject getAnalysisProject(
      @PathVariable String userId,
      @PathVariable String projectKey,
      @RequestParam(required = false) String externalReference) {
    return mapper.toRest(analysisProjectService.getStatus(userId, projectKey, externalReference));
  }

  @PutMapping("/users/{userId}/analysisProjects/{projectKey}/2d")
  public AnalysisProject registerAnalysisProject2d(
      @PathVariable String userId, @PathVariable String projectKey, @RequestBody Register2d body) {
    var tracking = mapper.toTracking(projectKey, body, AuthProvider.getAuthenticatedUser());
    detectionTrackingService.computeTrackingWithSubscriptionConsumptionLog(List.of(tracking));
    return mapper.toRest(
        analysisProjectService.getStatus(userId, projectKey, body.getExternalReference()));
  }

  @PutMapping("/users/{userId}/analysisProjects/{projectKey}/3d")
  public AnalysisProject registerAnalysisProject3d(
      @PathVariable String userId,
      @PathVariable String projectKey,
      @RequestBody(required = false) Register3d body) {
    var externalReference = body == null ? null : body.getExternalReference();
    var address = body == null ? null : body.getAddress();
    analysisProjectService.register(
        userId,
        projectKey,
        externalReference,
        MODEL_3D,
        "Modèle 3D : " + (address == null ? projectKey : address),
        AnalysisProjectDescription.ofAddress(address));
    return mapper.toRest(analysisProjectService.getStatus(userId, projectKey, externalReference));
  }
}
