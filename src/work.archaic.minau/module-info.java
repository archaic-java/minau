/**
 * Minau - A lightweight Java test runner with virtual thread execution.
 *
 * <p>Minau is a test runner that discovers and executes tests implementing the {@code
 * work.archaic.service.test.v01.TestSuite} interface. It provides parallel test execution using
 * Java virtual threads, making it efficient for running large test suites.
 *
 * <h2>Key Features</h2>
 *
 * <ul>
 *   <li>Automatic test discovery through module scanning
 *   <li>Parallel test execution using virtual threads within test classes
 *   <li>Thread-safe result collection and reporting
 *   <li>Structured test output with timing statistics
 *   <li>TestSuite lifecycle management (setup/teardown)
 *   <li>Exit code based on test results (0 for success, 1 for failures)
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <p>Run Minau from the command line:
 *
 * <pre>{@code
 * java -ea -m work.archaic.minau/work.archaic.minau.Main [options] <module>[,<module>...]
 * }</pre>
 *
 * <p><b>Examples:</b>
 *
 * <pre>{@code
 * # Run tests in a single module
 * java -ea -m work.archaic.minau/work.archaic.minau.Main com.example.tests
 *
 * # Run tests in multiple modules with debug information
 * java -ea -m work.archaic.minau/work.archaic.minau.Main --debug com.example.tests,com.example.integration
 *
 * <h2>Writing Tests</h2>
 *
 * <p>Tests must implement the {@code TestSuite} interface from {@code
 * work.archaic.service.catalog}:
 *
 * <pre>{@code
 * package com.example.tests;
 *
 * import work.archaic.service.test.v01.TestSuite;
 * import work.archaic.service.test.v01.Test;
 *
 * public class MyTest implements TestSuite {
 *
 *     @Override
 *     public void setup() {
 *         // Initialize test resources
 *     }
 *
 *     @Test
 *     public void testSomething() {
 *         assert 1 + 1 == 2 : "Math is broken!";
 *     }
 *
 *     @Test
 *     public void testAnotherThing() {
 *         String result = doOperation();
 *         assert "expected".equals(result) : "Unexpected result: " + result;
 *     }
 *
 *     @Override
 *     public void teardown() {
 *         // Clean up test resources
 *     }
 * }
 * }</pre>
 *
 * <h2>Execution Model</h2>
 *
 * <p>Minau executes tests with the following model:
 *
 * <ol>
 *   <li>Test discovery via module scanning
 *   <li>For each test class:
 *       <ol type="a">
 *         <li>Create instance and call {@code setup()}
 *         <li>Execute all {@code @Test} methods in parallel using virtual threads
 *         <li>Collect results from all test threads
 *         <li>Call {@code teardown()}
 *       </ol>
 *   <li>Generate summary report with statistics
 * </ol>
 *
 * <h2>Output Format</h2>
 *
 * <p>The summary report displays comprehensive statistics:
 *
 * <pre>
 * 17:45:29 │ 13.08 - INFO  - work.archaic.minau.Reporter::printSummary
 *          └
 *            Test Results:
 *            ├─ Suites:     2
 *            ├─ Tests:      14
 *            ├─ Passed:     14 (100%)
 *            ├─ Failed:     0
 *            ├─ Duration:   26 ms (avg: 1.9 ms/test)
 *            └─ Test Times: min: 0 ms, max: 1 ms
 * </pre>
 *
 * <h2>Requirements</h2>
 *
 * <ul>
 *   <li>Java 21 or later (uses virtual threads)
 *   <li>Assertions must be enabled ({@code -ea} flag)
 *   <li>Test modules must export packages containing test classes
 * </ul>
 *
 * @since 1.0
 */
module work.archaic.minau {
  requires work.archaic.service.catalog;
}
