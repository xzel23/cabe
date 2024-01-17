package com.dua3.cabe.processor;

import java.io.Serializable;

public record Config(Check publicApi, Check privateApi) implements Serializable {
    public enum Check {
        /**
         * Do not generate Checks.
         */
        IGNORE,
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
        NO_CHECKS(new Config(Check.IGNORE, Check.IGNORE));

        StandardConfig(Config config) {
            this.config = config;
        }

        private static final long serialVersionUID = 1L;

        public final Config config;
    }
}
