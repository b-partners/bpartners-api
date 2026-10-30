package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.SecondaryUserRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateSecondaryUser;
import app.bpartners.api.endpoint.rest.model.SecondaryUser;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.subscription.SecondaryUserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SecondaryUserController {
  private final SecondaryUserService service;
  private final SecondaryUserRestMapper mapper;

  @GetMapping("/users/{uId}/secondaryUsers")
  public List<SecondaryUser> getSecondaryUsers(@PathVariable String uId) {
    return service.findAllByPrimaryUserId(uId).stream().map(mapper::toRest).toList();
  }

  @PostMapping("/users/{uId}/secondaryUsers")
  public SecondaryUser inviteSecondaryUser(
      @PathVariable String uId, @RequestBody(required = false) CreateSecondaryUser body) {
    if (body == null) {
      throw new BadRequestException("Request body is mandatory.");
    }
    return mapper.toRest(service.invite(uId, body.getEmail()));
  }

  @DeleteMapping("/users/{uId}/secondaryUsers/{secondaryUserId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeSecondaryUser(@PathVariable String uId, @PathVariable String secondaryUserId) {
    service.remove(uId, secondaryUserId);
  }

  @GetMapping("/users/{uId}/secondaryUserInvitations")
  public List<SecondaryUser> getSecondaryUserInvitations(@PathVariable String uId) {
    return service.findPendingInvitationsOf(uId).stream().map(mapper::toRest).toList();
  }

  @PutMapping("/users/{uId}/secondaryUserInvitations/{invitationId}")
  public SecondaryUser acceptSecondaryUserInvitation(
      @PathVariable String uId, @PathVariable String invitationId) {
    return mapper.toRest(service.acceptInvitation(uId, invitationId));
  }
}
