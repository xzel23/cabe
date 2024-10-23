package com.dua3.cabe.test.coba;

import com.dua3.cabe.test.coba.api.nullmarked.NullMarkedPackage;
import com.dua3.cabe.test.coba.api.nullunmarked.NullUnmarkedPackage;

public class Coba {

    public static void main(String[] args) {
        ParameterAnnotationsStaticMethods.test();
        ParameterAnnotations.test();
        NonNullPackage.test();
        NullablePackage.test();
    }

}
