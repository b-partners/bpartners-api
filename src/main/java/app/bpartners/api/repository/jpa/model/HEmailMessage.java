package app.bpartners.api.repository.jpa.model;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "\"email_message\"")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HEmailMessage implements Serializable {
  @Id
  private String id;
  private String message;
  private String idAccount;
}
