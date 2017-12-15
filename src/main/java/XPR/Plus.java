package XPR;

import com.sun.istack.internal.NotNull;

public class Plus {
  
  private Plus(){};

  public static <ANY> ANY valueOf(Object v){return (ANY)v;}

  public final static class Help { private Help(){}

    public interface Locator {
      @NotNull String locateHelpFor(String path);
    }

  }

  // java language patch for the e-quals
  public final static boolean sameClass(Class a, Class... b) {
    for (Class c: b) {
      if (a.equals(c)) return true;
    }
    return false;
  }

  // java language patch for the array
  public final static boolean dimensionalValue(Object data) {
    return data.getClass().isArray();
  }

  // java language patch for the array
  public final static boolean dimensionalValueClass(Class data) {
    return data.isArray();
  }

}
