package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.ANALYSIS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.endpoint.event.model.UserOnboardedNotificationRequested;
import app.bpartners.api.endpoint.rest.model.UserApiKey;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.User;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.user.UserService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class UserOnboardedNotificationRequestedServiceTest {
  private static final String ADMIN_EMAIL = "admin@email.com";
  SesService sesServiceMock = mock();
  TemplateResolverEngine templateResolverEngine = new TemplateResolverEngine();
  SesConf sesConfMock = mock();
  UserService userServiceMock = mock();
  UserOnboardedNotificationRequestedService subject =
      new UserOnboardedNotificationRequestedService(
          sesServiceMock, templateResolverEngine, sesConfMock, userServiceMock);

  @SneakyThrows
  @Test
  void trigger_email_notification_with_analysis_key() {
    var userId = randomUUID().toString();
    var userApiKey = randomUUID().toString();
    var holderIdentifier = randomUUID().toString();
    var userMock = mock(User.class);
    var accountHolderMock = mock(AccountHolder.class);
    var userApiKeyMock = mock(UserApiKey.class);

    when(userMock.getId()).thenReturn(userId);
    when(userMock.getName()).thenReturn("John Doe");
    when(userMock.getFirstName()).thenReturn("John");
    when(userMock.getLastName()).thenReturn("Doe");
    when(userMock.getEmail()).thenReturn("john.doe@mail.com");
    when(userMock.getMobilePhoneNumber()).thenReturn("0612345678");
    when(accountHolderMock.getName()).thenReturn("John Doe");
    when(accountHolderMock.getId()).thenReturn(holderIdentifier);
    when(userMock.getDefaultHolder()).thenReturn(accountHolderMock);
    when(userApiKeyMock.getKey()).thenReturn(userApiKey);
    when(sesConfMock.getAdminEmail()).thenReturn(ADMIN_EMAIL);
    when(userServiceMock.getUserById(userId)).thenReturn(userMock);
    when(userServiceMock.getApiKeys(eq(userMock), eq(List.of(ANALYSIS))))
        .thenReturn(List.of(userApiKeyMock));

    assertDoesNotThrow(() -> subject.accept(new UserOnboardedNotificationRequested(userId)));

    verify(sesServiceMock)
        .sendEmail(
            eq(ADMIN_EMAIL),
            eq(null),
            eq("[birdia] Nouvelle inscription sur dashboard : John Doe"),
            eq(getHtmlBody(userId, holderIdentifier, List.of(userApiKey))),
            eq(List.of()),
            eq("tech@birdia.fr"));
  }

  @SneakyThrows
  @Test
  void trigger_email_notification_without_retrieved_analysis_key() {
    var userId = randomUUID().toString();
    var holderIdentifier = randomUUID().toString();
    var userMock = mock(User.class);
    var accountHolderMock = mock(AccountHolder.class);

    when(userMock.getId()).thenReturn(userId);
    when(userMock.getName()).thenReturn("John Doe");
    when(userMock.getFirstName()).thenReturn("John");
    when(userMock.getLastName()).thenReturn("Doe");
    when(userMock.getEmail()).thenReturn("john.doe@mail.com");
    when(userMock.getMobilePhoneNumber()).thenReturn("0612345678");
    when(accountHolderMock.getName()).thenReturn("John Doe");
    when(accountHolderMock.getId()).thenReturn(holderIdentifier);
    when(userMock.getDefaultHolder()).thenReturn(accountHolderMock);
    when(sesConfMock.getAdminEmail()).thenReturn(ADMIN_EMAIL);
    when(userServiceMock.getUserById(userId)).thenReturn(userMock);
    when(userServiceMock.getApiKeys(eq(userMock), eq(List.of(ANALYSIS))))
        .thenThrow(RuntimeException.class);

    assertDoesNotThrow(() -> subject.accept(new UserOnboardedNotificationRequested(userId)));

    verify(sesServiceMock)
        .sendEmail(
            eq(ADMIN_EMAIL),
            eq(null),
            eq("[birdia] Nouvelle inscription sur dashboard : John Doe"),
            eq(getHtmlBody(userId, holderIdentifier, List.of())),
            eq(List.of()),
            eq("tech@birdia.fr"));
  }

  private String getHtmlBody(String userId, String accountHolderId, List<String> analysisApiKeys) {
    return String.format(
        """
<html xmlns="http://www.w3.org/1999/html">
<head>
    <title>Onboarded user mail</title>
    <style>
        * {
            font-family: Arial, Verdana, Georgia, and Courier, serif;
            color: black;
        }

        h1, section, footer {
            margin: 2vh 2vw;
        }

        h1 {
            font-size: 1.2em;
            font-weight: 500;
        }
    </style>
</head>
<body>
<p>
    Bonjour,
</p>
<p> Un nouvel artisan s'est inscrit sur notre plateforme. Voici les informations que nous avons
    récoltées à travers son inscription :</p>
<p><strong><u>Nom de la société</u> :</strong> <span>John Doe</span></p>
<p><strong><u>Informations concernant le compte</u> :</strong></p>
<ul>
    <li>ID User : <span>%s</span></li>
    <li>ID Account Holder : <span>%s</span></li>
    <li>Nom(s) : <span>John</span></li>
    <li>Prénom(s) : <span>Doe</span></li>
    <li>Email : <span>john.doe@mail.com</span></li>
    <li>Numéro de téléphone : <span>0612345678</span></li>
    <li>Clés API pour Birdia API (bouton couvreur) :
        <span>%s</span>
    </li>
</ul>
<p>Cordialement,</p>
<p>L'équipe Birdia.</p>
</body>
</html>""",
        userId, accountHolderId, analysisApiKeys.isEmpty() ? "Aucun" : analysisApiKeys);
  }
}
