package com.dua3.cabe.processor;

import java.io.Serializable;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Configuration class represents a configuration for validating public and private APIs.
 *
 * <p>The Configuration provides a function to parse a configuration string and methods to generate
 * a configuration string representation from its state. Configuration includes a {@link Check} enum which
 * encompasses different validation strategies, and a {@link StandardConfig} enum for standard configurations.
 *
 * @param publicApi  the validation strategy for public APIs.
 * @param privateApi the validation strategy for private APIs.
 */
public record Configuration(Check publicApi, Check privateApi) implements Serializable {
    private static final Logger LOG = Logger.getLogger(Configuration.class.getName());

    /**
     * Parses a configuration string and returns a corresponding Configuration object.
     *
     * @param configStr the configuration string to parse. It can be one of the predefined strings:
     *                  "standard", "development", "no-checks", or a custom configuration string in
     *                  the format "singleParam" or "publicApi=publicParam:privateApi=privateParam".
     * @return the Configuration object corresponding to the parsed configuration string.
     * @throws IllegalArgumentException if the configuration string is invalid.
     * @throws IllegalStateException if the configuration string cannot be parsed.
     */
    public static Configuration parseConfigString(String configStr) {
        switch (configStr) {
            case "standard" -> {return StandardConfig.STANDARD.config;}
            case "development" -> {return StandardConfig.DEVELOPMENT.config;}
            case "no-checks" -> {return StandardConfig.NO_CHECKS.config;}
        }

        LOG.fine(() -> "parsing custom configuration: " + configStr);
        Pattern pattern = Pattern.compile("(?<singleParam>\\w+)|(publicApi=(?<publicParam>\\w+):privateApi=(?<privateParam>\\w+))");
        Matcher matcher = pattern.matcher(configStr);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("invalid configuration string: '" + configStr + "'");
        }

        String singleParam = matcher.group("singleParam");
        if (singleParam != null) {
            Check c = Check.valueOf(singleParam);
            return new Configuration(c, c);
        }

        String publicParam = matcher.group("publicParam");
        String privateParam = matcher.group("privateParam");
        if (publicParam != null && privateParam != null) {
            return new Configuration(Check.valueOf(publicParam), Check.valueOf(privateParam));
        }

        throw new IllegalStateException("could not parse configuration string: '" + configStr + "'");
    }

    /**
     * Constructs a configuration string composed of public API and private API names.
     *
     * @return A string in the format "publicApi=&lt;Public-API-Name&gt;:privateApi=&lt;Private-API-Name&gt;".
     */
    public String getConfigString() {
        return "publicApi=" + publicApi.name() + ":privateApi=" +privateApi.name();
    }

    /**
     * The {@code Check} enum defines various strategies for handling null checks or other assertions in the code.
     */
    public enum Check {
        /**
         * Do not generate Checks.
         */
        NO_CHECK,
        /**
         * Generate standard assertions that can be controlled at runtime (i.e., g {@code -ea} and {@code -da} JVM flags).
         */
        ASSERT,
        /**
         * Throw a NullPointerException if parameter is null.
         */
        THROW_NPE,
        /**
         * Throw an AssertionError if parameter is null, regardless of the assertion setting.
         */
        ASSERT_ALWAYS
    }

    /**
     * Enum representing different standard configurations for an application.
     * Each configuration corresponds to a specific set of checks that can be
     * applied during development or runtime.
     *
     * <ul>
     * <li>DEVELOPMENT: Configuration for development with strict checks to detect problems early.
     * <li>STANDARD: Configuration for standard runtime environment with moderate checks, i.e.,  <li>NO_CHECKS: Configuration with all checks disabled.
     * </ul>
     *
     * Each enum constant holds a Configuration object that specifies the checks to apply.
     */
    public enum StandardConfig {
        /**
         * When the DEVELOPMENT setting is used, parameters are always checked and for violations an
         * {@link AssertionError} is thrown even when assertions are disabled.
         */
        DEVELOPMENT(new Configuration(Check.ASSERT_ALWAYS, Check.ASSERT_ALWAYS)),
        /**
         * When the  STANDARD setting is used, parameters are checked differently depending of whether the method is
         * part of the public or private API:
         * <ul>
         * <li>Public API: a {@link NullPointerException} is thrown when a violation is detected
         * <li>Private API: a standard assertion is used that can be controlled by the standard JVM flags
         * </ul>
         */
        STANDARD(new Configuration(Check.THROW_NPE, Check.ASSERT)),
        /**
         * When NO_CHECKS is used, no parameter checks are generated.
         */
        NO_CHECKS(new Configuration(Check.NO_CHECK, Check.NO_CHECK));

        StandardConfig(Configuration config) {
            this.config = config;
        }

        private static final long serialVersionUID = 1L;

        private final Configuration config;

        /**
         * Retrieves the configuration.
         *
         * @return the current {@link Configuration} denoted by this enum value.
         */
        public Configuration config() {
            return config;
        }
    }
}
