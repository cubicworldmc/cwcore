package space.cubicworld.core.event;

import com.velocitypowered.api.command.CommandSource;
import lombok.Data;
import space.cubicworld.core.database.CoreTeam;

@Data
public class TeamVerifyEvent {

    private final CommandSource verifier;
    private final CoreTeam team;

}
