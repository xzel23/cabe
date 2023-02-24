package com.dua3.cabe.spoon.notnull;

import com.dua3.cabe.annotations.NotNull;
import com.dua3.cabe.annotations.NotNullApi;
import com.dua3.cabe.annotations.Nullable;
import com.dua3.cabe.annotations.NullableApi;
import org.slf4j.Logger;
import spoon.Launcher;
import spoon.processing.AbstractProcessor;
import spoon.processing.AnnotationProcessor;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtStatement;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeInformation;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * An AnnotationProcessor for cabe annotations.
 * <p>
 * - adds assertions for method/constructor parameters annotated with @NotNull
 */
public class CabeAnnotationsNotNullProcessor extends AbstractProcessor<CtParameter<?>> implements AnnotationProcessor<Annotation, CtParameter<?>> {

    private static final Set<Class<? extends Annotation>> NOT_NULL_ANNOTATION_TYPES = Set.of(NotNull.class, NotNullApi.class);
    private static final Set<Class<? extends Annotation>> NULLABLE_ANNOTATION_TYPES = Set.of(Nullable.class, NullableApi.class);

    private static final Set<Class<? extends Annotation>> ALL_ANNOTATION_TYPES = Set.of(NotNull.class, Nullable.class, NotNullApi.class, NullableApi.class);
    
    private static Logger logger() {
        return Launcher.LOGGER;
    }

    /**
     * Constructor.
     */
    public CabeAnnotationsNotNullProcessor() {
        logger().debug("instance created");
    }

    @Override
    public void process(Annotation annotation, CtParameter<?> element) { 
        process(element);
    }

    @Override
    public Set<Class<? extends Annotation>> getProcessedAnnotationTypes() {
        return ALL_ANNOTATION_TYPES;
    }

    @Override
    public Set<Class<? extends Annotation>> getConsumedAnnotationTypes() {
        return ALL_ANNOTATION_TYPES;
    }

    @Override
    public boolean inferConsumedAnnotationType() {
        return false;
    }

    @Override
    public boolean shoudBeConsumed(CtAnnotation<? extends Annotation> annotation) {
        return ALL_ANNOTATION_TYPES.contains(annotation.getAnnotationType());
    }

    @Override
    public void process(CtParameter<?> param) {
        try {
            // primitive types are inherently non-nullable
            if (Optional.ofNullable(param.getType()).map(CtTypeInformation::isPrimitive).orElse(true)) {
                return;
            }

            boolean notNull = false;
            for (CtElement element = param; element != null; element = getAncestor(element)) {
                CtElement el = element;
                Optional<Boolean> notNullAnnotated = getIsNotNullAnnotated(
                        element,
                        annotation -> {
                            if (el == param && shoudBeConsumed(annotation)) {
                                el.removeAnnotation(annotation);
                            }
                        }
                );
                if (notNullAnnotated.isPresent()) {
                    notNull = notNullAnnotated.orElseThrow();
                    break;
                }
            }

            if (notNull) {
                this.processNotNullAnnotatedElement(param);
            }
        } catch (Exception e) {
            throw new IllegalStateException(String.format(
                    "Exception while processing parameter '%s' %s: %s", 
                    param.getSimpleName(), 
                    param.getPosition().toString(),
                    e.getMessage()
            ), e);
        }
    }

    private void processNotNullAnnotatedElement(CtParameter<?> param) {
        logger().debug("processing {} with {} at ", param, getClass().getSimpleName());
        CtExecutable<?> method = Objects.requireNonNull(
                param.getParent(CtExecutable.class), 
                "annotated element is not inside method/constructor declaration");
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
            CtType<?> parentType = method.getParent(CtType.class);
            if (parentType!= null) {
                logger().debug("not generating code for annotated parameter {} because {}.{}(...) does not have a body", 
                        param.getSimpleName(),
                        parentType.getQualifiedName(), 
                        method.getSimpleName() 
                );
            }
        }
    }

    private static Optional<Boolean> getIsNotNullAnnotated(CtElement element, Consumer<CtAnnotation<?>> annotationConsumer) {
        boolean notNullAnnotated = false;
        boolean nullableAnnotated = false;
        for (var annotation: element.getAnnotations()) {
            notNullAnnotated = notNullAnnotated || NOT_NULL_ANNOTATION_TYPES.contains(annotation.getAnnotationType().getActualClass());
            nullableAnnotated = nullableAnnotated || NULLABLE_ANNOTATION_TYPES.contains(annotation.getAnnotationType().getActualClass());

            annotationConsumer.accept(annotation);
        }

        // consistency check
        if (notNullAnnotated && nullableAnnotated) {
            throw new IllegalStateException(String.format(
                    "both nullable and non-nullable annotations present at: %s",
                    element.getOriginalSourceFragment().getSourcePosition()
            ));
        }
        
        return notNullAnnotated || nullableAnnotated
                ? Optional.of(notNullAnnotated)
                : Optional.empty();
    }
    
    private static CtElement getAncestor(CtElement element) {
        // for a method, return the declaring class
        if (element instanceof CtParameter) {
            CtExecutable<?> method = ((CtParameter<?>) element).getParent();
            return method.getParent();
        }
        
        // for a class, return the package
        if (element instanceof CtType<?>) {
            CtType<?> type = (CtType<?>) element;

            // return the outer class
            CtType<?> declaringType = type.getDeclaringType();
            if (declaringType!=null) {
                return declaringType;
            }
            
            return type.getPackage(); 
        }
        
        return null;
    }
}
