package XPR;

/*

  XPR Language:

  object = value, variant, compound or data
  array = elements, list, element-list or basic-list but not List (because java claims the type)
  explicit = certain
  extends = mocks (the verb) or mocking (the noun)
  implements = models (the verb) or modeling (the noun) or modelings (the plural noun)
  implement = model (the verb) or modeling (the noun)
  enumeration = codex [there are two-ways to say: e-numeration (1) en-umeration (2); en = french]

  This codex = translation-table is not to be considered complete.

*/

import com.sun.istack.internal.NotNull;

import java.util.Set;

public class Plus {
  
  private Plus(){};

  /**
   * Convenience feature to convert compound element types to basic-list.
   * @param data a supported java value.
   * @param <ANY> Automatic Type Cast.
   * @return pass-through-value-casting to basic list if supported. If there is
   * no support for the pass-through, a fault is flagged on the operation.
   */
  static public <ANY> ANY[] getBasicListOf(Object data) {
    if (data instanceof Set) {
      return valueOf(((Set)data).toArray());
    }
    throw new Fault("no solution for coercion of dimensional data type", new UnsupportedOperationException());
  }


  /**
   * For casting the data value to a known-type.
   * @param data a java value
   * @param <ANY> Automatic Type Cast.
   * @return The value being written to a variable of a certain type.
   */
  public static <ANY> ANY valueOf(Object data){
    return (ANY) data;
  }

  public final static class Help { private Help(){}

    public interface Locator {
      @NotNull String locateHelpFor(String path);
    }

  }

  /**
   * Search for a java class in a list of java classes.
   * @param a the class being sought
   * @param b a class[], parameter list of classes or a single-class.
   * @return true if class a is found within class[] b or is b.
   */
  public final static boolean sameClass(@NotNull Class a, Class... b) {
    for (Class c: b) {
      if (a.equals(c)) return true;
    }
    return false;
  }

  /**
   * Basic List Test
   * @param data the test candidate
   * @return true if the data value is a value[]
   */
  public final static boolean basicListValue(@NotNull Object data) {
    return data.getClass().isArray();
  }

  /**
   * Basic List Class Test
   * @param data the test candidate
   * @return true if the class is a value[]
   */
  public final static boolean basicListClass(@NotNull Class data) {
    return data.isArray();
  }

}
