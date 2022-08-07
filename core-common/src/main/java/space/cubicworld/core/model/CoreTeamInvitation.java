package space.cubicworld.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "team_invitations")
public class CoreTeamInvitation {

    @EmbeddedId
    private CorePlayerTeamLink link;

}
