package app.bpartners.api.endpoint.rest.controller;

import static java.util.stream.Collectors.toUnmodifiableList;

import app.bpartners.api.endpoint.rest.mapper.AreaPictureRestMapper;
import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.service.AreaPictureService;
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
public class AreaPictureController {
  private final AreaPictureService service;
  private final AreaPictureRestMapper mapper;

  @GetMapping(value = "/accounts/{accountId}/areaPictures")
  public List<AreaPictureDetails> findAllAreaPictures(
      @PathVariable(name = "accountId") String accountId,
      @RequestParam(required = false, defaultValue = "") String address,
      @RequestParam(required = false, defaultValue = "") String filename) {
    String userId = AuthProvider.getAuthenticatedUserId();
    return service.findAllBy(userId, address, filename).stream()
        .map(mapper::toRest)
        .collect(toUnmodifiableList());
  }

  @GetMapping(value = "/accounts/{accountId}/areaPictures/{id}")
  public AreaPictureDetails getAreaPictureById(
      @PathVariable(name = "accountId") String accountId,
      @PathVariable(name = "id") String areaPictureId) {
    String userId = AuthProvider.getAuthenticatedUserId();
    return mapper.toRest(service.findBy(userId, areaPictureId));
  }

  @PutMapping(value = "/accounts/{accountId}/areaPictures/{id}")
  public AreaPictureDetails crupdateAreaPictureDetails(
      @PathVariable(name = "accountId") String accountId,
      @PathVariable(name = "id") String areaPictureId,
      @RequestBody CrupdateAreaPictureDetails toCrupdate) {
    String userId = AuthProvider.getAuthenticatedUserId();
    var result =
        service.downloadFromExternalSourceAndSave(
            mapper.toDomain(toCrupdate, areaPictureId, userId));
    return mapper.toRest(result);
  }
}
