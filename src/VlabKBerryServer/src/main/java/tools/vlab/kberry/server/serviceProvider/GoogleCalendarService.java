package tools.vlab.kberry.server.serviceProvider;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

import java.time.*;

public class GoogleCalendarService extends AbstractVerticle implements CalendarServiceProvider {

    private static final String APPLICATION_NAME = "Kberry SmartHome";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR_READONLY);
    private final GoogleClientSecrets clientSecrets;
    private final String userId;
    private final String calendarId;
    private final String tokenPath;

    private Calendar service;

    private GoogleCalendarService(GoogleClientSecrets clientSecrets, String userId, String calendarId, String tokenPath) {
        this.clientSecrets = clientSecrets;
        this.userId = userId;
        this.calendarId = calendarId;
        this.tokenPath = tokenPath;
    }

    public static GoogleCalendarService fromCredentialsFile(Path credPath, String userId, String calendarId, String tokenPath) throws IOException {
        try (InputStream in = Files.newInputStream(credPath)) {
            return fromCredentialsStream(in, userId, calendarId, tokenPath);
        }
    }

    public static GoogleCalendarService fromCredentialsStream(InputStream stream, String userId, String calendarId, String tokenPath) throws IOException {
        GoogleClientSecrets secrets = GoogleClientSecrets.load(
                JSON_FACTORY,
                new InputStreamReader(stream)
        );
        return new GoogleCalendarService(secrets, userId, calendarId, tokenPath);
    }

    @Override
    public void start() throws Exception {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        var tokenDir = new java.io.File(tokenPath);

        var flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(tokenDir))
                .setAccessType("offline")
                .build();

        var credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize(userId);

        this.service = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Future<Boolean> hasEvents(LocalDate date) {
        return this.getVertx().executeBlocking(() -> {
            var start = date.atStartOfDay();
            var end = date.plusDays(1).atStartOfDay();
            var startTime = new DateTime(start.toInstant(ZoneOffset.UTC).toEpochMilli());
            var endTime = new DateTime(end.toInstant(ZoneOffset.UTC).toEpochMilli());
            Events events = service.events().list(calendarId)
                    .setTimeMin(startTime)
                    .setTimeMax(endTime)
                    .setSingleEvents(true)
                    .execute();
            return !events.getItems().isEmpty(); // true = busy
        }, false);
    }

    @Override
    public Future<Boolean> isAvailableNow() {
        return hasEvents(LocalDate.now());
    }

    @Override
    public Future<Boolean> tomorrowAvailable() {
        return hasEvents(LocalDate.now().plusDays(1));
    }

    @Override
    public Future<Boolean> availableInTwoDays() {
        return hasEvents(LocalDate.now().plusDays(2));
    }
}