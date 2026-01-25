package com.dua3.cabe.processor.test.instrument.equals;

import org.jspecify.annotations.NonNull;

public class EqualsIgnored {
    // private should be ignored (not an override anyway)
    private boolean equals(String s) { return false; }

    // overload should be ignored
    public boolean equals(Object obj, Object other) { return false; }

    // another overload - different name to avoid conflict with the private one above if it was public
    public boolean equals(@NonNull Integer i) { return false; }
}
