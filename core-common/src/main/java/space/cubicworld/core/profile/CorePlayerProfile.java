package space.cubicworld.core.profile;

import lombok.Builder;
import lombok.Data;
import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.model.CoreTeamMember;

import java.util.List;

@Data
@Builder
public class CorePlayerProfile {

    private final CorePlayer player;
    private final List<CoreTeamMember> memberships;

}
