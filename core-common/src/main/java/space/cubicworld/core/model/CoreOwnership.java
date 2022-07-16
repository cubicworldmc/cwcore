package space.cubicworld.core.model;

public enum CoreOwnership {

    OWNED,
    NOT_OWNED,
    UNDEFINED

    ;

    public static CoreOwnership fromBoolean(boolean owned) {
        return owned ? OWNED : NOT_OWNED;
    }

}
