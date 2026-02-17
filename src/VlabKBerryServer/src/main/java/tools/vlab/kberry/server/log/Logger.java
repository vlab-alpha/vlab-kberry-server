package tools.vlab.kberry.server.log;

import io.vertx.core.Vertx;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.helpers.MessageFormatter;
import tools.vlab.kberry.core.PositionPath;
import tools.vlab.kberry.core.devices.KNXDevice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Logger {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Logger.class);
    private static final ConcurrentHashMap<String, BoundedStack<LogEntry>> LOG_BUFF = new ConcurrentHashMap<>();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static Vertx vertx;
    private static String adminEmail;
    private static Mailer mailer;

    public static void init(Vertx vertx, String mailUserName, String mailPassword, String adminEmail) {
        Logger.vertx = vertx;
        Logger.adminEmail = adminEmail;
        if (mailUserName != null && mailPassword != null && adminEmail != null) {
            Logger.mailer = MailerBuilder
                    .withSMTPServer("smtp.strato.de", 587, mailUserName, mailPassword)
                    .withTransportStrategy(TransportStrategy.SMTP_TLS)
                    .withSessionTimeout(10 * 1000)
                    .clearEmailValidator()
                    .withDebugLogging(true)
                    .async()
                    .buildMailer();
        }
    }

    private static void bufferLog(PositionPath positionPath, String level, String message, Throwable throwable) {
        var room = positionPath != null ? positionPath.getRoom() : "system";
        LOG_BUFF.computeIfAbsent(room, k -> new BoundedStack<>(500));

        LogEntry entry = new LogEntry(
                LocalDateTime.now(),
                level,
                positionPath,
                message,
                throwable
        );

        LOG_BUFF.get(room).push(entry);
    }

    private static String formatMessage(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }

        if (message.contains("{}")) {
            return MessageFormatter.arrayFormat(message, args).getMessage();
        } else {
            return String.format(message, args);
        }
    }

    private static void setMDC(PositionPath positionPath) {
        if (positionPath != null) {
            MDC.put("location", positionPath.getLocation().trim());
            MDC.put("floor", positionPath.getFloor().trim());
            MDC.put("room", positionPath.getRoom().trim());
            MDC.put("position", positionPath.getPosition());
        } else {
            MDC.put("location", "System");
            MDC.put("floor", "System");
            MDC.put("room", "System");
            MDC.put("position", "System");
        }
    }

    private static void clearMDC() {
        MDC.clear();
    }

    public static void debug(String message, Object... args) {
        debug((PositionPath) null, message, args);
    }

    public static void debug(KNXDevice device, String message, Object... args) {
        debug(device.getPositionPath(), String.format("[%s] %s", device.getClass().getSimpleName(), message), args);
    }

    public static void debug(PositionPath positionPath, String message, Object... args) {
        String formattedMessage = formatMessage(message, args);
        setMDC(positionPath);
        LOGGER.debug(formattedMessage);
        clearMDC();
        bufferLog(positionPath, "DEBUG", formattedMessage, null);
    }

    public static void debug(PositionPath positionPath, Throwable throwable, String message, Object... args) {
        String formattedMessage = formatMessage(message, args);
        setMDC(positionPath);
        LOGGER.debug(formattedMessage, throwable);
        clearMDC();
        bufferLog(positionPath, "DEBUG", formattedMessage, throwable);
    }

    public static void info(KNXDevice device, String message, Object... args) {
        info(device.getPositionPath(), String.format("[%s] %s", device.getClass().getSimpleName(), message), args);
    }

    public static void info(String message, Object... args) {
        info((PositionPath) null, message, args);
    }

    public static void info(PositionPath positionPath, String message, Object... args) {
        String formattedMessage = formatMessage(message, args);
        setMDC(positionPath);
        LOGGER.info(formattedMessage);
        clearMDC();
        bufferLog(positionPath, "INFO", formattedMessage, null);
    }

    public static void warn(PositionPath positionPath, String message, Object... args) {
        String formattedMessage = formatMessage(message, args);
        setMDC(positionPath);
        LOGGER.warn(formattedMessage);
        clearMDC();
        bufferLog(positionPath, "WARN", formattedMessage, null);
    }

    public static void warn(PositionPath positionPath, Throwable throwable, String message, Object... args) {
        String formattedMessage = formatMessage(message, args);
        setMDC(positionPath);
        LOGGER.warn(formattedMessage, throwable);
        clearMDC();
        bufferLog(positionPath, "WARN", formattedMessage, throwable);
    }

    public static void error(String message, Object... args) {
        errorWithoutThrowable(null, message, args);
    }

    public static void error(Throwable throwable, String message, Object... args) {
        error(null, throwable, message, args);
    }

    public static void error(PositionPath positionPath, String message, Object... args) {
        errorWithoutThrowable(positionPath, message, args);
    }

    public static void errorWithoutThrowable(PositionPath positionPath, String message, Object... args) {
        String formattedMessage = formatMessage(message, args);
        setMDC(positionPath);
        LOGGER.error(formattedMessage);
        clearMDC();
        bufferLog(positionPath, "ERROR", formattedMessage, null);
    }

    public static void error(PositionPath positionPath, Throwable throwable, String message, Object... args) {
        String formattedMessage = formatMessage(message, args);
        setMDC(positionPath);
        LOGGER.error(formattedMessage, throwable);
        clearMDC();
        bufferLog(positionPath, "ERROR", formattedMessage, throwable);

        // Mail senden bei Errors mit Exception
        sendErrorEmail(positionPath, formattedMessage, throwable);
    }

    private static void sendErrorEmail(PositionPath positionPath, String message, Throwable throwable) {
        if (vertx == null || adminEmail == null) {
            return;
        }

        vertx.executeBlocking(() -> {
            String subject = String.format("[Kberry Error] %s - %s", positionPath.getRoom(), message);
            String body = buildErrorEmailBody(positionPath, message, throwable);

            var email = EmailBuilder.startingBlank()
                    .to(adminEmail, adminEmail)
                    .from("Cluster Status", "Kberry@vlab.tools")
                    .withSubject(subject)
                    .withPlainText(body)
                    .withReturnReceiptTo()
                    .buildEmail();
            return mailer.sendMail(email);
        });
    }

    private static String buildErrorEmailBody(PositionPath positionPath, String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ein Fehler ist im Kberry SmartHome System aufgetreten:\n\n");
        sb.append("Zeit: ").append(LocalDateTime.now().format(TIME_FORMATTER)).append("\n");
        sb.append("Location: ").append(positionPath.getLocation()).append("\n");
        sb.append("Ebene: ").append(positionPath.getFloor()).append("\n");
        sb.append("Raum: ").append(positionPath.getRoom()).append("\n");
        sb.append("Position: ").append(positionPath.getPosition()).append("\n\n");
        sb.append("Nachricht: ").append(message).append("\n\n");

        if (throwable != null) {
            sb.append("Exception: ").append(throwable.getClass().getName()).append("\n");
            sb.append("Message: ").append(throwable.getMessage()).append("\n\n");
            sb.append("Stack Trace:\n");
            for (StackTraceElement element : throwable.getStackTrace()) {
                sb.append("  at ").append(element.toString()).append("\n");
            }
        }

        return sb.toString();
    }

    public static List<LogEntry> getLastLogs(PositionPath positionPath) {
        return getLastLogs(positionPath.getRoom());
    }

    public static List<LogEntry> getLastLogs(String room) {
        BoundedStack<LogEntry> stack = LOG_BUFF.get(room);
        if (stack == null) {
            return new ArrayList<>(0);
        }

        return stack.toList();
    }

    public static String getLastLogs(PositionPath positionPath, int minutes) {
        return getLastLogs(positionPath.getRoom(), minutes);
    }

    public static String getLastLogs(String room, int minutes) {
        BoundedStack<LogEntry> stack = LOG_BUFF.get(room);
        if (stack == null) {
            return "Keine Logs für Raum: " + room;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);
        List<LogEntry> entries = stack.toList();

        StringBuilder sb = new StringBuilder();
        for (LogEntry entry : entries) {
            if (entry.timestamp.isAfter(cutoff)) {
                sb.append(entry.format()).append("\n");
            }
        }
        return sb.toString();
    }

    public static List<String> getAllRooms() {
        return List.copyOf(LOG_BUFF.keySet());
    }

    public static void clearLogs(String room) {
        LOG_BUFF.remove(room);
    }

    public record LogEntry(LocalDateTime timestamp, String floor, PositionPath positionPath, String message,
                            Throwable throwable) {

        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(timestamp.format(TIME_FORMATTER)).append("] ");
            sb.append("[").append(floor).append("] ");
            sb.append("[").append(positionPath.getRoom()).append("/").append(positionPath.getPosition()).append("] ");
            sb.append(message);

            if (throwable != null) {
                sb.append(" - Exception: ").append(throwable.getClass().getSimpleName());
                sb.append(": ").append(throwable.getMessage());
            }

            return sb.toString();
        }
    }
}