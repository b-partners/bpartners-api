package app.bpartners.api.repository.jpa.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "\"sogefi_building_permit_prospect\"")
@Setter
@ToString
@AllArgsConstructor
@Getter
@Builder
@NoArgsConstructor
@EqualsAndHashCode
public class HSogefiBuildingPermitProspect {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private long idSogefi;
  private String idProspect;
  private String geojsonType;
  private double geojsonLongitude;
  private double geojsonLatitude;
}
