package app.bpartners.api.repository.mapper;

import app.bpartners.api.repository.sendinblue.model.Attributes;
import app.bpartners.api.repository.sendinblue.model.Contact;
import com.google.gson.internal.LinkedTreeMap;
import org.springframework.stereotype.Component;
import sibModel.CreateContact;
import sibModel.GetExtendedContactDetails;

@Component
public class SendinblueMapper {

  public Contact toDomain(GetExtendedContactDetails contactDetails) {
    Attributes attributes =
        mapAttributes((LinkedTreeMap<Object, Object>) contactDetails.getAttributes());
    return Contact.builder()
        .email(contactDetails.getEmail())
        .emailBlackListed(contactDetails.isEmailBlacklisted())
        .smsBlackListed(contactDetails.isSmsBlacklisted())
        .attributes(attributes)
        .listIds(contactDetails.getListIds())
        .build();
  }

  public CreateContact toExternalCreateContact(Contact domain) {
    return new CreateContact()
        .email(domain.getEmail())
        .emailBlacklisted(domain.isEmailBlacklisted())
        .smsBlacklisted(domain.isSmsBlacklisted())
        .listIds(domain.getListIds())
        .updateEnabled(domain.isUpdateEnabled())
        .smtpBlacklistSender(domain.getSmtpBlacklisted())
        .attributes(mapAttributes(domain.getAttributes()));
  }

  private LinkedTreeMap<String, String> mapAttributes(Attributes attributes) {
    String phoneNumber = null;
    if (attributes.getPhone() != null) {
      phoneNumber = attributes.getPhone().toString();
    }
    LinkedTreeMap<String, String> attributesMap = new LinkedTreeMap<>();
    attributesMap.put(Attributes.JSON_PROPERTY_ID, attributes.getId().toString());
    attributesMap.put(Attributes.JSON_PROPERTY_LASTNAME, attributes.getLastName());
    attributesMap.put(Attributes.JSON_PROPERTY_FIRSTNAME, attributes.getFirstName());
    attributesMap.put(Attributes.JSON_PROPERTY_PHONE, phoneNumber);
    attributesMap.put(Attributes.JSON_PROPERTY_SMS, attributes.getSmsPhoneNumber());
    return attributesMap;
  }

  private Attributes mapAttributes(LinkedTreeMap<Object, Object> detailsAttributes) {
    return Attributes.builder()
        .id((Double) detailsAttributes.get(Attributes.JSON_PROPERTY_ID))
        .firstName((String) detailsAttributes.get(Attributes.JSON_PROPERTY_FIRSTNAME))
        .lastName((String) detailsAttributes.get(Attributes.JSON_PROPERTY_LASTNAME))
        .phone((Double) detailsAttributes.get(Attributes.JSON_PROPERTY_PHONE))
        .smsPhoneNumber((String) detailsAttributes.get(Attributes.JSON_PROPERTY_SMS))
        .build();
  }
}
