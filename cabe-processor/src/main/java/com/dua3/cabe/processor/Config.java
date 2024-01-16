package com.dua3.cabe.processor;

import java.io.Serializable;

public record Config(Check publicApi, Check privateApi) implements Serializable {
    public enum Check {
        ASSERTION,
        THROW_NPE,
        IGNORE
    }

    public enum StandardConfig {
        DEVELOPMENT(new Config(Check.ASSERTION, Check.ASSERTION)),
        RELEASE(new Config(Check.THROW_NPE, Check.IGNORE)),
        NO_CHECKS(new Config(Check.IGNORE, Check.IGNORE));

        StandardConfig(Config config) {
            this.config = config;
        }

        private static final long serialVersionUID = 1L;

        public final Config config;
    }
}
