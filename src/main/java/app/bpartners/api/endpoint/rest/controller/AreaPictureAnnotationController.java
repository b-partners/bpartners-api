package app.bpartners.api.endpoint.rest.controller;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import app.bpartners.api.endpoint.rest.mapper.AreaPictureAnnotationRestMapper;
import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.validator.ConverterValidator;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.annotation.AreaPictureAnnotationConverter;
import app.bpartners.api.service.areapicture.AreaPictureAnnotationService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@AllArgsConstructor
public class AreaPictureAnnotationController {
  private final AreaPictureAnnotationService service;
  private final AreaPictureAnnotationRestMapper mapper;
  private final AreaPictureAnnotationConverter areaPictureAnnotationConverter;
  private final ConverterValidator converterValidator;

  @GetMapping("/accounts/{aId}/areaPictures/{areaPictureId}/annotations")
  public List<AreaPictureAnnotation> getAreaPictureAnnotations(
      @PathVariable String aId,
      @PathVariable String areaPictureId,
      @RequestParam(defaultValue = "1", required = false) PageFromOne page,
      @RequestParam(defaultValue = "10", required = false) BoundedPageSize pageSize) {
    var authenticatedUserId = AuthProvider.getAuthenticatedUserId();
    return service.findAllCompleted(authenticatedUserId, areaPictureId, page, pageSize).stream()
        .map(mapper::toRest)
        .toList();
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

  @PostMapping(value = "/accounts/{aId}/annotations/exports", consumes = MULTIPART_FORM_DATA_VALUE)
  public PreSignedURL exportAreaPictureAnnotationToPdf(
      @PathVariable(name = "aId") String ignored,
      @RequestPart(value = "data") ExportAreaPictureAnnotation annotation,
      @RequestPart(value = "globalImage3D", required = false) MultipartFile globalImage3D)
      throws IOException {
    var userId = AuthProvider.getAuthenticatedUserId();
    byte[] globalImageBytes = globalImage3D != null ? globalImage3D.getBytes() : null;

    return service.exportAreaPictureAnnotationToPdf(userId, annotation, globalImageBytes);
  }

  @PostMapping("/accounts/{aId}/annotations/convert")
  public Map<String, ConverterAnnotation> convertLatLonPolygonToPixel(
      @PathVariable(name = "aId") String ignored,
      @RequestBody Map<String, ConverterAnnotation> converterAnnotationMap) {

    converterValidator.accept(converterAnnotationMap);
    return areaPictureAnnotationConverter.toPixel(converterAnnotationMap);
  }

  @PostMapping("/accounts/{aId}/annotations/lon-lat/convert")
  public Map<String, ConverterAnnotation> convertPixelToLatLon(
      @PathVariable(name = "aId") String ignored,
      @RequestBody Map<String, ConverterAnnotation> converterAnnotationMap) {

    converterValidator.accept(converterAnnotationMap);
    return areaPictureAnnotationConverter.toLatLong(converterAnnotationMap);
  }
}
