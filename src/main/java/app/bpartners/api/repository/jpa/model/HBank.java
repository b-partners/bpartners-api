package app.bpartners.api.repository.jpa.model;

import java.io.Serializable;
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
@Table(name = "\"bank\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class HBank implements Serializable {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  private String bridgeId;
  private String name;
  private String logoUrl;
}