package tools.vlab.kberry.server.logic;

import lombok.Getter;

public enum TargetLux {
    NIGHT(30),
    FLOOR(100),
    LIVING_ROOM(250),
    OFFICE(500),
    WORKING_PLACE(750),
    DAYLIGHT_BOOST(900);

    @Getter
    private final float targetLux;

    TargetLux(int targetLux) {
        this.targetLux = targetLux;
    }
}
