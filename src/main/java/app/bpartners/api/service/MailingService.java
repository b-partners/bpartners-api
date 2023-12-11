package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventConf;
import app.bpartners.api.endpoint.rest.model.EmailStatus;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.Email;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.EmailRepository;
import app.bpartners.api.service.aws.SesService;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Service
@AllArgsConstructor
@Slf4j
public class MailingService {
  private final SesService sesService;
  private final EmailRepository emailRepository;
  private final UserService userService;
  private final EventConf eventConf;

  public List<Email> getEmailsByUserId(String userId) {
    return emailRepository.findAllByUserId(userId);
  }

  public List<Email> editOrSendEmails(List<Email> emails) {
    checkEmailEditionValidity(emails);

    List<Email> savedEmails = emailRepository.saveAll(emails);
    Map<String, List<Email>> emailByUser = dispatchEmailByUser(savedEmails);

    emailByUser.forEach(
        (idUser, emailList) -> {
          User user = userService.getUserById(idUser);
          AccountHolder accountHolder = user.getDefaultHolder();
          for (Email email : emailList) {
            if (email.getStatus() == EmailStatus.SENT) {
              try {
                String recipient = email.getRecipient();
                String concerned = accountHolder.getEmail();
                String object = email.getObject();
                String body = email.getBody();
                List<Attachment> attachments = email.getAttachments();
                String invisibleRecipient = eventConf.getAdminEmail();
                sesService.sendEmail(
                    recipient,
                    concerned,
                    object,
                    body,
                    attachments,
                    invisibleRecipient);
                log.info("Email sent to {} from {} with object {}", recipient, concerned, object);
              } catch (IOException | MessagingException e) {
                log.error("Unable to sent email {}", email);
                throw new ApiException(SERVER_EXCEPTION, e);
              }
            }
          }
        });

    return savedEmails;
  }

  private void checkEmailEditionValidity(List<Email> emails) {
    StringBuilder msgBuilder = new StringBuilder();
    emails.forEach(actualEmail -> {
      Email existingEmail = emailRepository.findById(actualEmail.getId());
      if (existingEmail != null
          && existingEmail.getStatus() == EmailStatus.SENT
          && actualEmail.getStatus() == EmailStatus.DRAFT) {
        msgBuilder.append("Unable to edit email ")
            .append(actualEmail.describe())
            .append(" because it was already sent. ");
      }
      if (actualEmail.getStatus() == EmailStatus.SENT) {
        actualEmail.setSendingDatetime(Instant.now());
      }
    });
    String msgException = msgBuilder.toString();
    if (!msgException.isEmpty()) {
      throw new BadRequestException(msgException);
    }
  }

  private Map<String, List<Email>> dispatchEmailByUser(List<Email> savedEmails) {
    Map<String, List<Email>> emailByUser = new HashMap<>();
    for (Email e : savedEmails) {
      String idUser = e.getIdUser();
      if (idUser != null) {
        if (!emailByUser.containsKey(idUser)) {
          List<Email> subList = new ArrayList<>();
          subList.add(e);
          emailByUser.put(idUser, subList);
        } else {
          emailByUser.get(idUser).add(e);
        }
      }
    }
    return emailByUser;
  }
}
