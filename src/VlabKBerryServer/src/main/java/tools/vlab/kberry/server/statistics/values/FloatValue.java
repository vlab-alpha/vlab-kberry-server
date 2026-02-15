package tools.vlab.kberry.server.statistics.values;

public record FloatValue(float value) implements PrimitiveValue {

    public static FloatValue of(float currentTemp) {
        return new FloatValue(currentTemp);
    }

    @Override public String serialize() {
        return Float.toString(value);
    }
}
