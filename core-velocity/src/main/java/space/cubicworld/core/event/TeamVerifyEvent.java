package space.cubicworld.core.event;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.Builder;
import lombok.Data;
import space.cubicworld.core.model.CoreTeam;

@Data
@Builder
public class TeamVerifyEvent {

    private final CommandSource verifier;
    private final CoreTeam team;

}
