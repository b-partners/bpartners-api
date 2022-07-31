package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.model.Group;
import org.springframework.stereotype.Component;

@Component
public class GroupMapper {

  public app.bpartners.api.endpoint.rest.model.Group toRest(Group group) {
    var restGroup = new app.bpartners.api.endpoint.rest.model.Group();
    restGroup.setId(group.getId());
    restGroup.setName(group.getName());
    restGroup.setRef(group.getRef());
    restGroup.setCreationDatetime(group.getCreationDatetime());
    return restGroup;
  }

  public Group toDomain(app.bpartners.api.endpoint.rest.model.Group restGroup) {
    return Group.builder()
        .id(restGroup.getId())
        .name(restGroup.getName())
        .ref(restGroup.getRef())
        .creationDatetime(restGroup.getCreationDatetime())
        .build();
  }
}
