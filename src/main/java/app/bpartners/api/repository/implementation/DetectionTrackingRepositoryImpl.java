package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.detection.DetectionTracking;
import app.bpartners.api.model.mapper.detection.DetectionTrackingMapper;
import app.bpartners.api.repository.DetectionTrackingRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.DetectionTrackingJpaRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DetectionTrackingRepositoryImpl implements DetectionTrackingRepository {
  private final DetectionTrackingJpaRepository jpaRepository;
  private final UserRepository userRepository;
  private final DetectionTrackingMapper mapper;

  @Override
  public List<DetectionTracking> saveAll(List<DetectionTracking> detectionTracking) {
    var entities = detectionTracking.stream().map(mapper::toEntity).toList();
    return jpaRepository.saveAll(entities).stream()
        .map(saved -> mapper.toDomain(userRepository.getById(saved.getIdUser()), saved))
        .toList();
  }

  @Override
  public List<DetectionTracking> findAllByIdUserBetween(String idUser, Instant from, Instant to) {
    return jpaRepository.findAllByIdUserAndCreationDatetimeBetween(idUser, from, to).stream()
        .map(entity -> mapper.toDomain(userRepository.getById(entity.getIdUser()), entity))
        .toList();
  }
}
