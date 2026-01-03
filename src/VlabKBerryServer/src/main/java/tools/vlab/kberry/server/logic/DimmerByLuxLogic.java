package tools.vlab.kberry.server.logic;

import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.actor.Dimmer;
import tools.vlab.kberry.core.devices.actor.OnOffDevice;
import tools.vlab.kberry.core.devices.actor.OnOffStatus;
import tools.vlab.kberry.core.devices.sensor.LuxSensor;
import tools.vlab.kberry.core.devices.sensor.LuxStatus;

import java.util.List;
import java.util.Vector;

public class DimmerByLuxLogic extends Logic
        implements OnOffStatus, LuxStatus {

    private final float targetLux;
    private final double kp;
    private final int minDimPercent;
    private final int maxStepPercent;
    private final int deadbandPercent;

    private DimmerByLuxLogic(
            Vector<PositionPath> paths,
            TargetLux targetLux,
            double kp,
            int minDimPercent,
            int maxStepPercent,
            int deadbandPercent
    ) {
        super(paths);
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
            PositionPath... paths
    ) {
        return new DimmerByLuxLogic(
                new Vector<>(List.of(paths)),
                targetLux,
                0.12,   // stabiler Startwert
                5,      // min 5 %
                8,      // max 8 % Stellschritt
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
        if (!isOn || !contains(device.getPositionPath())) {
            return;
        }
        regulate(device.getPositionPath());
    }

    /**
     * Wird aufgerufen wenn sich der Lux-Wert ändert
     */
    @Override
    public void luxChanged(LuxSensor sensor, float lux) {
        if (!contains(sensor.getPositionPath())) {
            return;
        }
        regulate(sensor.getPositionPath());
    }

    /**
     * Zentrale Regel-Funktion
     */
    private void regulate(PositionPath path) {

        var dimmerOpt = getKnxDevices().getKNXDevice(Dimmer.class, path);
        var luxOpt = getKnxDevices().getKNXDeviceByRoom(LuxSensor.class, path);

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