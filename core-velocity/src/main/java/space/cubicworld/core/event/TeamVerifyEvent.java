package space.cubicworld.core.event;

import com.velocitypowered.api.command.CommandSource;
import lombok.Builder;
import lombok.Data;
import space.cubicworld.core.database.CoreTeam;

@Data
@Builder
public class TeamVerifyEvent {

    private final CommandSource verifier;
    private final CoreTeam team;

}
