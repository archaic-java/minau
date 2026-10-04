package work.archaic.minau;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import work.archaic.service.test.v01.Test;
import work.archaic.service.test.v01.TestSuite;

final class ModuleScanner {
  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  record LegacySuite(Class<?> type, List<TestDescriptor> methods) {}
  record Discovery(List<LegacySuite> legacySuites, List<Class<?>> caseSuites) {}

  static Discovery discoverTests(Collection<String> moduleNames) throws DiscoveryFailure {
    var legacy = new ArrayList<LegacySuite>();
    var cases = new ArrayList<Class<?>>();
    var problems = new ArrayList<Exception>();
    for (var name : moduleNames) {
      int before = legacy.size() + cases.size();
      int failuresBefore = problems.size();
      try {
        var module = ModuleLayer.boot().findModule(name).orElseThrow(() ->
            new IllegalArgumentException("Module is not resolved; add --add-modules " + name));
        var resolved = module.getLayer().configuration().findModule(name).orElseThrow();
        try (var reader = resolved.reference().open(); var resources = reader.list()) {
          var names = resources.filter(resource -> resource.endsWith(".class"))
              .filter(resource -> !resource.equals("module-info.class"))
              .sorted().toList();
          for (var resource : names) {
            String className = resource.substring(0, resource.length() - 6).replace('/', '.');
            try {
              var type = Class.forName(module, className);
              if (type == null) throw new ClassNotFoundException(className);
              inspect(type, legacy, cases);
            } catch (Exception | LinkageError error) {
              problems.add(new Exception(name + ": cannot inspect " + className, error));
            }
          }
        }
        if (legacy.size() + cases.size() == before && problems.size() == failuresBefore)
          problems.add(new Exception(name + ": no concrete test suites found"));
      } catch (Exception | LinkageError error) {
        problems.add(new Exception(name + ": cannot discover module", error));
      }
    }
    if (!problems.isEmpty()) throw new DiscoveryFailure(problems);
    return new Discovery(List.copyOf(legacy), List.copyOf(cases));
  }

  private static void inspect(Class<?> type, List<LegacySuite> legacy, List<Class<?>> cases)
      throws ReflectiveOperationException {
    if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) return;
    boolean v02 = work.archaic.service.test.v02.TestSuite.class.isAssignableFrom(type);
    if (!v02 && !TestSuite.class.isAssignableFrom(type)) return;
    if (!Modifier.isPublic(type.getModifiers()))
      throw new IllegalArgumentException("Test suite must be public");
    Constructor<?> constructor;
    try {
      constructor = type.getConstructor();
    } catch (NoSuchMethodException missing) {
      throw new IllegalArgumentException("Suite needs a public no-argument constructor: " + type.getName(), missing);
    }
    if (!constructor.canAccess(null))
      throw new IllegalAccessException("Suite needs an accessible public no-argument constructor; "
          + "export " + type.getPackageName() + " to work.archaic.minau"
          + (v02 ? "" : " or open the package to work.archaic.minau"));
    if (v02) {
      // Resolve method-signature types too, so missing runtime dependencies cannot hide.
      type.getDeclaredMethods();
      cases.add(type);
      return;
    }
    var methods = new ArrayList<TestDescriptor>();
    for (var method : Arrays.stream(type.getDeclaredMethods())
        .sorted(Comparator.comparing(Method::getName)).toList()) {
      if (!method.isAnnotationPresent(Test.class)) continue;
      if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0
          || method.getReturnType() != void.class)
        throw new IllegalArgumentException("@Test method must be non-static, void and parameterless: "
            + method.getName());
      if (!method.trySetAccessible())
        throw new IllegalAccessException("Cannot access @Test method " + method.getName()
            + "; open " + type.getPackageName() + " to work.archaic.minau");
      methods.add(new TestDescriptor(method.getName(), LOOKUP.unreflect(method)));
    }
    legacy.add(new LegacySuite(type, List.copyOf(methods)));
  }

  static final class DiscoveryFailure extends Exception {
    DiscoveryFailure(List<Exception> problems) {
      super(problems.size() + " discovery error(s)");
      problems.forEach(this::addSuppressed);
    }
  }

  private ModuleScanner() {}
}
