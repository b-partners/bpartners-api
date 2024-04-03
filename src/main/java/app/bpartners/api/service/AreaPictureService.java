package app.bpartners.api.service;

import static java.util.stream.Collectors.toUnmodifiableList;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AreaPictureMapper;
import app.bpartners.api.repository.jpa.AreaPictureJpaRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AreaPictureService {
  private final AreaPictureJpaRepository jpaRepository;
  private final AreaPictureMapper mapper;

  public List<AreaPicture> findAllBy(String userId, String address, String filename) {
    return jpaRepository
        .findAllByIdUserAndAddressContainingIgnoreCaseAndFilenameContainingIgnoreCase(
            userId, address, filename)
        .stream()
        .map(mapper::toDomain)
        .collect(toUnmodifiableList());
  }

  public AreaPicture findBy(String userId, String id) {
    return mapper.toDomain(
        jpaRepository
            .findByIdUserAndId(userId, id)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "HAreaPicture with UserId = "
                            + userId
                            + " and Id = "
                            + id
                            + " was not found.")));
  }
}
