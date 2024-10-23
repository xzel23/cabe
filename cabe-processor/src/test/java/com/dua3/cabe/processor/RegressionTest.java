package com.dua3.cabe.processor;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegressionTest {
    private static final Logger LOG = Logger.getLogger(RegressionTest.class.getName());

    private static Stream<Arguments> fetchTests() throws IOException {
        return Files.list(TestUtil.resourceDir.resolve("regression"))
                .filter(Files::isDirectory)
                .map(dir -> Arguments.of(dir.getFileName().toString(), dir));
    }

    @ParameterizedTest(name = "regression: {0}")
    @MethodSource("fetchTests")
    public void runTest(String testName, Path testDir) throws IOException, ClassFileProcessingFailedException, InterruptedException {

        System.out.println("Running regression test: " + testName);

        // create directories
        Path root = TestUtil.buildDir.resolve("regression").resolve(testName);
        Path srcDir = root.resolve("src");
        Path unprocessedDir = root.resolve("classes-unprocessed-regression-" + testName);
        Path processedDir = root.resolve("classes-processed-regression-" + testName);

        // copy sources
        LOG.info("copying test sources ...");
        TestUtil.copyRecursive(testDir.resolve("src"), srcDir);

        // compile source files to classes-unprocessed folder
        LOG.info("compiling test sources ...");
        TestUtil.compileSources(srcDir, unprocessedDir, TestUtil.resourceDir.resolve("testLib"));

        // instrument classes
        LOG.info("instrumenting classes ...");
        TestUtil.processClasses(unprocessedDir, processedDir, Configuration.StandardConfig.STANDARD.config());

        // run test
        LOG.info("running test ...");
        String result = TestUtil.runClass(processedDir, testName, true);

        assertEquals("OK", result.strip());
    }
}
