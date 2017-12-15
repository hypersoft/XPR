package XPR;

import com.sun.istack.internal.NotNull;

import java.util.Set;

public class Plus {
  
  private Plus(){};

  // java language patch for the array
  static public <ANY> ANY[] getDimensionalValueOf(Object data) {
    if (data instanceof Set) {
      return valueOf(((Set)data).toArray());
    }
    throw new Fault("no solution for coercion of dimensional data type", new UnsupportedOperationException());
  }
  public static <ANY> ANY valueOf(Object value){
    return (ANY) value;
  }

  public final static class Help { private Help(){}

    public interface Locator {
      @NotNull String locateHelpFor(String path);
    }

  }

  // java language patch for the e-quals
  public final static boolean sameClass(@NotNull Class a, Class... b) {
    for (Class c: b) {
      if (a.equals(c)) return true;
    }
    return false;
  }

  // java language patch for the array
  public final static boolean dimensionalValue(@NotNull Object data) {
    return data.getClass().isArray();
  }

  // java language patch for the array
  public final static boolean dimensionalValueClass(@NotNull Class data) {
    return data.isArray();
  }

}
