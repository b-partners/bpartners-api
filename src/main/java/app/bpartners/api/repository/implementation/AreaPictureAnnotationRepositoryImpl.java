package app.bpartners.api.repository.implementation;

import static java.util.stream.Collectors.toUnmodifiableList;

import app.bpartners.api.model.AreaPictureAnnotation;
import app.bpartners.api.model.mapper.AreaPictureAnnotationMapper;
import app.bpartners.api.repository.AreaPictureAnnotationRepository;
import app.bpartners.api.repository.jpa.AreaPictureAnnotationJpaRepository;
import app.bpartners.api.repository.jpa.model.HAreaPictureAnnotation;
import app.bpartners.api.repository.model.AreaPictureAnnotationCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class AreaPictureAnnotationRepositoryImpl implements AreaPictureAnnotationRepository {
  private final AreaPictureAnnotationJpaRepository jpaRepository;
  private final AreaPictureAnnotationMapper mapper;
  private final EntityManager entityManager;

  @Override
  public List<AreaPictureAnnotation> findAllBy(
      String idUser, String idAreaPicture, Boolean isDraft, Pageable pageable) {
    return jpaRepository
        .findAllByIdUserAndIdAreaPictureAndIsDraft(idUser, idAreaPicture, isDraft, pageable)
        .stream()
        .map(mapper::toDomain)
        .collect(toUnmodifiableList());
  }

  @Override
  public Optional<AreaPictureAnnotation> findBy(String idUser, String idAreaPicture, String id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public AreaPictureAnnotation save(AreaPictureAnnotation areaPictureAnnotation) {
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(areaPictureAnnotation)));
  }

  @Override
  public List<AreaPictureAnnotation> findAllByIsDraftAndAccountId(
      String idUser, Boolean isDraft, Pageable pageable) {
    return jpaRepository.findAllByIdUserAndIsDraft(idUser, isDraft, pageable).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<AreaPictureAnnotation> findAllByCriteria(AreaPictureAnnotationCriteria criteria) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HAreaPictureAnnotation> query = builder.createQuery(HAreaPictureAnnotation.class);
    Root<HAreaPictureAnnotation> root = query.from(HAreaPictureAnnotation.class);

    query.where(criteriaPredicates(builder, root, criteria));
    query.orderBy(builder.desc(root.get("creationDatetime")));

    var typedQuery = entityManager.createQuery(query);
    if (criteria.page() != null && criteria.pageSize() != null) {
      typedQuery
          .setFirstResult(criteria.page() * criteria.pageSize())
          .setMaxResults(criteria.pageSize());
    }
    return typedQuery.getResultList().stream().map(mapper::toDomain).collect(toUnmodifiableList());
  }

  private Predicate criteriaPredicates(
      CriteriaBuilder builder,
      Root<HAreaPictureAnnotation> root,
      AreaPictureAnnotationCriteria criteria) {
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(builder.equal(root.get("idUser"), criteria.idUser()));
    if (criteria.idAreaPicture() != null) {
      predicates.add(builder.equal(root.get("idAreaPicture"), criteria.idAreaPicture()));
    }
    if (criteria.isDraft() != null) {
      predicates.add(builder.equal(root.get("isDraft"), criteria.isDraft()));
    }
    if (criteria.creationFrom() != null) {
      predicates.add(
          builder.greaterThanOrEqualTo(root.get("creationDatetime"), criteria.creationFrom()));
    }
    if (criteria.creationTo() != null) {
      predicates.add(
          builder.lessThanOrEqualTo(root.get("creationDatetime"), criteria.creationTo()));
    }
    return builder.and(predicates.toArray(new Predicate[0]));
  }
}
