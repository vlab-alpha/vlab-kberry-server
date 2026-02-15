package tools.vlab.kberry.server.statistics.values;

public record BooleanValue(boolean value) implements PrimitiveValue {
    public static BooleanValue of(boolean present) {
        return new BooleanValue(present);
    }

    @Override public String serialize() {
        return Boolean.toString(value);
    }
}
