package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.EmailStatus;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "\"email\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class HEmail implements Serializable {
  @Id private String id;
  private String idUser;
  private String recipients;
  private String object;
  private String body;
  private Instant updatedAt;
  private Instant sendingDatetime;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "id_email")
  List<HAttachment> attachments;

  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private EmailStatus status;
}
