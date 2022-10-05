package app.bpartners.api.repository.sendinblue;

import app.bpartners.api.repository.sendinblue.model.Contact;
import java.util.List;

public interface ContactRepository {
  List<Contact> save(List<Contact> toCreate);
}
