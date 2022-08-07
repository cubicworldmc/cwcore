package space.cubicworld.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "team_members")
public class CoreTeamMember implements Serializable {

    @EmbeddedId
    private CorePlayerTeamLink link;

}
