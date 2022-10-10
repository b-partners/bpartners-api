package app.bpartners.api.repository.sendinblue.implementation;

import app.bpartners.api.repository.mapper.SendinblueMapper;
import app.bpartners.api.repository.sendinblue.ContactRepository;
import app.bpartners.api.repository.sendinblue.SendinblueApi;
import app.bpartners.api.repository.sendinblue.model.Contact;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class ContactRepositoryImpl implements ContactRepository {
  private final SendinblueApi sendinblueApi;
  private final SendinblueMapper mapper;

  @Override
  public List<Contact> save(List<Contact> contacts) {
    return contacts.stream()
        .map(contact -> sendinblueApi
            .createContact(mapper.toExternalCreateContact(contact)))
        .collect(Collectors.toUnmodifiableList());
  }
}
