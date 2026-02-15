package tools.vlab.kberry.server;

import io.vertx.core.Vertx;
import lombok.Getter;
import tools.vlab.kberry.core.baos.SerialBAOSConnection;
import tools.vlab.kberry.core.baos.TimeoutException;
import tools.vlab.kberry.core.devices.KNXDevice;
import tools.vlab.kberry.core.devices.KNXDevices;
import tools.vlab.kberry.server.commands.Command;
import tools.vlab.kberry.server.commands.CommandController;
import tools.vlab.kberry.server.commands.Scene;
import tools.vlab.kberry.server.log.Logger;
import tools.vlab.kberry.server.logic.Logic;
import tools.vlab.kberry.server.logic.LogicEngine;
import tools.vlab.kberry.server.scheduler.ScheduleEngine;
import tools.vlab.kberry.server.scheduler.Scheduler;
import tools.vlab.kberry.server.serviceProvider.*;
import tools.vlab.kberry.server.statistics.Statistics;
import tools.vlab.kberry.server.statistics.StatisticsScheduler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class KBerryServer {

    private final SerialBAOSConnection connection;
    @Getter
    private final KNXDevices devices;
    @Getter
    private final CommandController commandController;
    @Getter
    private final LogicEngine logicEngine;
    @Getter
    private final Statistics statistics;

    private KBerryServer(SerialBAOSConnection connection, KNXDevices devices, CommandController commandController, LogicEngine logicEngine, Statistics statistics) {
        this.connection = connection;
        this.devices = devices;
        this.commandController = commandController;
        this.logicEngine = logicEngine;
        this.statistics = statistics;
    }

    public void startListening() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        System.out.println("KBerryServer is now listening... Press Ctrl+C to stop.");
    }

    public void shutdown() {
        logicEngine.stop();
        connection.disconnect();
    }

    public List<Scene> getScenes() {
        return this.commandController.getCommandList().stream()
                .filter(command -> command instanceof Scene)
                .map(command -> ((Scene) command))
                .collect(Collectors.toList());
    }

    public static class Builder {

        private final SerialBAOSConnection connection;
        private final KNXDevices devices;
        private final Set<Command> commands = new HashSet<>();
        private final Set<Logic> logics = new HashSet<>();
        private final String mqttHost;
        private final int mqttPort;
        private final ScheduleEngine scheduler = new ScheduleEngine();
        private GoogleCalendarService googleCalendarServiceProvider;
        private IcloudCalendarService icloudCalenderService;

        public Builder(SerialBAOSConnection connection, KNXDevices devices, String mqttAddress, int mqttPort) {
            this.connection = connection;
            this.mqttHost = mqttAddress;
            this.mqttPort = mqttPort;
            this.devices = devices;
        }

        public static Builder create(String serialInterface, String mqttHost, int mqttPort) {
            var connection = new SerialBAOSConnection(serialInterface, 1000, 10);
            var devices = new KNXDevices(connection);
            return new Builder(connection, devices, mqttHost, mqttPort);
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

        public Builder scheduler(Scheduler scheduler) {
            this.scheduler.registerSchedule(devices, scheduler);
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
            return build(csvExportFileName, null, null, null);
        }

        public KBerryServer build(String mailLogUserName, String mailLogPassword, String adminEmail) throws IOException, TimeoutException {
            return build("ETSWeinzierlBAOSImport.csv", mailLogUserName, mailLogPassword, adminEmail);
        }

        public KBerryServer build(String csvExportFileName, String mailLogUserName, String mailLogPassword, String adminEmail) throws IOException, TimeoutException {
            devices.exportCSV(Path.of(csvExportFileName));
            connection.connect();
            Vertx vertx = Vertx.vertx();
            Logger.init(vertx, mailLogUserName, mailLogPassword, adminEmail);

            // Statistics
            Statistics statistics = new Statistics("statistics");
            var statisticsScheduler = new StatisticsScheduler(statistics, devices);

            Logger.info("KBerryServer Service Provider ...");

            // ServiceProvider
            var costWattVerticle = new CostWattVerticle();
            var weatherServiceProvider = new MeteoWeatherVerticle();

            var serviceProvider = new ServiceProviders(
                    costWattVerticle,
                    weatherServiceProvider,
                    googleCalendarServiceProvider != null ? googleCalendarServiceProvider : icloudCalenderService
            );

            Logger.info("KBerryServer Logics Init ...");
            // Logic
            var logicEngine = new LogicEngine(vertx, devices, serviceProvider, statistics);

            logics.forEach(logicEngine::register);

            // Commands
            Logger.info("KBerryServer MQTT Commands ...");
            var controller = new CommandController(mqttHost, mqttPort, devices, statistics, serviceProvider, scheduler, logicEngine);
            commands.forEach(controller::register);

            Logger.info("KBerryServer Deploy Verticles ...");
            return vertx.deployVerticle(statisticsScheduler)
                    .compose(ignore -> vertx.deployVerticle(weatherServiceProvider))
                    .compose(ignore -> vertx.deployVerticle(costWattVerticle))
                    .compose(ignore -> vertx.deployVerticle(controller))
                    .compose(ignore -> vertx.deployVerticle(scheduler))
                    .map(ignore -> {
                        Logger.info("KBerryServer Build Done ...");
                        return new KBerryServer(connection, devices, controller, logicEngine, statistics);
                    })
                    .await();
        }

    }

}
