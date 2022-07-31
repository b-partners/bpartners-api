package app.bpartners.api.service;

import app.bpartners.api.model.Group;
import app.bpartners.api.repository.GroupRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GroupService {

  private final GroupRepository repository;

  public Group getById(String groupId) {
    return repository.getById(groupId);
  }

  public List<Group> getAll() {
    return repository.findAll();
  }

  public List<Group> saveAll(List<Group> groups) {
    return repository.saveAll(groups);
  }
}
