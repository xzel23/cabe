package com.dua3.cabe.processor;

import javassist.CtClass;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Utility class for working with the CabeMeta attribute.
 * This attribute is added to class files that have been processed by the Cabe processor.
 */
public class CabeAttribute {
    private static final Logger LOG = Logger.getLogger(CabeAttribute.class.getName());
    private static final String ATTRIBUTE_NAME = "CabeMeta";
    private static final String SET_PROCESSOR_VERSION = "processorVersion=";

    private CabeAttribute() { /* utility class constructor */ }

    /**
     * Adds the CabeMeta attribute to a class file.
     *
     * @param ctClass          the class to add the attribute to
     * @param processorVersion the processor version to include in the attribute
     */
    public static void addToClass(CtClass ctClass, String processorVersion) {
        ClassFile classFile = ctClass.getClassFile();
        String attributeValue = SET_PROCESSOR_VERSION + processorVersion;
        byte[] data = attributeValue.getBytes(StandardCharsets.UTF_8);
        AttributeInfo attribute = new AttributeInfo(classFile.getConstPool(), ATTRIBUTE_NAME, data);
        classFile.addAttribute(attribute);
        LOG.fine(() -> "Added CabeMeta attribute to class " + ctClass.getName() + " with value: " + attributeValue);
    }

    /**
     * Checks if a class file already has the CabeMeta attribute.
     *
     * @param ctClass the class to check
     * @return true if the class has the CabeMeta attribute, false otherwise
     */
    public static boolean hasAttribute(CtClass ctClass) {
        ClassFile classFile = ctClass.getClassFile();
        return classFile.getAttribute(ATTRIBUTE_NAME) != null;
    }

    /**
     * Gets the processor version from the CabeMeta attribute of a class file.
     *
     * @param ctClass the class to get the processor version from
     * @return the processor version, or null if the attribute is not present or does not contain a valid processor version
     */
    public static String getProcessorVersion(CtClass ctClass) {
        ClassFile classFile = ctClass.getClassFile();
        AttributeInfo attribute = classFile.getAttribute(ATTRIBUTE_NAME);
        if (attribute == null) {
            return null;
        }

        String attributeValue = new String(attribute.get(), StandardCharsets.UTF_8);
        if (attributeValue.startsWith(SET_PROCESSOR_VERSION)) {
            return attributeValue.substring(SET_PROCESSOR_VERSION.length());
        }
        return null;
    }
}