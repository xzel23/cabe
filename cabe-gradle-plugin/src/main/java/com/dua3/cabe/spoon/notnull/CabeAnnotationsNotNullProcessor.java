package com.dua3.cabe.spoon.notnull;

import com.dua3.cabe.annotations.NotNull;
import org.slf4j.Logger;
import spoon.Launcher;
import spoon.processing.AbstractProcessor;
import spoon.processing.AnnotationProcessor;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetExpression;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtStatement;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtParameter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        logger().debug("processing {} with {} at ", param, getClass().getSimpleName());
        CtExecutable<?> method = Objects.requireNonNull(
                param.getParent(CtExecutable.class), 
                () -> String.format("annotated element '%s' is not inside method/constructor declaration: %s", param.getSimpleName(), param.getOriginalSourceFragment().getSourcePosition()));
        CtBlock<?> body = method.getBody();

        if (body != null) {
            CtAssert<?> ctAssert = getFactory().createAssert();

            CtCodeSnippetExpression<Boolean> assertExpression = getFactory().Core().createCodeSnippetExpression();
            assertExpression.setValue(param.getSimpleName() + "!=null");
            ctAssert.setAssertExpression(assertExpression);

            ctAssert.setExpression(getFactory().Code()
                    .createCodeSnippetExpression(String.format("\"parameter '%s' must not be null\"", param.getSimpleName()))
            );

            // check if explicit constructor call is present
            List<CtStatement> statements = body.getStatements();
            boolean hasSuperConstructorCall = false;
            if (!statements.isEmpty()) {
                CtStatement firstStatement = statements.get(0);
                if ( !firstStatement.isImplicit() && (firstStatement instanceof CtInvocation)) {
                    String statementText = firstStatement.toString();
                    if (statementText.startsWith("super(") || statementText.startsWith("this(")) {
                        hasSuperConstructorCall = true;
                    }
                }
            }

            // insert position: on same line as parameters if no super constructor call, otherwise after call to super()
            SourcePosition position = hasSuperConstructorCall ? statements.get(0).getPosition() : param.getPosition();
            ctAssert.setPosition(position);
                    
            // make sure assignments are in order of parameters
            int idx = hasSuperConstructorCall ? 1 : 0;
            while (idx<statements.size() && (statements.get(idx) instanceof CtAssert)) {
                idx++;
            }
            
            body.addStatement(idx, ctAssert);
        } else {
            logger().debug("parent of annotated element '{}' does not have a body: {}", param.getSimpleName(), param.getOriginalSourceFragment().getSourcePosition());
        }
    }
}
