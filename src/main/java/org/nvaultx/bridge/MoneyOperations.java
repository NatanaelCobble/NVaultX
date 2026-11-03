package org.nvaultx.bridge;

import org.nvaultx.api.EconomyResponse;
import org.nvaultx.api.EconomyResponse.Status;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Validates before mutation and detects unconfirmed writes without pretending to roll them back. */
public final class MoneyOperations {
    public interface Account {
        double balance();
        void deposit(double amount);
        void withdraw(double amount);
    }
    private MoneyOperations() { }
    public static boolean validAmount(double amount) {
        return Double.isFinite(amount) && amount >= 0 && amount <= 1_000_000_000_000d
                && BigDecimal.valueOf(amount).stripTrailingZeros().scale() <= 2;
    }
    public static EconomyResponse apply(Account account, double amount, boolean withdraw) {
        if (!validAmount(amount)) return result(Status.FAILURE, amount, Double.NaN, "Montant invalide (maximum 2 decimales, 10^12)");
        final double before;
        try { before = account.balance(); }
        catch (RuntimeException e) { return result(Status.FAILURE, amount, Double.NaN, "Solde indisponible"); }
        if (!Double.isFinite(before) || before < 0 || before > 1_000_000_000_000d)
            return result(Status.FAILURE, amount, before, "Solde hors limites du connecteur");
        double decimalBefore = BigDecimal.valueOf(before).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
        if (Math.abs(decimalBefore - before) > Math.ulp(before) * 2)
            return result(Status.FAILURE, amount, before, "Solde avec plus de 2 decimales");
        if (withdraw && decimalBefore < amount) return result(Status.FAILURE, amount, before, "Solde insuffisant");
        double expected = BigDecimal.valueOf(decimalBefore).add(BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(withdraw ? -1 : 1))).doubleValue();
        if (!validAmount(expected)) return result(Status.FAILURE, amount, before, "Limite de solde depassee");
        if (amount == 0) return result(Status.SUCCESS, 0, before, "");
        try {
            if (withdraw) account.withdraw(amount); else account.deposit(amount);
            double after = account.balance();
            // Floating-point addition can differ slightly from a decimal sum; never round real balances here.
            if (Double.isFinite(after) && Math.abs(after - expected) <= Math.ulp(expected) * 2)
                return result(Status.SUCCESS, amount, after, "");
            if (after == before) return result(Status.FAILURE, amount, after, "Operation refusee par le fournisseur");
            return result(Status.UNKNOWN, amount, after, "Solde inattendu : ne pas reessayer automatiquement");
        } catch (RuntimeException e) {
            return result(Status.UNKNOWN, amount, Double.NaN, "Resultat inconnu : ne pas reessayer automatiquement");
        }
    }
    private static EconomyResponse result(Status status, double amount, double balance, String message) {
        return new EconomyResponse(status, amount, balance, message);
    }
}
