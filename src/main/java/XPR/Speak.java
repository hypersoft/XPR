package XPR;

public final class Speak {
  private Speak(){};
  public static String quoteCitation(Object text) {
    return " (" + text + ")";
  }

  public static String quoteFunction(String name, String... parameters) {
    return name + quoteFunctionParameters(parameters);
  }

  public static String quoteEllipsis(Object text) {
    return text + "...";
  }

  public static String quoteExactTarget(Object text) {
    return quoteTarget(quoteExact(text));
  }

  public static String quoteFunctionParameters(String... param) {
    return "(" + (String.join(", ", param)) + ")";
  }

  public static String quoteTarget(Object text) {
    return ": " + text;
  }

  public static String quoteFunctionReturnType(Object text) {
    return " => " + text;
  }

  public static String quoteExact(Object text) {
    return "`" + text + "'";
  }

  public static String quoteCountPlurality(long count, Object text) {
    if (count != 1) return quoteCitation(count) + " " + text + "s";
    else return quoteCitation(count) + " " + text;
  }

  public static String quoteFunctionSpecification(String type, String...
    names) {
    return "f:" + quoteFunctionParameters(names) + quoteFunctionReturnType(type);
  }

  public static String quoteAnd(Object text) {
    return "; " + text;
  }

  public static String quoteSingle(Object text) {
    return "'" + text + "'";
  }

  public static String quoteDouble(Object text) {
    return "\"" + text + "\"";
  }

  public static String getParameterLengthCallout(int count, int max) {
    return quoteAnd("the caller of this entry-point has given") +
      quoteCountPlurality(count, "parameter")
      + quoteAnd("this entry-point takes a minimum of")
      + quoteCountPlurality(max, "parameter")
      ;
  }

}
