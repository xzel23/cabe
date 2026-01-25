package com.dua3.cabe.processor.test.instrument.equals.neither;

import org.jspecify.annotations.NonNull;

public class EqualsNonNull {
    @Override
    public boolean equals(@NonNull Object obj) {
        return super.equals(obj);
    }
}
