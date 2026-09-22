/**
 * A JDK-first test runner that discovers suites in selected modules and runs tests on virtual
 * threads. Supports the catalog's v01 annotated suites and v02 explicitly registered cases.
 * v02 cases receive an independent bounded failure trail and need no reflective access.
 *
 * <p>Run with assertions enabled and resolve the test modules on the module path. See the
 * repository README for discovery, package visibility, lifecycle and CLI details.
 */
module work.archaic.minau {
  requires work.archaic.service.catalog;
}
