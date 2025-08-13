module com.example.foo.test {
  requires com.example.foo;
  requires work.archaic.service.catalog;

  opens com.example.foo.test to
      work.archaic.minau;
}
