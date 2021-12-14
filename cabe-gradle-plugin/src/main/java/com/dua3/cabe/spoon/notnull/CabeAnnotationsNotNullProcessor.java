package com.dua3.cabe.spoon.notnull;

import com.dua3.cabe.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.compiler.Environment;
import spoon.processing.AbstractAnnotationProcessor;
import spoon.reflect.declaration.CtParameter;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CabeAnnotationsNotNullProcessor extends AbstractAnnotationProcessor<NotNull, CtParameter<?>> implements NotNullProcessorImpl<NotNull> {
    
    private static final Logger LOG = LoggerFactory.getLogger(CabeAnnotationsNotNullProcessor.class);
            
    public CabeAnnotationsNotNullProcessor() {
        LOG.debug("instance created");
    }

    public Logger logger() {
        return LOG;
    }
    
    @Override
    public void process(NotNull annotation, CtParameter<?> element) {
        doProcess(this, annotation, element);
    }

    @Override
    public void patchEnvironment(Environment env) {
        try {
            Class<NotNull> annotationClass = NotNull.class;
            URL classLocation = annotationClass.getProtectionDomain().getCodeSource().getLocation();
            Path parent = Paths.get(classLocation.toURI()).getParent();
            if (parent == null) {
                LOG.warn("could not determine classpath location of {}", annotationClass);
            } else {
                LOG.debug("adding {} to source class path", parent);
                List<String> sourceClasspath = new ArrayList<>();
                Optional.ofNullable(env.getSourceClasspath()).ifPresent(cp -> Arrays.stream(cp).forEach(sourceClasspath::add));
                sourceClasspath.add(parent.toString());
                env.setSourceClasspath(sourceClasspath.toArray(String[]::new));
            }
        } catch (URISyntaxException e) {
            LOG.error("could not update source classpath", e);
        }
    }
}
