package tools.vlab.kberry.server.serviceProvider;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
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
        Log.info("Starting CostWattVerticle");
        try {
            if (localPriceFile != null) {
                loadLocalPrice()
                        .compose(none -> updateMarketPrice())
                        .compose(none -> {
                            vertx.setPeriodic(refreshInterval.toMillis(), id -> updateMarketPrice());
                            return Future.succeededFuture();
                        }).onSuccess(result -> startFuture.complete())
                        .onFailure(startFuture::fail);
            } else {
                providerName = "";
                localPricePerKWh = 1.2;
                marketPricePerKWh = 1.2;
                startFuture.complete();
            }
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

    private Future<Void> loadLocalPrice() {
        return this.getVertx().fileSystem().readFile(localPriceFile).compose(file -> {
            var json = file.toJsonObject();
            providerName = json.getString("provider");
            localPricePerKWh = json.getDouble("localPricePerKWh");
            return Future.succeededFuture();
        });
    }

    private Future<Void> updateMarketPrice() {
        WebClient client = WebClient.create(getVertx());
        return client
                .getAbs("https://api.corrently.io/v2.0/marketdata")
                .send()
                .compose(body -> {
                    try {
                        JsonObject json = body.bodyAsJsonObject();
                        if (json == null) {
                            Log.error("JSON response body is null {} use default 13.0c", body);
                            marketPricePerKWh = 13.0;
                            return Future.succeededFuture();
                        }
                        double priceCt = json.getDouble("current");
                        System.out.println("Aktualisierter Marktpreis: " + marketPricePerKWh + " €/kWh");
                        marketPricePerKWh = priceCt / 100.0;
                        return Future.succeededFuture();
                    } catch (Exception e) {
                        return Future.failedFuture(e);
                    }
                });
    }

    public void updateMarketPriceSimulated(double price) {
        marketPricePerKWh = price;
    }
}