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
@Table(name = "\"marketplace\"")
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HMarketplace {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String name;
  private String description;
  private String phoneNumber;
  private String websiteUrl;
  private String logoUrl;
}
