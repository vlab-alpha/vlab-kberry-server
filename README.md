## Overview
V.Lab KBerry is a smart home framework designed for controlling and monitoring KNX devices in a home automation environment to use the Kberry Core (KBerry Weinzierl® Raspberry Pi shield). The project enables integration of various sensors and actors, implementation of automation logic, and provides a server component for centralized control.


## Architecture
The KBerry server consists of several main components:

### 1. KBerryServer
The KBerryServer is the central component that provides the following functions:
- Connection to KNX devices via a SerialBAOS interface (Weinzierl® Kberry Raspberry Shield)
- Management of KNX devices (sensors and actors)
- Execution of logic modules (with default logic like Auto Light Off/on)
- Command processing (Define MQTT comannds)
- MQTT integration for communication with external systems

### 2. Devices
The system supports various KNX device types:
- **Actors**: e.g., light control (Light)
- **Sensors**: e.g., presence sensors (PresenceSensor), humidity sensors (HumiditySensor)

### 3. Logic Engine
The logic engine enables the definition and execution of automation rules:
- Registration of various logic modules
- Example: AutoPlugOnLogic

### 4. Service Providers
The system integrates various services:
- Weather information (MeteoWeatherVerticle)
- Energy cost monitoring (CostWattVerticle)
- Calendar integration (Google Calendar, iCloud Calendar)

### 5. Command Controller
The command controller manages command processing and provides an MQTT interface for external control.

### 6. Scheduler
Enables scheduled execution of actions and automations.


## Server Initialization
To initialize the KBerry server, follow these steps:

### 1. Basic Configuration
```
KBerryServer server = KBerryServer.Builder.create(
    "tmmy",             // Serial interface
    "localhost",        // MQTT host
    1883,               // MQTT port
    2000,               // Timeout in ms
    10                  // Number of retry attempts
);
```


### 2. Device Registration
```
Register the required devices:
.register(Light.at(HausTester.KinderzimmerBlau))
.register(PresenceSensor.at(HausTester.KinderzimmerBlau))
.register(HumiditySensor.at(HausTester.KinderzimmerBlau, 60000))
```

### 3. Add Logic
Add the desired automation logic:
```
.logic(AutoPlugOnLogic.only(HausTester.KinderzimmerBlau))
```

### 4. Configure Optional Services
If needed, additional services like Google Calendar can be integrated:
```
.setGoogleCalendar(
    Path.of("credentials.json"),
    "user@example.com",
    "primary",
    "token-path"
)
```

Or iCloud Calendar:
```
.setICloudCalender(
    "username",
    "app-password",
    "calendar-url"
)
```

### 5. Build and Start the Server
```
KBerryServer server = builder.build();
server.startListening();
```

### 6. Error Handling
It's important to implement proper error handling and close the connection when problems occur:
```
try {
    // Server initialization and start
} catch (Exception e) {
    if (server != null) {
        server.disconnect();
    }
}
```

## Example of Complete Server Initialization
```
KBerryServer server = null;
try {
    server = KBerryServer.Builder.create("tmmy", "localhost", 1883, 2000, 10)
            .logic(AutoPlugOnLogic.only(HausTester.KinderzimmerBlau))
            .register(Light.at(HausTester.KinderzimmerBlau))
            .register(PresenceSensor.at(HausTester.KinderzimmerBlau))
            .register(HumiditySensor.at(HausTester.KinderzimmerBlau, 60000))
            .build();
    server.startListening();
} catch (Exception e) {
    if (server != null) {
        server.disconnect();
    }
}
```

## Technical Requirements
- Java SDK 25-preview
- Lombok for code simplification
- Vertx for reactive programming
- MQTT broker for communication

## Additional Information
For more detailed information about individual components and their usage, please consult the class documentation or contact the development team.
