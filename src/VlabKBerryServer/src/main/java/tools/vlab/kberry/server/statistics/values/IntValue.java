package tools.vlab.kberry.server.statistics.values;

public record IntValue(int value) implements PrimitiveValue {
    @Override public String serialize() {
        return Integer.toString(value);
    }
}
