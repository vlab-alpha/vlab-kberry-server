package tools.vlab.kberry.server.serviceProvider;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.vlab.kberry.core.Haus;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.knx.devices.actor.Jalousie;

class IcsCalendarServiceProviderTest {

    private Vertx vertx;


    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() {
        vertx.close();
    }

    @Test
    void testGetTodayWithRealIcloudUrl() {
        Vertx vertx = Vertx.vertx();
        String url = "webcal://p106-caldav.icloud.com/published/2/MTAyMjIyMTE2OTEwMjIyMvNYKt_ybZD4V6_Bz7wBQxRslKzaDwD_NSh3BuJw1k7J5v98xVB2R_IXdbkSmaEfvG3Ejz2v7hofWHFSAfHtQ0I";

        var service = new IcsCalendarServiceProvider(vertx, url);

        // Nutze eine ID und Klasse, von der du weißt, dass sie heute im Kalender steht
        // Falls du "Shutter:BathWall" im Kalender hast:
        PositionPath path = Haus.BathWall;

        var result = service.getToday(path, Jalousie.class) // TestDevice wie im vorigen Beispiel
                .toCompletionStage()
                .toCompletableFuture()
                .join();

        // Wenn heute kein Event drin ist, wird result null sein.
        // Zum reinen Verbindungstest reicht es zu prüfen, ob keine Exception fliegt.
        System.out.println("Result: " + result);
        vertx.close();
    }
}