package com.dua3.cabe.gradle;

import org.apache.log4j.Logger;
import spoon.processing.AbstractAnnotationProcessor;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetExpression;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NotNullProcessor extends AbstractAnnotationProcessor<Annotation, CtParameter<?>> {

    private static final Logger LOG = Logger.getLogger(NotNullProcessor.class.getName());
    
    private static final Set<Class<? extends Annotation>> ANNOTATIONS_NOT_NULL = mapAnnotationsToClass(
            "org.jetbrains.annotations.NotNull"
    );

    private static Set<Class<? extends Annotation>> mapAnnotationsToClass(String... annotationClassNames) {
        Set<Class<? extends Annotation>> annotationClasses = new HashSet<>();
        for (String annotationClassName: annotationClassNames) {
            try {
                annotationClasses.add((Class<? extends Annotation>) Class.forName(annotationClassName));
            } catch (ClassNotFoundException e) {
                LOG.debug("annotation class not on classpath: " + annotationClassName); 
            }
        }
        return Collections.unmodifiableSet(annotationClasses);
    }

    @Override
    public Set<Class<? extends CtElement>> getProcessedElementTypes() {
        return super.getProcessedElementTypes();
    }

    @Override
    public boolean inferConsumedAnnotationType() {
        return false;
    }

    @Override
    public void process(Annotation annotation, CtParameter<?> param) {
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

