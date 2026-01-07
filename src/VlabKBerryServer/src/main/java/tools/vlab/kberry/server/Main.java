package tools.vlab.kberry.server;

import tools.vlab.kberry.core.Haus;
import tools.vlab.kberry.core.baos.TimeoutException;
import tools.vlab.kberry.core.devices.PushButton;
import tools.vlab.kberry.core.devices.actor.Light;

public class Main {

    public static void main(String[] args) throws TimeoutException {

        KBerryServer server = null;
        try {
            server = KBerryServer.Builder.create("/dev/ttyAMA0", "localhost", 1883)
                    .register(PushButton.at(Haus.KidsRoomYellowWall))
                    .register(PushButton.at(Haus.KidsRoomBlueWall))
                    .register(Light.at(Haus.BathTop))
                    .register(Light.at(Haus.BathWall))
                    .register(Light.at(Haus.KidsRoomBlueTop))
                    .register(Light.at(Haus.KidsRoomYellowTop))
                    .register(Light.at(Haus.OfficeTop))
                    .register(Light.at(Haus.SleepingRoomTop))
                    .register(Light.at(Haus.UpperHallwayTop))
                    .register(Light.at(Haus.HallwayTop))
                    .register(Light.at(Haus.GuestWC_Top))
                    .register(Light.at(Haus.ChangingRoomTop))
                    .register(Light.at(Haus.DiningRoomTop))
                    .register(Light.at(Haus.LivingRoomTop))
                    .register(Light.at(Haus.KitchenTop))

//                    .logic(AutoPlugOnLogic.at(HausTester.KinderzimmerBlau))
//                    .register(Light.at(HausTester.KinderzimmerBlau))
//                    .register(PresenceSensor.at(HausTester.KinderzimmerBlau))
//                    .register(HumiditySensor.at(HausTester.KinderzimmerBlau, 60000))
                    .build();
            server.startListening();
        } catch (Exception e) {
            e.printStackTrace();
            if (server != null) {
                server.shutdown();
            }
        }


    }
}
