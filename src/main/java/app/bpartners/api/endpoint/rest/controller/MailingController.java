package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.MailingRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateEmail;
import app.bpartners.api.endpoint.rest.model.Email;
import app.bpartners.api.service.MailingService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class MailingController {
  private final MailingService mailingService;
  private final MailingRestMapper mapper;

  @GetMapping("/users/{userId}/emails")
  public List<Email> readEmails(@PathVariable String userId) {
    return mailingService.getEmailsByUserId(userId).stream()
        .map(mapper::toRest)
        .collect(Collectors.toList());
  }

  @PutMapping("/users/{userId}/emails")
  public List<Email> editOrSendEmails(
      @PathVariable String userId,
      @RequestBody List<CreateEmail> createEmailList) {
    List<app.bpartners.api.model.Email> emailList = createEmailList.stream()
        .map(email -> mapper.toDomain(userId, email))
        .toList();
    return mailingService.editOrSendEmails(emailList).stream()
        .map(mapper::toRest)
        .collect(Collectors.toList());
  }
}
