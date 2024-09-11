package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.CustomerStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.CustomerType.PROFESSIONAL;
import static app.bpartners.api.endpoint.rest.model.ProspectStatus.CONVERTED;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.endpoint.event.model.ProspectUpdated;
import app.bpartners.api.endpoint.rest.model.ProspectFeedback;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Location;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.jpa.HasCustomerJpaRepository;
import app.bpartners.api.repository.jpa.model.HHasCustomer;
import app.bpartners.api.service.CustomerService;
import app.bpartners.api.service.UserService;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.GeoUtils;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
@Slf4j
public class ProspectUpdatedService implements Consumer<ProspectUpdated> {
  public static final String PROSPECT_UPDATED_TEMPLATE = "prospect_updated";
  private final AccountHolderRepository holderRepository;
  private final CustomerService customerService;
  private final UserService userService;
  private final SesService sesService;
  private final SesConf sesConf;
  private final HasCustomerJpaRepository hasCustomerJpaRepository;
  private final TemplateResolverEngine templateResolverEngine;
  private final CustomDateFormatter customDateFormatter;

  @Override
  public void accept(ProspectUpdated prospectUpdated) {
    Prospect prospect = prospectUpdated.getProspect();
    AccountHolder accountHolder =
        prospect.isGivenUp()
            ? holderRepository.findById(prospect.getLatestOldHolder())
            : holderRepository.findById(prospect.getIdHolderOwner());
    String userId = accountHolder.getUserId();
    crupdateAndLinkCustomerToProspect(userService.getUserById(userId), prospect);
    ProspectUpdateType updateType =
        prospect.isGivenUp() ? ProspectUpdateType.GIVE_UP : ProspectUpdateType.CONTINUE_PROCESS;
    Instant updatedAt = prospectUpdated.getUpdatedAt();
    try {
      String recipient = sesConf.getAdminEmail();
      String concerned = null;
      String frenchUpdatedDatetime = customDateFormatter.formatFrenchDatetime(updatedAt);
      String translatedStatus = getTranslatedStatus(prospect.getActualStatus());
      String translatedFeedback = getTranslatedFeedBack(prospect.getProspectFeedback());
      String subject =
          prospect.isGivenUp()
              ? String.format(
                  "Le prospect intitulé %s a été abandonné par l'artisan %s le %s",
                  prospect.getName(), accountHolder.getName(), frenchUpdatedDatetime)
              : String.format(
                  "Le prospect intitulé %s appartenant à l'artisan %s est passé en statut %s le %s",
                  prospect.getName(),
                  accountHolder.getName(),
                  translatedStatus,
                  frenchUpdatedDatetime);
      String htmlBody =
          htmlBody(
              prospect,
              accountHolder,
              translatedStatus,
              frenchUpdatedDatetime,
              translatedFeedback,
              updateType);
      List<Attachment> attachments = List.of();
      sesService.sendEmail(recipient, concerned, subject, htmlBody, attachments);
      log.info("{} updated and mail sent to recipient={}", prospect.describe(), recipient);
    } catch (IOException | MessagingException e) {
      log.warn("Unable to send email after " + prospect + " update. Exception was :" + e);
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  private String getTranslatedStatus(ProspectStatus prospectStatus) {
    String translatedStatus =
        switch (prospectStatus) {
          case TO_CONTACT -> "À contacter";
          case CONTACTED -> "Contacté";
          case CONVERTED -> "Converti";
        };
    return translatedStatus.toUpperCase();
  }

  private String getTranslatedFeedBack(ProspectFeedback feedback) {
    if (feedback == null) {
      return null;
    }
    String translatedFeedback =
        switch (feedback) {
          case NOT_INTERESTED -> "Pas intéressé";
          case INTERESTED -> "Interessé";
          case PROPOSAL_SENT -> "Devis envoyé";
          case PROPOSAL_ACCEPTED -> "Devis accepté";
          case PROPOSAL_DECLINED -> "Devis refusé";
          case INVOICE_SENT -> "Facture envoyée";
        };
    return translatedFeedback.toUpperCase();
  }

  private String htmlBody(
      Prospect prospect,
      AccountHolder accountHolder,
      String translatedStatus,
      String frenchUpdatedDatetime,
      String translatedFeedback,
      ProspectUpdateType updateType) {
    Context context = new Context();
    context.setVariable("updateType", updateType);
    context.setVariable("prospect", prospect);
    context.setVariable("translatedFeedback", translatedFeedback);
    context.setVariable("accountHolder", accountHolder);
    context.setVariable("translatedStatus", translatedStatus);
    context.setVariable("frenchUpdatedDatetime", frenchUpdatedDatetime);
    return templateResolverEngine.parseTemplateResolver(PROSPECT_UPDATED_TEMPLATE, context);
  }

  private void crupdateAndLinkCustomerToProspect(User owner, Prospect prospect) {
    Optional<Customer> optionalLinkedCustomer = customerService.getByProspectId(prospect.getId());
    var crupdatedCustomer =
        customerService
            .crupdateCustomers(
                owner, List.of(customerFrom(prospect, optionalLinkedCustomer, owner)))
            .getFirst();
    if (optionalLinkedCustomer.isEmpty()) {
      hasCustomerJpaRepository.save(
          HHasCustomer.builder()
              .idProspect(prospect.getId())
              .idCustomer(crupdatedCustomer.getId())
              .build());
    }
    log.info("{} updated ", crupdatedCustomer.describe());
  }

  private Customer customerFrom(
      Prospect prospect, Optional<Customer> optionalLinkedCustomer, User owner) {
    var isConverted = CONVERTED.equals(prospect.getActualStatus());
    if (optionalLinkedCustomer.isPresent()) {
      Customer persistedCustomer = optionalLinkedCustomer.get();
      return persistedCustomer.toBuilder()
          .isConverted(isConverted)
          .firstName(prospect.getFirstName())
          .lastName(prospect.getName())
          .name(prospect.getName())
          .address(prospect.getAddress())
          .build();
    }
    var prospectLocation = prospect.getLocation();
    Double longitude = prospectLocation == null ? null : prospectLocation.getLongitude();
    Double latitude = prospectLocation == null ? null : prospectLocation.getLatitude();
    Location location =
        Location.builder()
            .address(prospect.getAddress())
            .longitude(longitude)
            .latitude(latitude)
            .coordinate(
                GeoUtils.Coordinate.builder().longitude(longitude).latitude(latitude).build())
            .build();
    return Customer.builder()
        .id(randomUUID().toString())
        .email(prospect.getEmail())
        .firstName(prospect.getFirstName())
        .lastName(prospect.getName())
        .name(prospect.getName())
        .address(prospect.getAddress())
        .phone(prospect.getPhone())
        .comment(prospect.getComment())
        .isConverted(isConverted)
        .location(location)
        .idUser(owner.getId())
        .status(ENABLED)
        // TODO: this defaults to PROFESSIONAL for now
        .customerType(PROFESSIONAL)
        .build();
  }

  enum ProspectUpdateType {
    GIVE_UP,
    CONTINUE_PROCESS
  }
}
