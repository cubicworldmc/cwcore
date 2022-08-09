package space.cubicworld.core.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Set;

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

    @Column(name = "verified")
    private boolean verified;

    @Column(name = "private")
    private boolean privateTeam;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_uuid", referencedColumnName = "uuid")
    private CorePlayer owner;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "link.team")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<CoreTeamMember> members;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "link.team")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<CoreTeamInvitation> invitations;


}
