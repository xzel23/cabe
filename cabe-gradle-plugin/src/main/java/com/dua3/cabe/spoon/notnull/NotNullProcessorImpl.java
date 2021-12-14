package com.dua3.cabe.spoon.notnull;

import com.dua3.cabe.spoon.CabeSpoonProcessor;
import spoon.processing.AbstractAnnotationProcessor;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetExpression;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;

import org.slf4j.Logger;

import java.lang.annotation.Annotation;

public interface NotNullProcessorImpl<A extends Annotation> extends CabeSpoonProcessor<CtParameter<?>> {
    
    Logger logger();
    
    default void doProcess(AbstractAnnotationProcessor<A, CtParameter<?>> processor, A annotation, CtParameter<?> param) {
        logger().debug("processing {} with {}", param, getClass().getSimpleName());
        CtMethod<?> method = param.getParent(CtMethod.class);
        CtBlock<?> body = method.getBody();
 
        CtAssert<?> ctAssert = processor.getFactory().createAssert();
        
        CtCodeSnippetExpression<Boolean> assertExpression = processor.getFactory().Core().createCodeSnippetExpression();
        assertExpression.setValue(param.getSimpleName()+"!=null");
        ctAssert.setAssertExpression(assertExpression);
        
        ctAssert.setExpression(processor.getFactory().Code()
                .createCodeSnippetExpression(String.format("\"parameter %s must not be null\"",  param.getSimpleName()))
        );

        body.insertBegin(ctAssert);
    }
    
}
