# Minau
A simple test runner where each test has its own virtual thread.

## Provider
Minau provides an implementation for the test related interfaces defined in
work.archaic.service.catalog. Application modules declare their reliance on the
service catalog, not on Minau explicitly.

## Logging Concept
Minau uses logs instead of printing to STDOUT to make sure the output is
consistent.

### Logs inside Minau
We have conventions for using the following log levels of SLF4J.

INFO: Generic status information. For example that we started a certain test or
that it ended successfully. In general things you would otherwise just print to
STDOUT.

DEBUG: Information typically not shown until one starts to debug Minau itself.

WARNING: You want to make obvious that Minau used some kind of fallback to
recover from an error condition while running tests.

ERROR: Details on an error condition Minau, or parts of it could not recover
from.

### Logs inside of tests
Actually, it should not be needed to log something inside a test. Minau takes
care of logging the details of running a test.

## User Manual
Users of Minau for testing just make sure Minau`s main module is on the module
path. How to write tests et cetera is documented in the javadoc.

