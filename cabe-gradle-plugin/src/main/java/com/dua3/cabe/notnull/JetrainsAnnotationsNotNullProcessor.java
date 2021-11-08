package com.dua3.cabe.notnull;

import org.jetbrains.annotations.NotNull;
import spoon.processing.AbstractAnnotationProcessor;
import spoon.reflect.declaration.CtParameter;

public class JetrainsAnnotationsNotNullProcessor extends AbstractAnnotationProcessor<NotNull, CtParameter<?>> implements NotNullProcessorImpl<NotNull> {
    public JetrainsAnnotationsNotNullProcessor() {
    }

    @Override
    public void process(NotNull annotation, CtParameter<?> element) {
        doProcess(this, annotation, element);
    }
}
