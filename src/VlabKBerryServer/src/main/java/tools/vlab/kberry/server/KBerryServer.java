package tools.vlab.kberry.server;

import io.vertx.core.Vertx;
import lombok.Getter;
import tools.vlab.kberry.core.baos.SerialBAOSConnection;
import tools.vlab.kberry.core.baos.TimeoutException;
import tools.vlab.kberry.core.devices.KNXDevice;
import tools.vlab.kberry.core.devices.KNXDevices;
import tools.vlab.kberry.server.commands.Command;
import tools.vlab.kberry.server.commands.CommandController;
import tools.vlab.kberry.server.logic.Logic;
import tools.vlab.kberry.server.logic.Logics;
import tools.vlab.kberry.server.scheduler.ScheduleEngine;
import tools.vlab.kberry.server.scheduler.trigger.Trigger;
import tools.vlab.kberry.server.serviceProvider.*;
import tools.vlab.kberry.server.statistics.Statistics;
import tools.vlab.kberry.server.statistics.StatisticsScheduler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class KBerryServer {

    private final SerialBAOSConnection connection;
    @Getter
    private final KNXDevices devices;
    @Getter
    private final CommandController commandController;
    @Getter
    private final Logics logicEngine;

    private KBerryServer(SerialBAOSConnection connection, KNXDevices devices, CommandController commandController, Logics logicEngine) {
        this.connection = connection;
        this.devices = devices;
        this.commandController = commandController;
        this.logicEngine = logicEngine;
    }

    public void startListening() {
        System.out.println("KBerryServer is now listening... Press Ctrl+C to stop.");
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        logicEngine.stop();
        connection.disconnect();
    }

    public static class Builder {

        private final SerialBAOSConnection connection;
        private final KNXDevices devices;
        private final Set<Command> commands = new HashSet<>();
        private final Set<Logic> logics = new HashSet<>();
        private final String mqttHost;
        private final int mqttPort;
        private final ScheduleEngine scheduler = new ScheduleEngine(new File("schedule"));
        private GoogleCalendarService googleCalendarServiceProvider;
        private IcloudCalendarService icloudCalenderService;

        public Builder(SerialBAOSConnection connection, String mqttAddress, int mqttPort) {
            this.connection = connection;
            this.devices = new KNXDevices(connection);
            this.mqttHost = mqttAddress;
            this.mqttPort = mqttPort;
        }

        public static Builder create(String serialInterface, String mqttHost, int mqttPort, int timeoutMs, int retries) {
            return new Builder(new SerialBAOSConnection(serialInterface, timeoutMs, retries), mqttHost, mqttPort);
        }

        public <T extends KNXDevice> Builder register(T device) {
            this.devices.register(device);
            return this;
        }

        public Builder command(Command command) {
            commands.add(command);
            return this;
        }

        public Builder logic(Logic logic) {
            this.logics.add(logic);
            return this;
        }

        public Builder scheduler(String id, Trigger trigger, Runnable runnable) {
            this.scheduler.start(id, trigger, runnable);
            return this;
        }

        public Builder setGoogleCalendar(Path credPath, String userId, String calendarId, String tokenPath) throws IOException {
            this.icloudCalenderService = null;
            this.googleCalendarServiceProvider = GoogleCalendarService.fromCredentialsFile(credPath, userId, calendarId, tokenPath);
            return this;
        }

        public Builder setICloudCalender(String username, String appPassword, String calendarUrl) {
            this.googleCalendarServiceProvider = null;
            this.icloudCalenderService = IcloudCalendarService.ical(username, appPassword, calendarUrl);
            return this;
        }

        public KBerryServer build() throws IOException, TimeoutException {
            return build("ETSWeinzierlBAOSImport.csv");
        }

        public KBerryServer build(String csvExportFileName) throws IOException, TimeoutException {
            Vertx vertx = Vertx.vertx();
            devices.exportCSV(Path.of(csvExportFileName));
            connection.connect();

            // Statistics
            Statistics statistics = new Statistics();
            var statisticsScheduler = new StatisticsScheduler(statistics, devices);



            // ServiceProvider
            var costWattVerticle = new CostWattVerticle();
            var weatherServiceProvider = new MeteoWeatherVerticle();

            var serviceProvider = new ServiceProviders(
                    costWattVerticle,
                    weatherServiceProvider,
                    googleCalendarServiceProvider != null ? googleCalendarServiceProvider : icloudCalenderService
            );

            // Logic
            var logicEngine = new Logics(devices, serviceProvider);
            logics.forEach(logicEngine::register);

            // Commands
            var controller = new CommandController(mqttHost, mqttPort, devices, statistics, serviceProvider, scheduler, logicEngine);
            commands.forEach(controller::register);

            return vertx.deployVerticle(statisticsScheduler)
                    .compose(ignore -> vertx.deployVerticle(weatherServiceProvider))
                    .compose(ignore -> vertx.deployVerticle(costWattVerticle))
                    .compose(ignore -> vertx.deployVerticle(controller))
                    .map(ignore -> new KBerryServer(connection, devices, controller, logicEngine))
                    .await();
        }

    }

}
