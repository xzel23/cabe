package com.dua3.cabe.spoon.notnull;

import com.dua3.cabe.annotations.NotNull;
import org.slf4j.Logger;
import spoon.Launcher;
import spoon.processing.AbstractProcessor;
import spoon.processing.AnnotationProcessor;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetExpression;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CabeAnnotationsNotNullProcessor extends AbstractProcessor<CtParameter<?>> implements AnnotationProcessor<Annotation, CtParameter<?>> {

    private static final Set<Class<? extends Annotation>> ANNOTATION_TYPES = Set.of(NotNull.class);
    private static final Set<String> ANNOTATION_TYPE_NAMES = ANNOTATION_TYPES.stream().map(Class::getCanonicalName).collect(Collectors.toSet());

    private static final Logger logger() {
        return Launcher.LOGGER;
    }

    public CabeAnnotationsNotNullProcessor() {
        logger().debug("instance created");
    }

    public boolean isMatchhingAnnotation(CtAnnotation<? extends Annotation> annotation) {
        return ANNOTATION_TYPE_NAMES.contains(annotation.getAnnotationType().getQualifiedName());
    }

    @Override
    public void process(Annotation annotation, CtParameter<?> element) { 
        process(element);
    }

    @Override
    public Set<Class<? extends Annotation>> getProcessedAnnotationTypes() {
        return ANNOTATION_TYPES;
    }

    @Override
    public Set<Class<? extends Annotation>> getConsumedAnnotationTypes() {
        return ANNOTATION_TYPES;
    }

    @Override
    public boolean inferConsumedAnnotationType() {
        return false;
    }

    @Override
    public boolean shoudBeConsumed(CtAnnotation<? extends Annotation> annotation) {
        return isMatchhingAnnotation(annotation);
    }
/*
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
*/
    @Override
    public void process(CtParameter<?> element) {
        // work on a copy of the annotations because annotations are removed inside loop
        List<CtAnnotation<? extends Annotation>> annotations = new ArrayList<>(element.getAnnotations());
        for (var annotation: annotations) {
            if (isMatchhingAnnotation(annotation)) {
                try {
                    this.processNotNullAnnotatedElement(element);
                } catch (Exception var5) {
                    Launcher.LOGGER.error(var5.getMessage(), var5);
                }

                if (this.shoudBeConsumed(annotation)) {
                    element.removeAnnotation(annotation);
                }
            }
        }
    }

    private void processNotNullAnnotatedElement(CtParameter<?> param) {
        logger().debug("processing {} with {}", param, getClass().getSimpleName());
        CtMethod<?> method = param.getParent(CtMethod.class);
        CtBlock<?> body = method.getBody();

        CtAssert<?> ctAssert = getFactory().createAssert();

        CtCodeSnippetExpression<Boolean> assertExpression = getFactory().Core().createCodeSnippetExpression();
        assertExpression.setValue(param.getSimpleName()+"!=null");
        ctAssert.setAssertExpression(assertExpression);

        ctAssert.setExpression(getFactory().Code()
                .createCodeSnippetExpression(String.format("\"parameter %s must not be null\"",  param.getSimpleName()))
        );

        body.insertBegin(ctAssert);
    }
}
