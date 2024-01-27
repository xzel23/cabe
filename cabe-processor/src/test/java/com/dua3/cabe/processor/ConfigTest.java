package com.dua3.cabe.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ConfigTest {

    @ParameterizedTest
    @EnumSource(Config.StandardConfig.class)
    public void testConfigString(Config.StandardConfig sc) {
        Config c = sc.config;

        String expected = switch (sc) {
            case STANDARD -> "publicApi=THROW_NPE:privateApi=ASSERT";
            case NO_CHECKS -> "publicApi=NO_CHECK:privateApi=NO_CHECK";
            case DEVELOPMENT -> "publicApi=ASSERT_ALWAYS:privateApi=ASSERT_ALWAYS";
        };

        String actual = c.getConfigString();

        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @CsvSource({
            "NO_CHECK, NO_CHECK",
            "NO_CHECK, ASSERT",
            "NO_CHECK, ASSERT_ALWAYS",
            "NO_CHECK, THROW_NPE",
            "ASSERT, NO_CHECK",
            "ASSERT, ASSERT",
            "ASSERT, ASSERT_ALWAYS",
            "ASSERT, THROW_NPE",
            "ASSERT_ALWAYS, NO_CHECK",
            "ASSERT_ALWAYS, ASSERT",
            "ASSERT_ALWAYS, ASSERT_ALWAYS",
            "ASSERT_ALWAYS, THROW_NPE",
            "THROW_NPE, NO_CHECK",
            "THROW_NPE, ASSERT",
            "THROW_NPE, ASSERT_ALWAYS",
            "THROW_NPE, THROW_NPE"
    })
    public void testParseConfigString(String s1, String s2) {
        Config.Check c1 = Config.Check.valueOf(s1);
        Config.Check c2 = Config.Check.valueOf(s2);
        Config expected = new Config(c1, c2);

        Config actual = Config.parseConfigString("publicApi=%s:privateApi=%s".formatted(s1, s2));

        assertEquals(expected, actual);
    }

    @Test
    public void testEqualsAndHashCode() {
        Config config1 = new Config(Config.Check.ASSERT, Config.Check.THROW_NPE);
        Config config2 = new Config(Config.Check.ASSERT, Config.Check.THROW_NPE);
        Config config3 = new Config(Config.Check.NO_CHECK, Config.Check.NO_CHECK);

        assertEquals(config1, config2);
        assertNotEquals(config1, config3);

        assertEquals(config1.hashCode(), config2.hashCode());
        assertNotEquals(config1.hashCode(), config3.hashCode());

        assertNotEquals(config1, Config.StandardConfig.STANDARD.config);
        assertEquals(config3, Config.StandardConfig.NO_CHECKS.config);
    }
}