package com.dua3.cabe.processor;

import java.io.Serializable;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Config(Check publicApi, Check privateApi) implements Serializable {
    private static final Logger LOG = Logger.getLogger(Config.class.getName());

    public static Config parseConfigString(String configStr) {
        switch (configStr) {
            case "standard" -> {return StandardConfig.STANDARD.config;}
            case "development" -> {return StandardConfig.DEVELOPMENT.config;}
            case "no-checks" -> {return StandardConfig.NO_CHECKS.config;}
        };

        LOG.fine(() -> "parsing custom configuration: " + configStr);
        Pattern pattern = Pattern.compile("(?<singleParam>\\w+)|(publicApi=(?<publicParam>\\w+):privateApi=(?<privateParam>\\w+))");
        Matcher matcher = pattern.matcher(configStr);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("invalid configuration string: '" + configStr + "'");
        }

        String singleParam = matcher.group("singleParam");
        if (singleParam != null) {
            Check c = Check.valueOf(singleParam);
            return new Config(c, c);
        }

        String publicParam = matcher.group("publicParam");
        String privateParam = matcher.group("privateParam");
        if (publicParam != null && privateParam != null) {
            return new Config(Check.valueOf(publicParam), Check.valueOf(privateParam));
        }

        throw new IllegalStateException("could not parse configuration string: '" + configStr + "'");
    }

    public String getConfigString() {
        return "publicApi=" + publicApi.name() + ":privateApi=" +privateApi.name();
    }

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

    public enum StandardConfig {
        DEVELOPMENT(new Config(Check.ASSERT_ALWAYS, Check.ASSERT_ALWAYS)),
        STANDARD(new Config(Check.THROW_NPE, Check.ASSERT)),
        NO_CHECKS(new Config(Check.NO_CHECK, Check.NO_CHECK));

        StandardConfig(Config config) {
            this.config = config;
        }

        private static final long serialVersionUID = 1L;

        public final Config config;
    }
}
