package tools.vlab.kberry.server.statistics.values;

public sealed interface PrimitiveValue
        permits IntValue, FloatValue, BooleanValue {

    String serialize();
}
