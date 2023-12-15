package app.bpartners.api.repository.jpa.model;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "\"business_activity_template\"")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HBusinessActivityTemplate implements Serializable {
  @Id @GeneratedValue private String id;
  private String name;
}
