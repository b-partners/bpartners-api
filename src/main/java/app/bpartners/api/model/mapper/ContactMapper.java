package app.bpartners.api.model.mapper;

import app.bpartners.api.model.PreUser;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.sendinblue.model.Attributes;
import app.bpartners.api.repository.sendinblue.model.Contact;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ContactMapper {
  private final SendinblueConf sendinblueConf;

  public app.bpartners.api.model.Contact toDomain(Contact external) {
    Attributes externalAttributes = external.getAttributes();
    return app.bpartners.api.model.Contact.builder()
        .id(externalAttributes.getId())
        .firstName(externalAttributes.getFirstName())
        .lastName(externalAttributes.getLastName())
        .phone(phoneStringValues(externalAttributes.getPhone()))
        .email(external.getEmail())
        .emailBlackListed(external.isEmailBlacklisted())
        .smsBlackListed(external.isSmsBlacklisted())
        .listIds(external.getListIds())
        .updateEnabled(external.isUpdateEnabled())
        .smtpBlackListed(external.getSmtpBlacklisted())
        .build();
  }

  public app.bpartners.api.model.Contact toDomain(Double id, PreUser preUser) {
    return app.bpartners.api.model.Contact.builder()
        .id(id)
        .firstName(preUser.getFirstname())
        .lastName(preUser.getLastname())
        .phone(preUser.getMobilePhoneNumber())
        .email(preUser.getEmail())
        .listIds(List.of(sendinblueConf.getContactListId()))
        //TODO: make this configurable or use correct configuration
        .emailBlackListed(false)
        .smsBlackListed(false)
        .updateEnabled(false)
        .smtpBlackListed(List.of())
        .build();
  }

  public Contact toSendinblueContact(app.bpartners.api.model.Contact domain) {
    Attributes contactAttributes = Attributes.builder()
        .id(domain.getId())
        .firstName(domain.getFirstName())
        .lastName(domain.getLastName())
        .phone(phoneDoubleValues(domain.getPhone()))
        .smsPhoneNumber(domain.getPhone())
        .build();
    return Contact.builder()
        .email(domain.getEmail())
        .emailBlackListed(domain.isEmailBlackListed())
        .smsBlackListed(domain.isSmsBlackListed())
        .listIds(domain.getListIds())
        .updateEnabled(domain.isUpdateEnabled())
        .smtpBlackListed(domain.getSmtpBlackListed())
        .attributes(contactAttributes)
        .build();
  }

  public Contact toSendinblueContact(Double id, PreUser preUser) {
    app.bpartners.api.model.Contact domainContact = toDomain(id, preUser);
    return toSendinblueContact(domainContact);
  }

  private Double phoneDoubleValues(String phone) {
    return Double.valueOf(phone.substring(1));
  }

  private String phoneStringValues(Double phone) {
    return "+" + phone;
  }
}
