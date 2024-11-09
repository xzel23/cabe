package com.dua3.cabe.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ConfigurationTest {

    @ParameterizedTest
    @EnumSource(Configuration.StandardConfig.class)
    public void testConfigString(Configuration.StandardConfig sc) {
        Configuration c = sc.config();

        String expected = switch (sc) {
            case STANDARD -> "publicApi=THROW_NPE:privateApi=ASSERT:returnValue=NO_CHECK";
            case NO_CHECK -> "publicApi=NO_CHECK:privateApi=NO_CHECK:returnValue=NO_CHECK";
            case DEVELOPMENT -> "publicApi=ASSERT_ALWAYS:privateApi=ASSERT_ALWAYS:returnValue=ASSERT_ALWAYS";
        };

        String actual = c.getConfigString();

        assertEquals(expected, actual);
    }

    record ConfigurationTestData(String configString, Configuration expected) {}

    static Stream<ConfigurationTestData> configurationTestData() {
        return Stream.of(
                // default configurations
                new ConfigurationTestData("standard",      Configuration.StandardConfig.STANDARD.config()),
                new ConfigurationTestData("development",   Configuration.StandardConfig.DEVELOPMENT.config()),
                new ConfigurationTestData("no-check",      Configuration.StandardConfig.NO_CHECK.config()),

                // single check configurations
                new ConfigurationTestData("ASSERT",        new Configuration(Configuration.Check.ASSERT, Configuration.Check.ASSERT, Configuration.Check.ASSERT)),
                new ConfigurationTestData("ASSERT_ALWAYS", new Configuration(Configuration.Check.ASSERT_ALWAYS, Configuration.Check.ASSERT_ALWAYS, Configuration.Check.ASSERT_ALWAYS)),
                new ConfigurationTestData("THROW_NPE",     new Configuration(Configuration.Check.THROW_NPE, Configuration.Check.THROW_NPE, Configuration.Check.THROW_NPE)),
                new ConfigurationTestData("NO_CHECK",      new Configuration(Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK)),

                // single API check configuration
                new ConfigurationTestData("publicApi=THROW_NPE",       new Configuration(Configuration.Check.THROW_NPE, Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK)),
                new ConfigurationTestData("privateApi=ASSERT",         new Configuration(Configuration.Check.NO_CHECK, Configuration.Check.ASSERT, Configuration.Check.NO_CHECK)),
                new ConfigurationTestData("returnValue=ASSERT_ALWAYS", new Configuration(Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK, Configuration.Check.ASSERT_ALWAYS)),

                // multiple API check configuration
                new ConfigurationTestData("publicApi=THROW_NPE:privateApi=ASSERT", new Configuration(Configuration.Check.THROW_NPE, Configuration.Check.ASSERT, Configuration.Check.NO_CHECK)),
                new ConfigurationTestData("publicApi=THROW_NPE:privateApi=ASSERT:returnValue=ASSERT_ALWAYS", new Configuration(Configuration.Check.THROW_NPE, Configuration.Check.ASSERT, Configuration.Check.ASSERT_ALWAYS))
        );
    }

    @ParameterizedTest
    @MethodSource("configurationTestData")
    public void testParseConfigString(ConfigurationTestData t) {
        Configuration expected = t.expected();
        Configuration actual = Configuration.parseConfigString(t.configString());
        assertEquals(expected, actual);
    }

    record ConfigurationTestInput(String s, Configuration.Check publicApi, Configuration.Check privateApi, Configuration.Check returnValue) {}


    @Test
    public void testEqualsAndHashCode() {
        Configuration config1 = new Configuration(Configuration.Check.ASSERT, Configuration.Check.THROW_NPE, Configuration.Check.NO_CHECK);
        Configuration config2 = new Configuration(Configuration.Check.ASSERT, Configuration.Check.THROW_NPE, Configuration.Check.NO_CHECK);
        Configuration config3 = new Configuration(Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK);

        assertEquals(config1, config2);
        assertNotEquals(config1, config3);

        assertEquals(config1.hashCode(), config2.hashCode());
        assertNotEquals(config1.hashCode(), config3.hashCode());

        assertNotEquals(config1, Configuration.StandardConfig.STANDARD.config());
        assertEquals(config3, Configuration.StandardConfig.NO_CHECK.config());
    }
}