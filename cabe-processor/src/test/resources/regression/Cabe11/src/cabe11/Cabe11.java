package cabe11;

import java.util.Collections;

/*
 * Testing Cabe#11: Instrumentation error / method not found
 */
public class Cabe11 {
    public static void main(String[] args) {
        try {
            new ComboBoxEx(null, null, null, String::valueOf, Collections.emptyList());
        } catch (NullPointerException|AssertionError e) {
            //ignore
        }

        System.out.println("OK");
    }
}
