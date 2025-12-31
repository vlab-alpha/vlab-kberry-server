package tools.vlab.kberry.server.serviceProvider;

public interface CostWattServiceProvider {
    double calculateCost(double consumptionLastDay);
    double calculateSavings(double consumptionLastDay);
}
