package com.dua3.cabe.notnull;

import com.dua3.cabe.annotations.NotNull;
import spoon.processing.AbstractAnnotationProcessor;
import spoon.reflect.declaration.CtParameter;

public class CabeAnnotationsNotNullProcessor extends AbstractAnnotationProcessor<NotNull, CtParameter<?>> implements NotNullProcessorImpl<NotNull> {
    public CabeAnnotationsNotNullProcessor() {
    }

    @Override
    public void process(NotNull annotation, CtParameter<?> element) {
        doProcess(this, annotation, element);
    }
}
