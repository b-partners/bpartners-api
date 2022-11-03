package app.bpartners.api.repository.jpa.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"business_activity\"")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HBusinessActivity {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  @ManyToOne
  @JoinColumn(name = "primary_activity", referencedColumnName = "name")
  private HBusinessActivityTemplate primaryActivity;
  @ManyToOne
  @JoinColumn(name = "secondary_activity", referencedColumnName = "name")
  private HBusinessActivityTemplate secondaryActivity;
  private String otherPrimaryActivity;
  private String otherSecondaryActivity;
  @ManyToOne
  @JoinColumn(name = "account_holder_id")
  private HAccountHolder accountHolder;
}
