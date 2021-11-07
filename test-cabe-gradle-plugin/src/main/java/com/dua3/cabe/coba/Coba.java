package com.dua3.cabe.coba;

import org.jetbrains.annotations.NotNull;

public class Coba {
    
    public static void main(String[] args) {
        nonNullAnnotatedArgument("hello world!");
        nonNullAnnotatedArgument(null);
    }
    
    private static void nonNullAnnotatedArgument(@NotNull String arg) {
        System.out.println(arg);
    }
    
}
