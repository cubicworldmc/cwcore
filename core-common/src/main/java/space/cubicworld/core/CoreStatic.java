package space.cubicworld.core;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CoreStatic {

    public final long TOP_SIZE = 10;
    public final long VERIFIES_PAGE_SIZE = 5;
    public final long INVITES_PAGE_SIZE = 5;
    public final long BOOSTS_PAGE_SIZE = 5;
    public final String PLAYER_UPDATE_CHANNEL = "cwcore:player_update";

    public final long BOOST_EXTEND_UNIX = 1000L * 60 * 60 * 24 * 30;

}
