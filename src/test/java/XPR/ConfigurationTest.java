package XPR;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class ConfigurationTest {
  Configuration configuration;
  Configuration.Parameter activation = new Configuration.Parameter(
    "activation", trigger -> trigger.equals("--activation")
  );

  Configuration.Parameter file = new Configuration.Parameter(
    "my.super.file", trigger -> trigger.equals("--file") || trigger.equals("-f"),
    Configuration.Parameter.ValueType.MANDATORY
  );

  @Before
  public void setUp() throws Exception {
    configuration = new Configuration("test",
      new Configuration.Director(true) {
        HashMap<String, Object> database = new HashMap<>();
        @Override
        public boolean set(Configuration main,
          Configuration.Parameter parameter,
          int parameterIndex, Object value)
        {
          String path = main.getParameterPath(parameter);
          if (parameter.equals(activation)) {
            database.put(path, true);
            return true;
          } else if (parameter.equals(file)) {
            database.put(path, value);
            return true;
          }
          return false;
        }

        @Override
        public int parse(Configuration main, int index, String parameter,
          String value)
        {
          return 0;
        }

        @Override
        public Object get(String path) {
          return database.get(path);
        }

        @Override
        public String locateHelpFor(String path) {
          return null;
        }

        @Override
        public String toJSON() {
          return new XPR.JSON.Type.Variant(database).toString();
        }
        @Override
        protected void onLoad(XPR.JSON.Type.Variant storage) {
          database.putAll(storage.toMap());
        }
      },
      new Configuration.Category("main", activation, file)
    );
    assertEquals(3, configuration.configure("--activation", "--file", "/dev/stdin"));
  }

  @Test
  public void findParameterByTrigger() throws Exception {
    assertEquals(activation, configuration.findParameterByTrigger("--activation"));
  }

  @Test
  public void findParameterByName() throws Exception {
    assertEquals(file, configuration.findParameterByName("test.main.my.super.file"));
  }

  @Test
  public void getName() throws Exception {
    assertEquals("test", configuration.getName());
  }

  @Test
  public void getParameterPath() throws Exception {
    assertEquals("test.main.activation", configuration.getParameterPath(activation));
  }

  @Test
  public void get_activation() throws Exception {
    assertEquals(true, configuration.get("test.main.activation"));
  }

  @Test
  public void get_file() throws Exception {
    assertEquals("/dev/stdin", configuration.get("test.main.my.super.file"));
  }

  @Test public void parameter_value_fail() throws Exception {
    try {
      configuration.configure("--file", "--activation", "/dev/stdin");
    } catch (Fault f) {
      System.err.print("FAULT-EXAMPLE: NOT-A-REAL-FAULT: ");
      f.printStackTrace();
      assertEquals(0, f.getFaultCode());
    }
  }

  @Test
  public void toJSON() throws Exception {
    try {
      System.err.println(configuration.toJSON());
    } catch (Exception e) {
      e.printStackTrace();
      assertEquals(false, true);
    }
  }
}