package app.bpartners.api.endpoint.rest.controller;

import static java.util.stream.Collectors.toUnmodifiableList;

import app.bpartners.api.endpoint.rest.mapper.AreaPictureAnnotationRestMapper;
import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.validator.LatLonDataToPixelValidator;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.annotation.LatLonPolygonToPixelConverter;
import app.bpartners.api.service.areapicture.AreaPictureAnnotationService;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AreaPictureAnnotationController {
  private final AreaPictureAnnotationService service;
  private final AreaPictureAnnotationRestMapper mapper;
  private final LatLonPolygonToPixelConverter latLonPolygonToPixelConverter;
  private final LatLonDataToPixelValidator latLonDataToPixelValidator;

  @GetMapping("/accounts/{aId}/areaPictures/{areaPictureId}/annotations")
  public List<AreaPictureAnnotation> getAreaPictureAnnotations(
      @PathVariable String aId,
      @PathVariable String areaPictureId,
      @RequestParam(defaultValue = "1", required = false) PageFromOne page,
      @RequestParam(defaultValue = "10", required = false) BoundedPageSize pageSize) {
    var authenticatedUserId = AuthProvider.getAuthenticatedUserId();
    return service.findAllCompleted(authenticatedUserId, areaPictureId, page, pageSize).stream()
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

  @GetMapping("/accounts/{aId}/areaPictures/{areaPictureId}/annotations/drafts")
  public List<DraftAreaPictureAnnotation> getDraftAreaPictureAnnotationsByAccountIdAndAreaPictureId(
      @PathVariable String aId,
      @PathVariable String areaPictureId,
      @RequestParam(defaultValue = "1", required = false) PageFromOne page,
      @RequestParam(defaultValue = "10", required = false) BoundedPageSize pageSize) {
    var authenticatedUserId = AuthProvider.getAuthenticatedUserId();
    return service
        .findAllDraftByAccountIdAndAreaPictureId(authenticatedUserId, areaPictureId, page, pageSize)
        .stream()
        .map(annotation -> mapper.toRestDraft(authenticatedUserId, annotation))
        .toList();
  }

  @GetMapping("/accounts/{aId}/annotations/drafts")
  public List<DraftAreaPictureAnnotation> getDraftAreaPictureAnnotationsByAccountId(
      @PathVariable String aId,
      @RequestParam(defaultValue = "1", required = false) PageFromOne page,
      @RequestParam(defaultValue = "10", required = false) BoundedPageSize pageSize) {
    var authenticatedUserId = AuthProvider.getAuthenticatedUserId();
    return service.findAllDraftByAccountId(authenticatedUserId, page, pageSize).stream()
        .map(annotation -> mapper.toRestDraft(authenticatedUserId, annotation))
        .toList();
  }

  @PostMapping("/accounts/{aId}/annotations/exports")
  public PreSignedURL exportAreaPictureAnnotationToPdf(
      @PathVariable(name = "aId") String ignored,
      @RequestBody ExportAreaPictureAnnotation exportAreaPictureAnnotation) {
    var userId = AuthProvider.getAuthenticatedUserId();
    return service.exportAreaPictureAnnotationToPdf(userId, exportAreaPictureAnnotation);
  }

  @PostMapping("/accounts/{aId}/annotations/convert")
  public Map<String, ConverterAnnotation> convertLatLonPolygonToPixel(
      @PathVariable(name = "aId") String ignored,
      @RequestBody Map<String, ConverterAnnotation> converterAnnotationMap) {

    latLonDataToPixelValidator.accept(converterAnnotationMap);
    return latLonPolygonToPixelConverter.apply(converterAnnotationMap);
  }
}
