package app.bpartners.api.repository.jpa.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import static javax.persistence.GenerationType.IDENTITY;

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
