package space.cubicworld.core.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@Table(
        name = "teams",
        uniqueConstraints =
                @UniqueConstraint(name = "name", columnNames = "name")
)
@SuperBuilder
@NoArgsConstructor
public class CoreTeam {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @OneToOne
    @JoinColumn(name = "owner_uuid", referencedColumnName = "uuid")
    private CorePlayer owner;

}
