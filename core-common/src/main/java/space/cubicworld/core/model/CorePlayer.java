package space.cubicworld.core.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import net.kyori.adventure.text.format.TextColor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Table(
        name = "players",
        uniqueConstraints =
                @UniqueConstraint(columnNames = "name")
)
@Cacheable
@SuperBuilder
@NoArgsConstructor
public class CorePlayer {

    @Id
    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "reputation")
    private int reputation;

    @Column(name = "global_color")
    private int globalColor;

    @ManyToOne
    @JoinColumn(name = "selected_team_id", referencedColumnName = "id")
    private CoreTeam selectedTeam;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "link.player")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<CoreTeamMember> teams;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "link.player")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<CoreTeamInvitation> invitations;

    public void setGlobalColor(TextColor color) {
        this.globalColor = color == null ? -1 : color.value();
    }

    public TextColor getGlobalColor() {
        return globalColor == -1 ? null : TextColor.color(globalColor);
    }

}
