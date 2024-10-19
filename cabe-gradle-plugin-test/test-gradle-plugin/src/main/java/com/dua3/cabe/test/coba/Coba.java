package com.dua3.cabe.test.coba;

import com.dua3.cabe.test.coba.api.nonnull.NonNullPackage;
import com.dua3.cabe.test.coba.api.nullable.NullablePackage;

public class Coba {

    public static void main(String[] args) {
        ParameterAnnotationsStaticMethods.test();
        ParameterAnnotations.test();
        NonNullPackage.test();
        NullablePackage.test();
    }

}
