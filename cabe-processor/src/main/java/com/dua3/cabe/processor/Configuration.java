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
public record Configuration(Check publicApi, Check privateApi, Check checkReturn, boolean strict) implements Serializable {
    private static final Logger LOG = Logger.getLogger(Configuration.class.getName());

    /**
     * Constructs a new {@code Configuration} instance with the provided {@code Check} configurations
     * for public API, private API, and return value handling.
     *
     * @param publicApi  the {@code Check} strategy to apply to the public API.
     * @param privateApi the {@code Check} strategy to apply to the private API.
     * @param checkReturn the {@code Check} strategy to apply for return values.
     */
    public Configuration(Check publicApi, Check privateApi, Check checkReturn) {
        this(publicApi, privateApi, checkReturn, false);
    }

    /**
     * Returns a new {@code Configuration} instance with the specified strictness setting.
     *
     * @param strict a boolean value indicating whether the configuration should enforce strict checks.
     * @return a new {@code Configuration} object updated with the provided strictness setting.
     */
    public Configuration withStrict(boolean strict) {
        return new Configuration(publicApi, privateApi, checkReturn, strict);
    }

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
    public static final Configuration NO_CHECKS = new Configuration(Check.NO_CHECK, Check.NO_CHECK, Check.NO_CHECK, false);
    /**
     * String constant to define parameter checks for public API methods.
     */
    public static final String PUBLIC_API = "publicApi";
    /**
     * String constant to define parameter checks for private API methods.
     */
    public static final String PRIVATE_API = "privateApi";
    /**
     * String constant to define return value checks.
     */
    public static final String RETURN_VALUE = "returnValue";
    /**
     * String constant to define strict mode.
     */
    public static final String STRICT = "strict";

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
        Configuration base;
        String remaining;
        if (configStr.equals("STANDARD") || configStr.startsWith("STANDARD:")) {
            base = STANDARD;
            remaining = configStr.substring("STANDARD".length());
        } else if (configStr.equals("DEVELOPMENT") || configStr.startsWith("DEVELOPMENT:")) {
            base = DEVELOPMENT;
            remaining = configStr.substring("DEVELOPMENT".length());
        } else if (configStr.equals("NO_CHECKS") || configStr.startsWith("NO_CHECKS:")) {
            base = NO_CHECKS;
            remaining = configStr.substring("NO_CHECKS".length());
        } else {
            base = new Configuration(Check.NO_CHECK, Check.NO_CHECK, Check.NO_CHECK, false);
            remaining = configStr;
        }

        if (remaining.startsWith(":")) {
            remaining = remaining.substring(1);
        }

        if (remaining.isEmpty()) {
            return base;
        }

        LOG.fine(() -> "parsing custom configuration: " + configStr);
        Pattern pattern = Pattern.compile("((publicApi=(?<publicApi>\\w+))|(privateApi=(?<privateApi>\\w+))|(returnValue=(?<returnValue>\\w+))|(strict=(?<strict>\\w+))|(?<singleCheck>\\w+))(:?|$)");

        Matcher matcher = pattern.matcher(remaining);

        Map<String, Check> checks = new HashMap<>();
        boolean strict = base.strict();
        while (matcher.find()) {
            String strictValue = matcher.group(STRICT);
            if (strictValue != null) {
                strict = Boolean.parseBoolean(strictValue);
            }
            Stream.of("singleCheck", PUBLIC_API, PRIVATE_API, RETURN_VALUE)
                            .forEach(group -> checks.compute(group, (k, v) -> updateCheck(k, v, matcher.group(k))));
        }

        if (checks.isEmpty() && !configStr.contains(STRICT)) {
            throw new IllegalArgumentException("invalid configuration string: '" + configStr + "'");
        }

        Optional.ofNullable(checks.get("singleCheck")).ifPresent(c -> {checks.put(PUBLIC_API, c); checks.put(PRIVATE_API, c); checks.put(RETURN_VALUE, c);});

        return new Configuration(
                checks.getOrDefault(PUBLIC_API, base.publicApi()),
                checks.getOrDefault(PRIVATE_API, base.privateApi()),
                checks.getOrDefault(RETURN_VALUE, base.checkReturn()),
                strict
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
        return "publicApi=" + publicApi.name() + ":privateApi=" + privateApi.name() + ":returnValue=" + checkReturn.name() + ":strict=" + strict;
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
