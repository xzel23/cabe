package com.dua3.cabe.processor.test.instrument.equals.nullmarked;

import org.jspecify.annotations.Nullable;

public class EqualsNullable {
    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }
}
