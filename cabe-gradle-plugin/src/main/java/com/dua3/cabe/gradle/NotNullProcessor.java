package com.dua3.cabe.gradle;

import org.jetbrains.annotations.NotNull;
import spoon.processing.AbstractAnnotationProcessor;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetExpression;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;

import java.lang.annotation.Annotation;

public class NotNullProcessor extends AbstractAnnotationProcessor<NotNull, CtParameter<?>> {

    @Override
    public void process(NotNull annotation, CtParameter<?> param) {
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

