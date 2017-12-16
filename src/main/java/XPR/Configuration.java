package XPR;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;


/**
 * This class and it's members provide a way to set application configuration
 * settings across a wide variety of setting schemes, with JSON serialization
 * support.
 */
public class Configuration {

  public static class Category {
    private final String name;
    private final Parameter[] parameter;
    public Category(String name, Parameter[] parameters) {
      this.name = name;
      parameter = parameters;
    }
    @Nullable
    public Parameter findParameterByName(@NotNull String name) {
      for (Parameter p: parameter) {
        if (p.name.equals(name)) return p;
      }
      return null;
    }
    @Nullable
    public Parameter findParameterByOption(@NotNull String option) {
      for (Parameter p: parameter) {
        if (p.selector.match(option)) return p;
      }
      return null;
    }
    boolean hasParameter(Parameter query) {
      for (int i = 0; i < parameter.length; i++) {
        if (parameter[i].equals(query)) return true;
      }
      return false;
    }
  }

  public static class Parameter {
    public enum ValueType {NONE, OPTIONAL, MANDATORY}
    public interface Selector<TYPE> {boolean match(TYPE trigger);}
    public final String name;
    public final Selector selector;
    public final ValueType value;
    public Parameter(@NotNull String name, @NotNull Selector matcher, @NotNull ValueType type) {
      this.name = name;
      this.selector = matcher;
      value = type;
    }
    public Parameter(String name, Selector matcher) {
      this.name = name;
      this.selector = matcher;
      value = ValueType.NONE;
    }
  }

  public static interface Supervisor {

    /**
     * Method for options configuration.
     * @param main the options object which contains the parameter.
     * @param parameter the parameter object which defines the parameter.
     * @param parameterIndex the index within the parameters being parsed.
     * @param value the value for the parameter or null if not specified or found
     * @return false to stop further options handling; true to continue.
     */
    boolean set(@NotNull Configuration main, @NotNull Parameter parameter,
      int parameterIndex, @Nullable Object value);

    /**
     * Fallback method for options that don't parse using automation.
     * @param main
     * @param parameter
     * @param value
     * @return 0 if you can't handle the request, 1 if you want the
     * parameter, 2 if you want the parameter and value. Returning a value of 0
     * will stop all options processing and cause a fault to be raised specifying
     * what went wrong.
     */
    int parse(Configuration main, int index, String parameter, String value);

    /**
     * @param path
     * @return the stored or default option value. You cannot return null from this method.
     * if you need to return null, consider using a fallback value other than null.
     */
    @NotNull Object get(String path);

  }


  /**
   * XPR.Configuration.Director<br>
   *   <br>
   *     Create a new XPR.Configuration.Director(true) {...}
   *     further adding methods onLoad(JSON.Type.Variant) and toJSON()
   *     for JSON Serialization support with XPR.Configuration.Director.
   */
  public static abstract class Director implements Supervisor, Plus.Help.Locator, JSON.Serialization {
    private static String directorFault = "this configuration director does not support serialization";
    protected boolean serializable = false;
    public boolean serializable() { return serializable; }
    protected Director(boolean serializable){this.serializable = serializable;}
    protected void onLoad(@NotNull JSON.Type.Variant storage) {
      throw new Fault(directorFault, new UnsupportedOperationException());
    }
    public String toJSON() {
      throw new Fault(directorFault, new UnsupportedOperationException());
    }
  };

  public final String name;
  private final Category[] category;
  private final Director director;

  public Configuration(@NotNull String name, @NotNull Category[] categories, @NotNull Director director) {
    this.name = name;  category = categories; this.director = director;
  }

  @NotNull public final Category getParameterCategory(Parameter p) {
    for (Category c: category) {
      if (c.hasParameter(p)) return c;
    }
    throw new Fault("parameter category lookup failure"
      + Speak.quoteExactTarget(p.name)
    );
  }

  @Nullable final public Category findCategoryByName(String search) {
    if (search.contains(".")) {
      search = search.split("\\.")[1];
    }
    for (Category c: category) {
      if (c.name.equals(search)) return c;
    }
    return null;
  }

  @Nullable final public Parameter findParameterByName(String search) {
    if (search.contains(".")) {
      search = search.split("\\.")[2];
    }
    if (search == null) return null;
    int x; Parameter parameter = null;
    for (x=0; x< category.length; x++) {
      if ((parameter = category[x].findParameterByName(search)) != null) break;
    }
    return parameter;
  }

  @Nullable public final Parameter findParameterByTrigger(String search) {
    if (search == null) return null;
    int x; Parameter parameter = null;
    for (x=0; x< category.length; x++) {
      if ((parameter = category[x].findParameterByOption(search)) != null) break;
    }
    return parameter;
  }

  @NotNull final public String getName() {
    return name;
  }

  @NotNull public String buildParameterPath(@NotNull Category category, @NotNull Parameter parameter) {
    return this.name+"."+category.name+"."+parameter.name;
  }

  @NotNull public String getParameterPath(Parameter parameter) {
    return buildParameterPath(getParameterCategory(parameter), parameter);
  }

  @Nullable private String nextValue(@NotNull String[] parameters, int index) {
    ++index;
    if (index < parameters.length) return parameters[index];
    return null;
  }

  /**
   *
   * @param parameters
   * @return The number of parameters consumed by this option set.
   */
  public int configure(@NotNull String... parameters) {
    int i;
    for (i = 0; i < parameters.length; i++) {

      String parameter = parameters[i];
      Parameter p = findParameterByTrigger(parameter);

      if (p == null) {
        String value = nextValue(parameters, i);
        if (findParameterByTrigger(value) != null) value = null;
        switch (director.parse(this, i, parameter, value))
        {
          case 1: continue;
          case 2: i++; continue;
          default: return i;
        }
      }

      String value = (! p.value.equals(Parameter.ValueType.NONE))
        ? nextValue(parameters, i) : null
      ;

      if (findParameterByTrigger(value) != null) value = null;

      if (value == null && p.value.equals(Parameter.ValueType.MANDATORY)) {
        throw new Fault("missing parameter value for parameter"
         + Speak.quoteCitation(i+1)
         + Speak.quoteExactTarget(parameter)
        );
      }

      int adjust = (value == null)?0:1;

      if (director.set(this, p, i+1, value)) {
        i += adjust; continue;
      } else break;

    }
    return i;
  }

  final public void set(String path) {
    set(path, null);
  }

  final public void set(String path, Object value) {
    Parameter p = findParameterByName(path);
    if (p == null) {
      throw new Fault("set failure" + Speak.quoteAnd("the configuration path")
        + Speak.quoteExactTarget(path) + " was not found within configuration"
        + Speak.quoteExactTarget(name),
        new java.lang.ReflectiveOperationException()
      );
    } else
    if (p.value.equals(Parameter.ValueType.NONE) && value != null)
      throw new Fault("set failure"
        + Speak.quoteAnd("the configuration path")
        + Speak.quoteExactTarget(path) + " does not handle values"
        + Speak.quoteAnd("try modifying this setting with no value"),
        new IllegalAccessException()
      );
    else
    if (p.value.equals(Parameter.ValueType.MANDATORY) && value == null)
      throw new Fault("set failure"
        + Speak.quoteAnd("the configuration path")
        + Speak.quoteExactTarget(path) + " specifies a mandatory value"
        + Speak.quoteAnd("try modifying this setting with a value"),
        new NullPointerException()
      );
    director.set(this, p, 0, value);
  }

  @NotNull final public <ANY> ANY get(String path) {
    return (ANY) director.get(path);
  }

  @NotNull final public String getHelp(String path) {return director.locateHelpFor(path);}

  final public boolean serializable() {
    return director.serializable();
  }

  final public String toJSON() {
    return director.toJSON();
  }

  final public void load(String serialization) {
    director.onLoad(new JSON.Type.Variant(new JSON.Compiler(serialization)));
  }

}
