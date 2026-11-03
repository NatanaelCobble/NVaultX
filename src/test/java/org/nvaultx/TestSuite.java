package org.nvaultx;

import org.powernukkitx.plugin.Plugin;
import org.powernukkitx.plugin.service.NKServiceManager;
import org.powernukkitx.plugin.service.ServicePriority;
import org.nvaultx.api.EconomyResponse.Status;
import org.nvaultx.api.Provider;
import org.nvaultx.bridge.MoneyOperations;
import java.lang.reflect.Proxy;

/** Deterministic contract tests; these do not claim to exercise a live Bedrock client. */
public final class TestSuite {
    private static int assertions;
    private static final class Account implements MoneyOperations.Account {
        double balance;
        int writes;
        boolean reject, fail, partial;
        Account(double balance) { this.balance = balance; }
        public double balance() { return balance; }
        public void deposit(double amount) { change(amount); }
        public void withdraw(double amount) { change(-amount); }
        private void change(double amount) {
            writes++;
            if (reject) return;
            balance += partial ? amount / 2 : amount;
            if (fail) throw new IllegalStateException("uncertain storage");
        }
    }
    private static void check(boolean value, String label) {
        assertions++;
        if (!value) throw new AssertionError(label);
    }
    public static void main(String[] args) throws Exception {
        for (double amount : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.001, 1e13}) {
            Account account = new Account(100);
            check(MoneyOperations.apply(account, amount, true).status() == Status.FAILURE, "invalid amount " + amount);
            check(account.writes == 0 && account.balance == 100, "invalid amount cannot mutate");
        }
        Account account = new Account(10);
        check(MoneyOperations.apply(account, 11, true).status() == Status.FAILURE, "insufficient funds");
        check(account.writes == 0, "insufficient funds cannot mutate");
        check(MoneyOperations.apply(account, 10, true).successful() && account.balance == 0, "exact withdrawal");
        check(MoneyOperations.apply(account, 0.1, false).successful(), "decimal deposit");
        check(MoneyOperations.apply(account, 0.2, false).successful(), "floating point addition");
        check(MoneyOperations.apply(account, 0.1, false).successful(), "operation after floating point addition");
        Account rounding = new Account(0.3 - 0.2);
        check(MoneyOperations.apply(rounding, 0.1, true).status() == Status.UNKNOWN,
                "sub-cent residual must not be silently treated as confirmed zero by a non-clamping provider");
        int writes = account.writes;
        check(MoneyOperations.apply(account, 0, true).successful() && account.writes == writes, "zero no write");
        Account reject = new Account(10); reject.reject = true;
        check(MoneyOperations.apply(reject, 1, false).status() == Status.FAILURE, "rejected mutation");
        Account partial = new Account(10); partial.partial = true;
        check(MoneyOperations.apply(partial, 2, false).status() == Status.UNKNOWN, "partial mutation unknown");
        Account failing = new Account(10); failing.fail = true;
        check(MoneyOperations.apply(failing, 2, false).status() == Status.UNKNOWN && failing.balance == 12, "failure after write unknown");
        Account limit = new Account(1e12);
        check(MoneyOperations.apply(limit, 0.01, false).status() == Status.FAILURE && limit.writes == 0, "overflow limit");
        for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1, 1.001}) {
            Account invalid = new Account(bad);
            check(MoneyOperations.apply(invalid, 1, false).status() == Status.FAILURE && invalid.writes == 0, "invalid stored balance");
        }
        services();
        System.out.println("PASS: " + assertions + " assertions (money, native PNX services)");
    }
    private static void services() {
        Plugin owner = (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class[]{Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isEnabled" -> true;
                    case "getName" -> "TestOwner";
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> null;
                });
        NKServiceManager services = new NKServiceManager();
        Provider low = provider("low"), high = provider("high");
        check(services.getProvider(Provider.class) == null, "missing service");
        check(services.register(Provider.class, low, owner, ServicePriority.LOWEST), "register fallback");
        check(services.register(Provider.class, high, owner, ServicePriority.NORMAL), "register preferred");
        check(services.getProvider(Provider.class).getProvider() == high, "priority selection");
        check(!services.register(Provider.class, provider("duplicate"), owner, ServicePriority.NORMAL), "PNX same priority rejection");
        services.cancel(Provider.class, high);
        check(services.getProvider(Provider.class).getProvider() == low, "fallback after cancellation");
        services.cancel(owner);
        check(services.getProvider(Provider.class) == null, "owner cancellation");
    }
    private static Provider provider(String name) {
        return new Provider() {
            public String getName() { return name; }
            public boolean isEnabled() { return true; }
        };
    }
}
