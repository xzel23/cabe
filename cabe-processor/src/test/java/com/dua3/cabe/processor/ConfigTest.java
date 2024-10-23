package com.dua3.cabe.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ConfigTest {

    @ParameterizedTest
    @EnumSource(Configuration.StandardConfig.class)
    public void testConfigString(Configuration.StandardConfig sc) {
        Configuration c = sc.config();

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
        Configuration.Check c1 = Configuration.Check.valueOf(s1);
        Configuration.Check c2 = Configuration.Check.valueOf(s2);
        Configuration expected = new Configuration(c1, c2);

        Configuration actual = Configuration.parseConfigString("publicApi=%s:privateApi=%s".formatted(s1, s2));

        assertEquals(expected, actual);
    }

    @Test
    public void testEqualsAndHashCode() {
        Configuration config1 = new Configuration(Configuration.Check.ASSERT, Configuration.Check.THROW_NPE);
        Configuration config2 = new Configuration(Configuration.Check.ASSERT, Configuration.Check.THROW_NPE);
        Configuration config3 = new Configuration(Configuration.Check.NO_CHECK, Configuration.Check.NO_CHECK);

        assertEquals(config1, config2);
        assertNotEquals(config1, config3);

        assertEquals(config1.hashCode(), config2.hashCode());
        assertNotEquals(config1.hashCode(), config3.hashCode());

        assertNotEquals(config1, Configuration.StandardConfig.STANDARD.config());
        assertEquals(config3, Configuration.StandardConfig.NO_CHECKS.config());
    }
}