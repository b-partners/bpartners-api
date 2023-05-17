package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Feedback;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.FeedbackMapper;
import app.bpartners.api.repository.FeedBackRepository;
import app.bpartners.api.repository.jpa.FeedbackJpaRepository;
import app.bpartners.api.repository.jpa.model.HFeedback;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class FeedbackRepositoryImpl implements FeedBackRepository {
  private final FeedbackJpaRepository jpaRepository;
  private final FeedbackMapper mapper;

  @Override
  public Feedback findById(String id) {
    return mapper.toDomain(jpaRepository.findById(id)
        .orElseThrow(() ->
            new NotFoundException("Feedback" + id + " is not found.")));
  }

  @Override
  public List<Feedback> saveAll(String accountHolderId, List<Feedback> toCreate) {
    List<HFeedback> entityToCreate = toCreate.stream()
        .map(mapper::toEntity)
        .collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entityToCreate).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

}
