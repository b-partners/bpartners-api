package app.bpartners.api.repository.jpa.model;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer;
import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import app.bpartners.api.repository.jpa.model.converter.OpenStreetMapLayerConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;

@Entity
@Setter
@Table(name = "\"area_picture\"")
@Getter
@ToString
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class HAreaPicture {
  @Id private String id;
  private String idUser;
  private String idFileInfo;
  private String address;
  private double longitude;
  private double latitude;
  private String idProspect;
  @CreationTimestamp private Instant createdAt;
  @UpdateTimestamp private Instant updatedAt;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private ZoomLevel zoomLevel;

  @Convert(converter = OpenStreetMapLayerConverter.class)
  private OpenStreetMapLayer layer;

  private String filename;

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    Class<?> oEffectiveClass =
        o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
    Class<?> thisEffectiveClass =
        this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
            : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) return false;
    HAreaPicture that = (HAreaPicture) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}