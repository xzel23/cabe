package com.dua3.cabe.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ConfigurationTest {
    
    record ConfigurationTestData(String configString, Configuration expected) {}

    static Stream<ConfigurationTestData> configurationTestData() {
        return Stream.of(
                // default configurations
                new ConfigurationTestData("STANDARD",      Configuration.STANDARD),
                new ConfigurationTestData("DEVELOPMENT",   Configuration.DEVELOPMENT),
                new ConfigurationTestData("NO_CHECKS",     Configuration.NO_CHECKS),

                // single check configurations
                new ConfigurationTestData("ASSERT",        new Configuration(Configuration.Check.ASSERT, Configuration.Check.ASSERT, Configuration.Check.ASSERT, true)),
                new ConfigurationTestData("ASSERT_ALWAYS", new Configuration(Configuration.Check.ASSERT_ALWAYS, Configuration.Check.ASSERT_ALWAYS, Configuration.Check.ASSERT_ALWAYS, true)),
                new ConfigurationTestData("THROW_NPE",     new Configuration(Configuration.Check.THROW_NPE, Configuration.Check.THROW_NPE, Configuration.Check.THROW_NPE, true)),
                new ConfigurationTestData("NO_CHECK",      new Configuration(Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK, true)),

                // single API check configuration
                new ConfigurationTestData("publicApi=THROW_NPE",       new Configuration(Configuration.Check.THROW_NPE, Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK, true)),
                new ConfigurationTestData("privateApi=ASSERT",         new Configuration(Configuration.Check.NO_CHECK, Configuration.Check.ASSERT, Configuration.Check.NO_CHECK, true)),
                new ConfigurationTestData("returnValue=ASSERT_ALWAYS", new Configuration(Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK, Configuration.Check.ASSERT_ALWAYS, true)),

                // multiple API check configuration
                new ConfigurationTestData("publicApi=THROW_NPE:privateApi=ASSERT", new Configuration(Configuration.Check.THROW_NPE, Configuration.Check.ASSERT, Configuration.Check.NO_CHECK, true)),
                new ConfigurationTestData("publicApi=THROW_NPE:privateApi=ASSERT:returnValue=ASSERT_ALWAYS", new Configuration(Configuration.Check.THROW_NPE, Configuration.Check.ASSERT, Configuration.Check.ASSERT_ALWAYS, true)),

                // strict mode configuration
                new ConfigurationTestData("strict=true", new Configuration(Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK, true)),
                new ConfigurationTestData("STANDARD:strict=true", new Configuration(Configuration.Check.THROW_NPE, Configuration.Check.ASSERT, Configuration.Check.NO_CHECK, true)),
                new ConfigurationTestData("publicApi=THROW_NPE:strict=true", new Configuration(Configuration.Check.THROW_NPE, Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK, true)),
                new ConfigurationTestData("publicApi=THROW_NPE:privateApi=ASSERT:returnValue=ASSERT_ALWAYS:strict=true", new Configuration(Configuration.Check.THROW_NPE, Configuration.Check.ASSERT, Configuration.Check.ASSERT_ALWAYS, true)),

                // strict=false tests
                new ConfigurationTestData("strict=false", new Configuration(Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK, false)),
                new ConfigurationTestData("STANDARD:strict=false", new Configuration(Configuration.Check.THROW_NPE, Configuration.Check.ASSERT, Configuration.Check.NO_CHECK, false)),
                new ConfigurationTestData("ASSERT:strict=false", new Configuration(Configuration.Check.ASSERT, Configuration.Check.ASSERT, Configuration.Check.ASSERT, false))
        );
    }

    @ParameterizedTest
    @MethodSource("configurationTestData")
    void testParse(ConfigurationTestData t) {
        Configuration expected = t.expected();
        Configuration actual = Configuration.parse(t.configString());
        assertEquals(expected, actual);
    }

    record ConfigurationTestInput(String s, Configuration.Check publicApi, Configuration.Check privateApi, Configuration.Check returnValue) {}


    @Test
    void testEqualsAndHashCode() {
        Configuration config1 = new Configuration(Configuration.Check.ASSERT, Configuration.Check.THROW_NPE, Configuration.Check.NO_CHECK, true);
        Configuration config2 = new Configuration(Configuration.Check.ASSERT, Configuration.Check.THROW_NPE, Configuration.Check.NO_CHECK, true);
        Configuration config3 = new Configuration(Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK, false);

        assertEquals(config1, config2);
        assertNotEquals(config1, config3);

        assertEquals(config1.hashCode(), config2.hashCode());
        assertNotEquals(config1.hashCode(), config3.hashCode());

        assertNotEquals(Configuration.STANDARD, config1);
        assertEquals(Configuration.NO_CHECKS, config3);
    }
}
