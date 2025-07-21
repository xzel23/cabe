package com.dua3.cabe.processor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The Configuration class represents a configuration for validating public and private APIs.
 *
 * <p>The Configuration provides a function to parse a configuration string and methods to generate
 * a configuration string representation from its state. Configuration includes a {@link Check} enum which
 * encompasses different validation strategies.
 *
 * @param publicApi  the validation strategy for public APIs.
 * @param privateApi the validation strategy for private APIs.
 * @param checkReturn the validation strategy for return values.
 */
public record Configuration(Check publicApi, Check privateApi, Check checkReturn) implements Serializable {
    private static final Logger LOG = Logger.getLogger(Configuration.class.getName());

    /**
     * When the DEVELOPMENT setting is used, parameters are always checked and for violations an
     * {@link AssertionError} is thrown even when assertions are disabled.
     */
    public static final Configuration DEVELOPMENT = new Configuration(Check.ASSERT_ALWAYS, Check.ASSERT_ALWAYS, Check.ASSERT_ALWAYS);

    /**
     * When the STANDARD setting is used, parameters are checked differently depending on whether the method is
     * part of the public or private API:
     * <ul>
     * <li>Public API: a {@link NullPointerException} is thrown when a violation is detected
     * <li>Private API: a standard assertion is used that can be controlled by the standard JVM flags
     * </ul>
     */
    public static final Configuration STANDARD = new Configuration(Check.THROW_NPE, Check.ASSERT, Check.NO_CHECK);

    /**
     * When NO_CHECK is used, no parameter checks are generated.
     */
    public static final Configuration NO_CHECKS =new Configuration(Check.NO_CHECK, Check.NO_CHECK, Check.NO_CHECK);
    public static final String PUBLIC_API = "publicApi";
    public static final String PRIVATE_API = "privateApi";
    public static final String RETURN_VALUE = "returnValue";

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
    public static Configuration parse(String configStr) {
        switch (configStr) {
            case "STANDARD":
                return STANDARD;
            case "DEVELOPMENT":
                return DEVELOPMENT;
            case "NO_CHECKS":
                return NO_CHECKS;
            default:
        }

        LOG.fine(() -> "parsing custom configuration: " + configStr);
        Pattern pattern = Pattern.compile("(^(?<singleCheck>\\w+)$)|" +
                "((publicApi=(?<publicApi>\\w+))|(privateApi=(?<privateApi>\\w+))|(returnValue=(?<returnValue>\\w+)))(:?|$)");

        Matcher matcher = pattern.matcher(configStr);

        Map<String, Check> checks = new HashMap<>();
        while (matcher.find()) {
            Stream.of("singleCheck", PUBLIC_API, PRIVATE_API, RETURN_VALUE)
                            .forEach(group -> checks.compute(group, (k, v) -> updateCheck(k, v, matcher.group(k))));
        }

        if (checks.isEmpty()) {
            throw new IllegalArgumentException("invalid configuration string: '" + configStr + "'");
        }

        Optional.ofNullable(checks.get("singleCheck")).ifPresent(c -> {checks.put(PUBLIC_API, c); checks.put(PRIVATE_API, c); checks.put(RETURN_VALUE, c);});

        return new Configuration(
                checks.getOrDefault(PUBLIC_API, Check.NO_CHECK),
                checks.getOrDefault(PRIVATE_API, Check.NO_CHECK),
                checks.getOrDefault(RETURN_VALUE, Check.NO_CHECK)
        );
    }

    /**
     * Updates the given check based on the provided check value.
     *
     * @param checkName  the name of the check being updated
     * @param oldCheck   the existing check
     * @param checkValue the new value for the check
     * @return the updated check, which may be the same as the old check if the new value is null or empty
     * @throws IllegalStateException if both oldCheck and newCheck are non-null, indicating a duplicate declaration
     */
    private static Check updateCheck(String checkName, Check oldCheck, String checkValue) {
        Check newCheck = checkValue == null || checkValue.isEmpty() ? null : Check.valueOf(checkValue);
        if (oldCheck != null && newCheck != null) {
            throw new IllegalStateException("duplicate declaration for " + checkName);
        }
        return Optional.ofNullable(newCheck).orElse(oldCheck);
    }

    /**
     * Constructs a configuration string composed of public API and private API names.
     *
     * @return A string in the format "publicApi=&lt;Public-API-Name&gt;:privateApi=&lt;Private-API-Name&gt;".
     */
    public String getConfigString() {
        return "publicApi=" + publicApi.name() + ":privateApi=" +privateApi.name() + ":returnValue=" + checkReturn.name();
    }

    /**
     * The {@code Check} enum defines various strategies for handling null checks or other assertions in the code.
     */
    public enum Check {
        /**
         * Do not generate Checks.
         */
        NO_CHECK(null, null),

        /**
         * Generate standard assertions that can be controlled at runtime (i.e., g {@code -ea} and {@code -da} JVM flags).
         *
         * <p>If assertions are enabled, violations will result in throwing an {@link AssertionError}.
         */

        ASSERT("new AssertionError((Object) ", ")"),

        /**
         * Throw a {@link NullPointerException} if parameter is null.
         */
        THROW_NPE("new NullPointerException(",")"),

        /**
         * Throw an {@link AssertionError} if parameter is null, regardless of the assertion setting.
         */
        ASSERT_ALWAYS("new AssertionError((Object) ", ")"),

        /**
         * Throw an {@link IllegalArgumentException} if parameter is null.
         */
        THROW_IAE("new IllegalArgumentException(",")");

        private final String prefix;
        private final String suffix;

        Check(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        /**
         * Generate the code fragment that creates a new instance of this Check instance's throwable class.
         * @param message the text to use as the message parameter of the throwable constructor.
         *
         * @return the code to create an instance of this Check instance's throwable
         */
        public Optional<String> getCodeForNewInstance(String message) {
            return Optional.ofNullable(prefix).map(pre -> pre + message + suffix);
        }
    }

}
