package tools.vlab.kberry.server;

import tools.vlab.kberry.core.HausTester;
import tools.vlab.kberry.core.devices.actor.Light;
import tools.vlab.kberry.core.devices.sensor.HumiditySensor;
import tools.vlab.kberry.core.devices.sensor.PresenceSensor;
import tools.vlab.kberry.server.logic.AutoPlugOnLogic;

public class Main {

    public static void main(String[] args) {
        KBerryServer server = null;
        try {
            server = KBerryServer.Builder.create("tmmy", "localhost", 1883, 2000, 10)
                    .logic(AutoPlugOnLogic.at(HausTester.KinderzimmerBlau))
                    .register(Light.at(HausTester.KinderzimmerBlau))
                    .register(PresenceSensor.at(HausTester.KinderzimmerBlau))
                    .register(HumiditySensor.at(HausTester.KinderzimmerBlau, 60000))
                    .build();
            server.startListening();
        } catch (Exception e) {
            if (server != null) {
                server.shutdown();
            }
        }



    }
}
