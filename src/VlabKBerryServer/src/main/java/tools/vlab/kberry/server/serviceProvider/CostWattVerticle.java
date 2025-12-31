package tools.vlab.kberry.server.serviceProvider;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Kostendienst für Stromverbrauch.
 * - Lädt initial den Strompreis und Anbieter aus einer Datei
 * - Optional kann der aktuelle Marktpreis über Corrently API abgefragt werden
 * - Berechnet Kosten und Einsparungen
 */
public class CostWattVerticle extends AbstractVerticle implements CostWattServiceProvider {

    private static final Logger Log = LoggerFactory.getLogger(CostWattVerticle.class);

    @Getter
    private double localPricePerKWh;   // Preis des lokalen Anbieters €/kWh
    @Getter
    private double marketPricePerKWh;  // aktueller Marktpreis €/kWh
    @Getter
    private String providerName;

    private final String localPriceFile;
    private final Duration refreshInterval;

    // --- Neue Konstruktoren für Tests ---
    public CostWattVerticle() {
        this.localPriceFile = "config/local-price";
        this.refreshInterval = Duration.ofDays(1);
    }

    /**
     * Konstruktor für Tests / manuelles Setzen der Werte, ohne Datei zu laden
     */
    public CostWattVerticle(String providerName, double localPrice, double marketPrice) {
        this.localPriceFile = null; // Datei wird nicht geladen
        this.refreshInterval = Duration.ofDays(1);
        this.providerName = providerName;
        this.localPricePerKWh = localPrice;
        this.marketPricePerKWh = marketPrice;
    }

    @Override
    public void start(Promise<Void> startFuture) {
        try {
            if (localPriceFile != null) {
                loadLocalPrice();
            }
            updateMarketPrice();
            // periodisches Update
            vertx.setPeriodic(refreshInterval.toMillis(), id -> updateMarketPrice());
            startFuture.complete();
        } catch (Exception e) {
            startFuture.fail(e);
        }
    }

    @Override
    public double calculateCost(double consumptionKWh) {
        return consumptionKWh * localPricePerKWh;
    }

    @Override
    public double calculateSavings(double consumptionKWh) {
        double marketCost = consumptionKWh * marketPricePerKWh;
        double localCost = consumptionKWh * localPricePerKWh;
        return marketCost - localCost;
    }

    private void loadLocalPrice() throws Exception {
        // Lädt Datei aus Classpath, also src/main/resources oder src/test/resources
        try (var is = getClass().getClassLoader().getResourceAsStream(this.localPriceFile)) {
            if (is == null) throw new Exception("Datei config/local-price nicht gefunden im Classpath");
            String content = new String(is.readAllBytes()).trim();
            String[] parts = content.split(";");
            if (parts.length >= 2) {
                providerName = parts[0].trim();
                localPricePerKWh = Double.parseDouble(parts[1].trim());
            }
        }
    }

    private void updateMarketPrice() {
        WebClient client = WebClient.create(getVertx());
        client
                .getAbs("https://api.corrently.io/v2.0/marketdata")
                .send()
                .onSuccess(body -> {
                    try {
                        JsonObject json = body.bodyAsJsonObject();
                        if (json == null) {
                            Log.error("JSON response body is null {}", body);
                            return;
                        }
                        double priceCt = json.getDouble("current");
                        marketPricePerKWh = priceCt / 100.0;
                        System.out.println("Aktualisierter Marktpreis: " + marketPricePerKWh + " €/kWh");
                    } catch (Exception e) {
                        System.err.println("Fehler beim Verarbeiten des Marktpreises: " + e.getMessage());
                    }
                })
                .onFailure(failure -> {
                    System.err.println("Fehler beim Abrufen des Marktpreises: " + failure.getMessage());
                });
    }

    public void updateMarketPriceSimulated(double price) {
        marketPricePerKWh = price;
    }
}