package app.bpartners.api.endpoint.rest.controller;

import static java.util.stream.Collectors.toUnmodifiableList;

import app.bpartners.api.endpoint.rest.mapper.AreaPictureAnnotationRestMapper;
import app.bpartners.api.endpoint.rest.model.AreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.AreaPictureAnnotationService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AreaPictureAnnotationController {
  private final AreaPictureAnnotationService service;
  private final AreaPictureAnnotationRestMapper mapper;

  @GetMapping("/accounts/{aId}/areaPictures/{areaPictureId}/annotations")
  public List<AreaPictureAnnotation> getAreaPictureAnnotations(
      @PathVariable String areaPictureId,
      @RequestParam(defaultValue = "1", required = false) PageFromOne page,
      @RequestParam(defaultValue = "10", required = false) BoundedPageSize pageSize) {
    var authenticatedUserId = AuthProvider.getAuthenticatedUserId();
    return service.findAllBy(authenticatedUserId, areaPictureId, page, pageSize).stream()
        .map(mapper::toRest)
        .collect(toUnmodifiableList());
  }

  @GetMapping("/accounts/{aId}/areaPictures/{areaPictureId}/annotations/{annotationId}")
  public AreaPictureAnnotation getAreaPictureAnnotation(
      @PathVariable String areaPictureId, @PathVariable String annotationId) {
    var authenticatedUserId = AuthProvider.getAuthenticatedUserId();
    return mapper.toRest(service.findBy(authenticatedUserId, areaPictureId, annotationId));
  }

  @PutMapping("/accounts/{aId}/areaPictures/{areaPictureId}/annotations/{annotationId}")
  public AreaPictureAnnotation annotateAreaPicture(
      @PathVariable String annotationId, @RequestBody AreaPictureAnnotation toCreate) {
    var authenticatedUserId = AuthProvider.getAuthenticatedUserId();
    return mapper.toRest(
        service.save(mapper.toDomain(annotationId, authenticatedUserId, toCreate)));
  }
}
