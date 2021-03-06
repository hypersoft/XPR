package XPR;

/*

  XPR Language:

  to err is human, so is an error [or = suffix]
  exception = fault [except = "all but for CAUSE[=ion = contract = false; no-contract = fact]", fault = conditional-break]
  object = value, variant, compound or data[-structure] [ob = modify, ject = throw: ambiguous: lacking-clarity with the verbs as the noun, 2 modifications = 2 verbs as the one noun]
  array = type+s (as in bytes), elements, list (is plural), element-list (plural-container) or basic-list (generic, or simple: elementary) but not List (a compound-data-type because java claims the type)
  explicit = certain, exact
  implicit, inferred = automatic, direct
  extends = morphs
  property = $[object], structure-member, compound-element, [logic: value of the/this]...
  implements = modeling (the noun) or modelings (the plural noun)
  inherits = shares (the value of THING; there is no "inherits-from" but there is a shares-with in coding)
  implement = forge, construct, build, or frame [a system]
  process, application = real-time-operating-methods (applied-science), system, software
  project = plan, system, library, [name-](software-stack or framework)
  procedure = method
  enumeration = codex
    [there are two-ways to say: e-numeration = (1) and en-umeration = (2);]
    [en = french: wrong-language;]
    [the correct translation word for "en" is "in": in+word = not-word = wrong-logic]
    [co = sharing, dex = table; correct-logic]
  interface = portifice ((standard-contrivance = class)-portal/gateway) class-port
  instance = class-member or same as $[object]
  import/include = (add-source, load-source, read-source, compile-source)-doings: specifically all of those things.

  This codex = translation-table is not to be considered complete.

  It is not feasible to change all of the words in the java language. So,
  this table is for your knowledge of the facts. We know the facts, and can
  use any alias we like, but coding with the knowledge is what makes the
  knowledge work.

  Verbs as nouns, and nouns as verbs are not considered correct-grammar, but
  will work for correct code due to the difference in method = verb/noun.
  Every verb has a noun = name, but no nouns are verbs in standard [A]english speech.

  Say you have a friend by the name of Triston. One doesn't go about
  "Tristoning" [notice the gerund: ing] people. Triston is not a verb. That's a
  simple-verb-smell-test. Its good exercise for every verb to end with -ing:
   Operating-Methods[while: could be now, then or when] vs. Operations-Methods
   [in the moment, a dicey-speculation]

*/

import com.sun.istack.internal.NotNull;

import java.util.Set;

public class Plus { private Plus(){};

  /**
   * Compare the class of a value with a class or list of classes for likeness.
   *
   * java-language-correction for instanceof and isAssignableFrom
   *
   * @param value a java value
   * @param kind a java class, java classes or a parameter-list of java classes to test
   * @return true when the value is a member of any one of the class kind(s).
   */
  static public boolean classMember(@NotNull Object value, @NotNull Class... kind) {
    Class<?> valueType = value.getClass();
    for (Class family: kind) {
      if (valueType.isAssignableFrom(family)) return true;
    }
    return false;
  }

  /**
   * Convenience feature to convert compound element types to basic-list.
   * @param data a supported java value.
   * @param <ANY> Automatic Type Cast.
   * @return pass-through-value-casting to basic list if supported. If there is
   * no support for the pass-through, a fault is flagged on the operation.
   */
  static public <ANY> ANY[] getBasicListOf(Object data) {
    if (classMember(data, Set.class)) {
      return valueOf(((Set)data).toArray());
    }
    throw new Fault("no solution known for pass-through of compound data type"
      +Speak.quoteExactTarget(data.getClass().getSimpleName())
      +" as a basic list", new UnsupportedOperationException()
    );
  }

  /**
   * For casting the value to a known-type.
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
   * @param b a java class, java classes or a parameter-list of classes to test
   * @return true if class a is found within class[] b or is b.
   */
  public final static boolean classMatch(@NotNull Class a, Class... b) {
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

  public static <T> T valueNotNull(T value) {
    if (value == null)
      throw new Fault(new NullPointerException());
    return value;
  }

  public static <T> T valueNotNull(T value, T fallback) {
    if (value == null) return fallback;
    return value;
  }

}
