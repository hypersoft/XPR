package XPR;

import java.util.StringJoiner;

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

  public static String concatenate(String join, String... data)
  {
    return concatenate(join, 0, 0, data);
  }

  public static String concatenate(String join, int start, String... data)
  {
    return concatenate(join, start, 0, data);
  }

  public static String concatenate(String join, int start, int count,
    String... data) {
    StringJoiner j = new StringJoiner(join);
    if (count < 0) count += data.length;
    else if (count == 0) count = data.length;
    else count += start;
    for (int i = start; i < count; i++) {
      j.add(data[i]);
    }
    return j.toString();
  }

}
