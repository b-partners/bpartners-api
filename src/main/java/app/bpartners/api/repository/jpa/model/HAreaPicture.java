package app.bpartners.api.repository.jpa.model;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.api.endpoint.rest.model.GeoPosition;
import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
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
  private double score;
  private String idProspect;
  @CreationTimestamp private Instant createdAt;
  @UpdateTimestamp private Instant updatedAt;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private ZoomLevel zoomLevel;

  private String idLayer;

  private String filename;
  private boolean isExtended;

  @JdbcTypeCode(JSON)
  @Column(name = "geopositions")
  private List<GeoPosition> geoPositions;

  private Integer shiftNb;

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

  public GeoPosition getCurrentGeoPosition() {
    return new GeoPosition().score(score).longitude(longitude).latitude(latitude);
  }
}
