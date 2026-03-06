package cabe025;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.util.function.Supplier;

@NullMarked
public class Cabe025 {
    public static void main(String[] args) {
        Cabe025 test = new Cabe025();
        
        // Test 1: loggerName is null
        try {
            test.filterAndDispatch(1L, null, LogLevel.INFO, null, null, new LocationResolver(), () -> "msg", null);
            System.out.println("FAIL: loggerName null check didn't throw NPE");
            return;
        } catch (NullPointerException e) {
            String msg = e.getMessage();
            if (!msg.equals("loggerName is null")) {
                System.out.println("FAIL: loggerName null check message was: " + msg);
                return;
            }
        }

        // Test 2: lvl is null
        try {
            test.filterAndDispatch(1L, "logger", null, null, null, new LocationResolver(), () -> "msg", null);
            System.out.println("FAIL: lvl null check didn't throw NPE");
            return;
        } catch (NullPointerException e) {
            String msg = e.getMessage();
            if (!msg.equals("lvl is null")) {
                System.out.println("FAIL: lvl null check message was: " + msg + " (expected 'lvl is null')");
                return;
            }
        }

        // Test 3: locationResolver is null
        try {
            test.filterAndDispatch(1L, "logger", LogLevel.INFO, null, null, null, () -> "msg", null);
            System.out.println("FAIL: locationResolver null check didn't throw NPE");
            return;
        } catch (NullPointerException e) {
            String msg = e.getMessage();
            if (!msg.equals("locationResolver is null")) {
                System.out.println("FAIL: locationResolver null check message was: " + msg);
                return;
            }
        }

        System.out.println("OK");
    }

    public void filterAndDispatch(
        long timestamp, 
        String loggerName, 
        LogLevel lvl, 
        @Nullable String mrk, 
        @Nullable MDC mdc, 
        LocationResolver locationResolver, 
        Supplier<CharSequence> msg, 
        @Nullable Throwable t
    ) {
        // logic doesn't matter
    }

    public enum LogLevel { INFO }
    public static class MDC {}
    public static class LocationResolver {}
}
