package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.ProspectRestMapper;
import app.bpartners.api.endpoint.rest.model.ExtendedProspectStatus;
import app.bpartners.api.endpoint.rest.model.Prospect;
import app.bpartners.api.endpoint.rest.model.UpdateProspect;
import app.bpartners.api.service.ProspectService;
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
public class ProspectController {
  private final ProspectService prospectService;
  private final ProspectRestMapper prospectRestMapper;

  @GetMapping("/accountHolders/{ahId}/prospects")
  public List<Prospect> getProspects(@PathVariable("ahId") String accountHolderId,
                                     @RequestParam(name = "name", required = false)
                                     String name,
                                     @RequestParam(name = "contactNature", required = false)
                                     String contactNature) {
    return prospectService.getByCriteria(accountHolderId, name, contactNature).stream()
        .map(prospectRestMapper::toRest)
        .toList();
  }

  @PutMapping("/accountHolders/{ahId}/prospects")
  public List<Prospect> crupdateProspects(@PathVariable("ahId") String accountHolderId,
                                          @RequestBody List<UpdateProspect> prospects) {
    List<app.bpartners.api.model.prospect.Prospect> prospectList = prospects.stream()
        .map(prospect -> prospectRestMapper.toDomain(accountHolderId, prospect))
        .toList();
    return prospectService.saveAll(prospectList).stream()
        .map(prospectRestMapper::toRest)
        .toList();
  }

  @PutMapping("/accountHolders/{ahId}/prospects/{id}")
  public Prospect updateProspectsStatus(@PathVariable("ahId") String accountHolderId,
                                        @PathVariable("id") String prospectId,
                                        @RequestBody ExtendedProspectStatus toUpdate) {
    app.bpartners.api.model.prospect.Prospect prospect =
        prospectRestMapper.toDomain(accountHolderId, toUpdate);
    return prospectRestMapper.toRest(prospectService.update(prospect));
  }
}