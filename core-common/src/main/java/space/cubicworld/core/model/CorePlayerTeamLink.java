package space.cubicworld.core.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class CorePlayerTeamLink implements Serializable {

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_uuid", referencedColumnName = "uuid")
    @ToString.Exclude
    private CorePlayer player;

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id", referencedColumnName = "id")
    @ToString.Exclude
    private CoreTeam team;

}
