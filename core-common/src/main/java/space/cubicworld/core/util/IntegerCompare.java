package space.cubicworld.core.util;

import java.util.Objects;
import java.util.function.BiPredicate;

public enum IntegerCompare implements BiPredicate<Integer, Integer> {

    GREATER,
    LESS,
    EQUALS;

    public static IntegerCompare fromOperator(String operator) {
        return switch (operator) {
            case ">" -> GREATER;
            case "<" -> LESS;
            case "=", "==" -> EQUALS;
            default -> throw new IllegalArgumentException(operator + " is not an operator");
        };
    }

    @Override
    public boolean test(Integer integer, Integer integer2) {
        return switch (this) {
            case GREATER -> integer > integer2;
            case LESS -> integer < integer2;
            case EQUALS -> Objects.equals(integer, integer2);
        };
    }

    @Override
    public String toString() {
        return switch (this) {
            case GREATER -> ">";
            case LESS -> "<";
            case EQUALS -> "=";
        };
    }
}
