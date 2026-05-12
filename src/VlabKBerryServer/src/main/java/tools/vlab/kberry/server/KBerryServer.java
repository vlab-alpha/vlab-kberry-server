package tools.vlab.kberry.server;

import io.vertx.core.Vertx;
import lombok.Getter;
import org.eclipse.paho.client.mqttv3.MqttException;
import tools.vlab.kberry.core.knx.baos.SerialBAOSConnection;
import tools.vlab.kberry.core.knx.baos.TimeoutException;
import tools.vlab.kberry.core.knx.devices.KNXDevice;
import tools.vlab.kberry.core.knx.devices.KNXDevices;
import tools.vlab.kberry.core.mqtt.MqttDevices;
import tools.vlab.kberry.core.mqtt.custom.devices.CustomMqttDevice;
import tools.vlab.kberry.core.mqtt.custom.devices.CustomMqttDevices;
import tools.vlab.kberry.core.mqtt.shelly.devices.ShellyDevice;
import tools.vlab.kberry.core.mqtt.shelly.devices.ShellyDevices;
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
import java.util.*;
import java.util.stream.Collectors;

public class KBerryServer {

    private final SerialBAOSConnection connection;
    @Getter
    private final KNXDevices knxDevices;
    @Getter
    private final MqttDevices mqttDevices;
    @Getter
    private final CommandController commandController;
    @Getter
    private final LogicEngine logicEngine;
    @Getter
    private final Statistics statistics;
    @Getter
    private final ServiceProviders serviceProviders;

    private KBerryServer(SerialBAOSConnection connection, KNXDevices knxDevices, MqttDevices mqttDevices, CommandController commandController, LogicEngine logicEngine, Statistics statistics, ServiceProviders serviceProviders) {
        this.connection = connection;
        this.knxDevices = knxDevices;
        this.mqttDevices = mqttDevices;
        this.commandController = commandController;
        this.logicEngine = logicEngine;
        this.statistics = statistics;
        this.serviceProviders = serviceProviders;
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
        private final KNXDevices knxDevices;
        private final CustomMqttDevices mqttDevices;
        private final ShellyDevices shellyDevices;
        private final Set<Command> commands = new HashSet<>();
        private final Set<Logic> logics = new HashSet<>();
        private final String mqttHost;
        private final int mqttPort;
        private final ScheduleEngine scheduler = new ScheduleEngine();
        private HashMap<String, String> webcalUrl = new HashMap<>();
        private ServiceProviders serviceProviders;

        public Builder(SerialBAOSConnection connection, KNXDevices knxDevices, CustomMqttDevices mqttDevices, ShellyDevices shellyDevices, String mqttAddress, int mqttPort) {
            this.connection = connection;
            this.mqttHost = mqttAddress;
            this.mqttPort = mqttPort;
            this.knxDevices = knxDevices;
            this.mqttDevices = mqttDevices;
            this.shellyDevices = shellyDevices;
        }

        public static Builder create(String serialInterface, String mqttHost, int mqttPort) {
            var connection = new SerialBAOSConnection(serialInterface, 1000, 10);
            var devices = new KNXDevices(connection);
            return new Builder(connection, devices, new CustomMqttDevices(mqttHost), new ShellyDevices(mqttHost), mqttHost, mqttPort);
        }

        public <T extends KNXDevice> Builder register(T device) {
            this.knxDevices.register(device);
            return this;
        }

        public <T extends CustomMqttDevice> Builder register(T device) {
            this.mqttDevices.register(device);
            return this;
        }

        public <T extends ShellyDevice> Builder register(T device) {
            this.shellyDevices.register(device);
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
            this.scheduler.registerSchedule(knxDevices, mqttDevices, shellyDevices, serviceProviders, scheduler);
            return this;
        }

        public Builder addCalendar(String calendar, String webcalUrl) {
            this.webcalUrl.put(calendar, webcalUrl);
            return this;
        }

        public KBerryServer build() throws IOException, TimeoutException, MqttException {
            return build("ETSWeinzierlBAOSImport.csv", "MqttDevices.csv", "ShellyDevices.csv");
        }

        public KBerryServer build(String csvExportFileName, String mqttDevicExportCsvFile, String shellyDevicExportCsvFile) throws IOException, TimeoutException, MqttException {
            return build(csvExportFileName, mqttDevicExportCsvFile, shellyDevicExportCsvFile, null, null, null);
        }

        public KBerryServer build(String knxCsvFile, String mqttCsvFile, String shellyCsvFile, String mailLogUserName, String mailLogPassword, String adminEmail) throws IOException, TimeoutException, MqttException {
            knxDevices.exportCSV(Path.of(knxCsvFile));
            connection.connect();
            Vertx vertx = Vertx.vertx();
            Logger.init(vertx, mailLogUserName, mailLogPassword, adminEmail);

            // MQTT Devices
            this.mqttDevices.start(Path.of(mqttCsvFile));

            // Shell Devices
            this.shellyDevices.start(Path.of(shellyCsvFile));

            // Statistics
            Statistics statistics = new Statistics("statistics");
            var statisticsScheduler = new StatisticsScheduler(statistics, knxDevices);

            Logger.info("KBerryServer Service Provider ...");

            // ServiceProvider
            var costWattVerticle = new CostWattVerticle();
            var weatherServiceProvider = new MeteoWeatherVerticle();

            Map<String, IcsCalendarServiceProvider> calendarUrls = webcalUrl.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> new IcsCalendarServiceProvider(vertx, e.getValue())));

            serviceProviders = new ServiceProviders(
                    costWattVerticle,
                    weatherServiceProvider,
                    calendarUrls
            );

            Logger.info("KBerryServer Logics Init ...");
            // Logic
            var logicEngine = new LogicEngine(vertx, knxDevices, mqttDevices, shellyDevices, serviceProviders, statistics);

            logics.forEach(logicEngine::register);

            // Commands
            Logger.info("KBerryServer MQTT Commands ...");
            var controller = new CommandController(mqttHost, mqttPort, knxDevices, mqttDevices, shellyDevices, statistics, serviceProviders, scheduler, logicEngine);
            commands.forEach(controller::register);

            Logger.info("KBerryServer Deploy Verticles ...");
            return vertx.deployVerticle(statisticsScheduler)
                    .compose(ignore -> vertx.deployVerticle(weatherServiceProvider))
                    .compose(ignore -> vertx.deployVerticle(costWattVerticle))
                    .compose(ignore -> vertx.deployVerticle(controller))
                    .compose(ignore -> vertx.deployVerticle(scheduler))
                    .map(ignore -> {
                        Logger.info("KBerryServer Build Done ...");
                        return new KBerryServer(connection, knxDevices, mqttDevices, controller, logicEngine, statistics, serviceProviders);
                    })
                    .await();
        }

    }

}
