package tools.vlab.kberry.server.logic;

import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.actor.Dimmer;
import tools.vlab.kberry.core.devices.actor.OnOffDevice;
import tools.vlab.kberry.core.devices.actor.OnOffStatus;
import tools.vlab.kberry.core.devices.sensor.LuxSensor;
import tools.vlab.kberry.core.devices.sensor.LuxStatus;

public class DimmerByLuxLogic extends Logic implements OnOffStatus, LuxStatus {

    public final static String LOGIC_NAME = "AutoUsageOff";

    private final float targetLux;
    private final double kp;
    private final int minDimPercent;
    private final int maxStepPercent;
    private final int deadbandPercent;

    private DimmerByLuxLogic(
            PositionPath path,
            TargetLux targetLux,
            double kp,
            int minDimPercent,
            int maxStepPercent,
            int deadbandPercent
    ) {
        super(LOGIC_NAME, path);
        this.targetLux = targetLux.getTargetLux();
        this.kp = kp;
        this.minDimPercent = minDimPercent;
        this.maxStepPercent = maxStepPercent;
        this.deadbandPercent = deadbandPercent;
    }

    /**
     * Standard-Factory:
     * - targetLux: gewünschte Raumhelligkeit
     * - kp: moderater P-Faktor
     * - minDim: Licht geht nie ganz aus
     */
    public static DimmerByLuxLogic at(
            TargetLux targetLux,
            PositionPath dimmerPath
    ) {
        return new DimmerByLuxLogic(
                dimmerPath,
                targetLux,
                0.12,   // stabiler Startwert
                5,      // min 5 %
                8,      // max. 8 % Stellschritt
                2       // 2 % Deadband
        );
    }

    @Override
    public void start() {
        // nichts nötig
    }

    @Override
    public void stop() {
        // nichts nötig
    }

    /**
     * Wird aufgerufen wenn Licht ein/aus geschaltet wird
     */
    @Override
    public void onOffStatusChanged(OnOffDevice device, boolean isOn) {
        if (!isOn || isNotSameRoom(device)) {
            return;
        }
        regulate();
    }

    /**
     * Wird aufgerufen wenn sich der Lux-Wert ändert
     */
    @Override
    public void luxChanged(LuxSensor sensor, float lux) {
        if (isNotSameRoom(sensor)) {
            return;
        }
        regulate();
    }

    /**
     * Zentrale Regel-Funktion
     */
    private void regulate() {

        var dimmerOpt = getKnxDevices().getKNXDevice(Dimmer.class, this.getPositionPath());
        var luxOpt = getKnxDevices().getKNXDeviceByRoom(LuxSensor.class, this.getPositionPath());

        if (dimmerOpt.isEmpty() || luxOpt.isEmpty()) {
            return;
        }

        Dimmer dimmer = dimmerOpt.get();
        LuxSensor luxSensor = luxOpt.get();

        float measuredLux = luxSensor.getCurrentLux();
        int currentDim = dimmer.getCurrentBrightness();

        // Soft-Start: beim Einschalten nicht gleich hochspringen
        if (currentDim == 0) {
            dimmer.setBrightness(minDimPercent);
            return;
        }

        float error = targetLux - measuredLux;

        int delta = (int) Math.round(kp * error);
        delta = clamp(delta, -maxStepPercent, maxStepPercent);

        int newDim = clamp(currentDim + delta, minDimPercent, 100);

        // Deadband gegen Flackern
        if (Math.abs(newDim - currentDim) >= deadbandPercent) {
            dimmer.setBrightness(newDim);
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}