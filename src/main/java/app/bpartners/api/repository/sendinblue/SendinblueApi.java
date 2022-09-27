package app.bpartners.api.repository.sendinblue;

import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.mapper.SendinblueMapper;
import app.bpartners.api.repository.sendinblue.model.Contact;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sendinblue.ApiException;
import sibApi.ContactsApi;
import sibModel.CreateContact;
import sibModel.CreateUpdateContactModel;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Component
@Slf4j
public class SendinblueApi {
  private final SendinblueMapper mapper;
  private final ContactsApi contactsApi;

  public SendinblueApi(SendinblueMapper mapper) {
    this.mapper = mapper;
    this.contactsApi = new ContactsApi();
  }

  public Contact getContactDetails(String identifier) {
    try {
      return mapper.toDomain(contactsApi.getContactInfo(identifier));
    } catch (ApiException e) {
      if (e.getCode() == 404) {
        throw new NotFoundException("Contact." + identifier + " does not exist");
      }
      throw new app.bpartners.api.model.exception.ApiException(SERVER_EXCEPTION, e);
    }
  }

  public Contact createContact(CreateContact toCreate) {
    CreateUpdateContactModel crupdateContact;
    try {
      crupdateContact = contactsApi.createContact(toCreate);
      return getContactDetails(String.valueOf(crupdateContact.getId()));
    } catch (ApiException e) {
      throw new app.bpartners.api.model.exception.ApiException(SERVER_EXCEPTION,
          e.getResponseBody());
    }
  }

  public boolean deleteContact(String identifier) {
    try {
      contactsApi.deleteContact(identifier);
      return true;
    } catch (ApiException e) {
      return false;
    }
  }
}
