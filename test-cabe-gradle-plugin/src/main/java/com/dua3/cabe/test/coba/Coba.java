package com.dua3.cabe.test.coba;

import com.dua3.cabe.test.coba.api.notnull.NotNullPackage;
import com.dua3.cabe.test.coba.api.nullable.NullablePackage;

public class Coba {

    public static void main(String[] args) {
        ParameterAnnotations.test();
        NotNullPackage.test();
        NullablePackage.test();
    }
    
}
