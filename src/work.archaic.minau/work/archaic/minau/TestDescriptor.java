package work.archaic.minau;

import java.lang.invoke.MethodHandle;

final class TestDescriptor {
  final Class<?> testClass;
  final String methodName;
  final MethodHandle methodHandle;

  TestDescriptor(Class<?> testClass, String methodName, MethodHandle methodHandle) {
    this.testClass = testClass;
    this.methodName = methodName;
    this.methodHandle = methodHandle;
  }
}
