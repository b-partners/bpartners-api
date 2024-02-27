package app.bpartners.api.repository.jpa.model;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
