package com.dua3.cabe.spoon.notnull;

import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import spoon.processing.AbstractAnnotationProcessor;
import spoon.reflect.declaration.CtParameter;

import org.slf4j.Logger;

public class JetBrainsAnnotationsNotNullProcessor extends AbstractAnnotationProcessor<NotNull, CtParameter<?>> implements NotNullProcessorImpl<NotNull> {

    private static final Logger LOG = LoggerFactory.getLogger(CabeAnnotationsNotNullProcessor.class);

    public JetBrainsAnnotationsNotNullProcessor() {
    }

    public Logger logger() {
        return LOG;
    }

    @Override
    public void process(NotNull annotation, CtParameter<?> element) {
        doProcess(this, annotation, element);
    }

}
