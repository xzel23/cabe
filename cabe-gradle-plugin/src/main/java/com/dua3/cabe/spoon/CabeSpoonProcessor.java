package com.dua3.cabe.spoon;

import spoon.compiler.Environment;
import spoon.processing.Processor;
import spoon.reflect.declaration.CtElement;

public interface CabeSpoonProcessor<T extends CtElement> extends Processor<T> {
    default void patchEnvironment(Environment env) {}
}
