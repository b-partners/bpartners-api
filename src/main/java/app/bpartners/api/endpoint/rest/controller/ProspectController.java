package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.ProspectRestMapper;
import app.bpartners.api.endpoint.rest.model.EvaluatedProspect;
import app.bpartners.api.endpoint.rest.model.Prospect;
import app.bpartners.api.endpoint.rest.model.ProspectConversion;
import app.bpartners.api.endpoint.rest.model.UpdateProspect;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.utils.ProspectEvalUtils;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.service.ProspectService;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static java.util.stream.Collectors.toUnmodifiableList;

@RestController
@AllArgsConstructor
public class ProspectController {
  private final ProspectService service;
  private final ProspectRestMapper mapper;
  private final ProspectEvalUtils prospectUtils;

  @PutMapping("/accountHolders/{ahId}/prospects/{id}/prospectConversion")
  public List<ProspectConversion> convertProspect(
      @PathVariable("ahId") String accountHolderId, @PathVariable("id") String prospectId,
      @RequestBody List<ProspectConversion> prospectConversion) {
    //TODO: what should we do here ?
    throw new NotImplementedException("prospect conversion not implemented yet");
  }

  @GetMapping("/accountHolders/{ahId}/prospects")
  public List<Prospect> getProspects(@PathVariable("ahId") String accountHolderId) {
    return service.getAllByIdAccountHolder(accountHolderId).stream()
        .map(mapper::toRest)
        .collect(toUnmodifiableList());
  }

  @PutMapping("/accountHolders/{ahId}/prospects")
  public List<Prospect> updateProspects(@PathVariable("ahId") String accountHolderId,
                                        @RequestBody List<UpdateProspect> prospects) {
    List<app.bpartners.api.model.Prospect> prospectList = prospects.stream()
        .map(mapper::toDomain)
        .collect(toUnmodifiableList());
    return service.saveAll(prospectList).stream()
        .map(mapper::toRest)
        .collect(toUnmodifiableList());
  }

  @PostMapping("/accountHolders/{ahId}/prospects/prospectsEvaluation")
  public List<EvaluatedProspect> updateProspects(
      @PathVariable("ahId") String accountHolderId,
      @RequestBody byte[] toEvaluate) {
    List<ProspectEval> prospectEvals =
        prospectUtils.convertFromExcel(new ByteArrayInputStream(toEvaluate));
    return service.evaluateProspects(prospectEvals).stream()
        .map(mapper::toRest)
        .collect(Collectors.toList());
  }

}