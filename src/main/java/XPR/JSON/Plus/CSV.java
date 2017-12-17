package XPR.JSON.Plus;

import XPR.Fault;
import XPR.JSON.Compiler;
import XPR.JSON.Type.Variant;
import XPR.JSON.Type.VariantList;


/**
 * This provides static methods to convert comma delimited text into a
 * VariantList, and to convert a VariantList into comma delimited text. Comma
 * delimited text is a very popular format for data interchange. It is
 * understood by most database, spreadsheet, and organizer programs.
 * <p>
 * Each row of text represents a row in a table or a data record. Each row
 * ends with a NEWLINE character. Each row contains one or more values.
 * Values are separated by commas. A value can contain any character except
 * for comma, unless is is wrapped in single quotes or double quotes.
 * <p>
 * The first row usually contains the names of the columns.
 * <p>
 * A comma delimited list can be converted into a VariantList of VariantList.
 * The names for the elements in the VariantList can be taken from the names
 * in the first row.
 *
 * @author JSON.org
 * @version 2016-05-01
 */
public class CSV {

  /**
   * Get the next value. The value can be wrapped in quotes. The value can
   * be empty.
   *
   * @param x A Compiler of the source text.
   * @return The value string, or null if empty.
   * @throws Fault if the quoted string is badly formed.
   */
  private static String getValue(Compiler x) throws Fault {
    char c;
    char q;
    StringBuffer sb;
    do {
      c = x.next();
    } while (c == ' ' || c == '\t');
    switch (c) {
      case 0:
        return null;
      case '"':
      case '\'':
        q = c;
        sb = new StringBuffer();
        for (; ; ) {
          c = x.next();
          if (c == q) {
            //Handle escaped double-quote
            char nextC = x.next();
            if (nextC != '\"') {
              // if our quote was the end of the file, don't step
              if (nextC > 0) {
                x.back();
              }
              break;
            }
          }
          if (c == 0 || c == '\n' || c == '\r') {
            throw x.syntaxError("Missing close quote '" + q + "'.");
          }
          sb.append(c);
        }
        return sb.toString();
      case ',':
        x.back();
        return "";
      default:
        x.back();
        return x.nextTo(',');
    }
  }

  /**
   * Produce a VariantList of strings from a row of comma delimited
   * values.
   *
   * @param x A Compiler of the source text.
   * @return A VariantList of strings.
   * @throws Fault
   */
  public static VariantList rowToJSONElements(
    Compiler x) throws Fault
  {
    VariantList ja = new VariantList();
    for (; ; ) {
      String value = getValue(x);
      char c = x.next();
      if (value == null ||
        (ja.length() == 0 && value.length() == 0 && c != ',')) {
        return null;
      }
      ja.put(value);
      for (; ; ) {
        if (c == ',') {
          break;
        }
        if (c != ' ') {
          if (c == '\n' || c == '\r' || c == 0) {
            return ja;
          }
          throw x.syntaxError("Bad character '" + c + "' (" +
            (int) c + ").");
        }
        c = x.next();
      }
    }
  }

  /**
   * Produce a Variant from a row of comma delimited text, using a
   * parallel VariantList of strings to provides the names of the
   * elements.
   *
   * @param names A VariantList of names. This is commonly obtained
   *              from the
   *              first row of a comma delimited text file using the
   *              rowToJSONElements
   *              method.
   * @param x     A Compiler of the source text.
   * @return A Variant combining the names and values.
   * @throws Fault
   */
  public static Variant rowToJSONValue(VariantList names,
    Compiler x)
    throws Fault
  {
    VariantList ja = rowToJSONElements(x);
    return ja != null ? ja.toJSONValue(names) : null;
  }

  /**
   * Produce a comma delimited text row from a VariantList. Values containing
   * the comma character will be quoted. Troublesome characters may be
   * removed.
   *
   * @param ja A VariantList of strings.
   * @return A string ending in NEWLINE.
   */
  public static String rowToString(VariantList ja) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ja.length(); i += 1) {
      if (i > 0) {
        sb.append(',');
      }
      Object object = ja.opt(i);
      if (object != null) {
        String string = object.toString();
        if (string.length() > 0 && (string.indexOf(',') >= 0 ||
          string.indexOf('\n') >= 0 || string.indexOf('\r') >= 0 ||
          string.indexOf(0) >= 0 || string.charAt(0) == '"')) {
          sb.append('"');
          int length = string.length();
          for (int j = 0; j < length; j += 1) {
            char c = string.charAt(j);
            if (c >= ' ' && c != '"') {
              sb.append(c);
            }
          }
          sb.append('"');
        } else {
          sb.append(string);
        }
      }
    }
    sb.append('\n');
    return sb.toString();
  }

  /**
   * Produce a VariantList of VariantList from a comma delimited text string,
   * using the first row as a source of names.
   *
   * @param string The comma delimited text.
   * @return A VariantList of VariantList.
   * @throws Fault
   */
  public static VariantList toJSONElements(String string) throws
    Fault
  {
    return toJSONElements(new Compiler(string));
  }

  /**
   * Produce a VariantList of VariantList from a comma delimited text string,
   * using the first row as a source of names.
   *
   * @param x The Compiler containing the comma delimited text.
   * @return A VariantList of VariantList.
   * @throws Fault
   */
  public static VariantList toJSONElements(Compiler x) throws Fault {
    return toJSONElements(rowToJSONElements(x), x);
  }

  /**
   * Produce a VariantList of VariantList from a comma delimited text string
   * using a supplied VariantList as the source of element names.
   *
   * @param names  A VariantList of strings.
   * @param string The comma delimited text.
   * @return A VariantList of VariantList.
   * @throws Fault
   */
  public static VariantList toJSONElements(VariantList names,
    String string)
    throws Fault
  {
    return toJSONElements(names, new Compiler(string));
  }

  /**
   * Produce a VariantList of VariantList from a comma delimited text string
   * using a supplied VariantList as the source of element names.
   *
   * @param names A VariantList of strings.
   * @param x     A Compiler of the source text.
   * @return A VariantList of VariantList.
   * @throws Fault
   */
  public static VariantList toJSONElements(VariantList names,
    Compiler x)
    throws Fault
  {
    if (names == null || names.length() == 0) {
      return null;
    }
    VariantList ja = new VariantList();
    for (; ; ) {
      Variant jo = rowToJSONValue(names, x);
      if (jo == null) {
        break;
      }
      ja.put(jo);
    }
    if (ja.length() == 0) {
      return null;
    }
    return ja;
  }


  /**
   * Produce a comma delimited text from a VariantList of VariantList. The
   * first row will be a list of names obtained by inspecting the first
   * Variant.
   *
   * @param ja A VariantList of VariantList.
   * @return A comma delimited text.
   * @throws Fault
   */
  public static String toString(VariantList ja) throws Fault {
    Variant jo = ja.optJSONValue(0);
    if (jo != null) {
      VariantList names = jo.names();
      if (names != null) {
        return rowToString(names) + toString(names, ja);
      }
    }
    return null;
  }

  /**
   * Produce a comma delimited text from a VariantList of VariantList using
   * a provided list of names. The list of names is not included in the
   * output.
   *
   * @param names A VariantList of strings.
   * @param ja    A VariantList of VariantList.
   * @return A comma delimited text.
   * @throws Fault
   */
  public static String toString(
    VariantList names, VariantList ja)
    throws Fault
  {
    if (names == null || names.length() == 0) {
      return null;
    }
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < ja.length(); i += 1) {
      Variant jo = ja.optJSONValue(i);
      if (jo != null) {
        sb.append(rowToString(jo.toJSONElements(names)));
      }
    }
    return sb.toString();
  }
}
