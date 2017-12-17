package XPR.JSON.Plus;

import XPR.Fault;
import XPR.JSON.Compiler;
import XPR.JSON.Type.Variant;

import java.util.Map;

/**
 * Convert a web browser cookie specification to a Variant and back.
 * JSON and Cookies are both notations for name/value pairs.
 *
 * @author JSON.org
 * @version 2015-12-09
 */
public class Cookie {

  /**
   * Produce a copy of a string in which the characters '+', '%', '=', ';'
   * and control characters are replaced with "%hh". This is a gentle form
   * of URL encoding, attempting to cause as little distortion to the
   * string as possible. The characters '=' and ';' are meta characters in
   * cookies. By convention, they are escaped using the URL-encoding.
   * This is
   * only a convention, not a standard. Often, cookies are expected to
   * have
   * encoded values. We encode '=' and ';' because we must. We encode
   * '%' and
   * '+' because they are meta characters in URL encoding.
   *
   * @param string The source string.
   * @return The escaped result.
   */
  public static String escape(String string) {
    char c;
    String s = string.trim();
    int length = s.length();
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i += 1) {
      c = s.charAt(i);
      if (c < ' ' || c == '+' || c == '%' || c == '=' || c == ';') {
        sb.append('%');
        sb.append(Character.forDigit((char) ((c >>> 4) & 0x0f), 16));
        sb.append(Character.forDigit((char) (c & 0x0f), 16));
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }


  /**
   * Convert a cookie specification string into a Variant. The string
   * will contain a name value pair separated by '='. The name and the
   * value
   * will be unescaped, possibly converting '+' and '%' sequences. The
   * cookie values may follow, separated by ';', also represented as
   * name=value (except the secure property, which does not have a value).
   * The name will be stored under the key "name", and the value will be
   * stored under the key "value". This method does not do checking or
   * validation of the parameters. It only converts the cookie string into
   * a Variant.
   *
   * @param string The cookie specification string.
   * @return A Variant containing "name", "value", and possibly other
   * members.
   * @throws Fault
   */
  public static Variant toJSONValue(String string) throws Fault {
    String name;
    Variant jo = new Variant();
    Object value;
    XPR.JSON.Compiler x = new XPR.JSON.Compiler(string);
    jo.put("name", x.nextTo('='));
    x.next('=');
    jo.put("value", x.nextTo(';'));
    x.next();
    while (x.more()) {
      name = unescape(x.nextTo("=;"));
      if (x.next() != '=') {
        if (name.equals("secure")) {
          value = Boolean.TRUE;
        } else {
          throw x.syntaxError("Missing '=' in cookie parameter.");
        }
      } else {
        value = unescape(x.nextTo(';'));
        x.next();
      }
      jo.put(name, value);
    }
    return jo;
  }


  /**
   * Convert a Variant into a cookie specification string. The Variant
   * must contain "name" and "value" members.
   * If the Variant contains "expires", "domain", "path", or "secure"
   * members, they will be appended to the cookie specification string.
   * All other members are ignored.
   *
   * @param jo A Variant
   * @return A cookie specification string
   * @throws Fault
   */
  public static String toString(Variant jo) throws Fault {
    StringBuilder sb = new StringBuilder();

    sb.append(escape(jo.getString("name")));
    sb.append("=");
    sb.append(escape(jo.getString("value")));
    if (jo.has("expires")) {
      sb.append(";expires=");
      sb.append(jo.getString("expires"));
    }
    if (jo.has("domain")) {
      sb.append(";domain=");
      sb.append(escape(jo.getString("domain")));
    }
    if (jo.has("path")) {
      sb.append(";path=");
      sb.append(escape(jo.getString("path")));
    }
    if (jo.optBoolean("secure")) {
      sb.append(";secure");
    }
    return sb.toString();
  }

  /**
   * Convert <code>%</code><i>hh</i> sequences to single characters, and
   * convert plus to space.
   *
   * @param string A string that may contain
   *               <code>+</code>&nbsp;<small>(plus)</small> and
   *               <code>%</code><i>hh</i> sequences.
   * @return The unescaped string.
   */
  public static String unescape(String string) {
    int length = string.length();
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; ++i) {
      char c = string.charAt(i);
      if (c == '+') {
        c = ' ';
      } else if (c == '%' && i + 2 < length) {
        int d = Compiler.dehexchar(string.charAt(i + 1));
        int e = Compiler.dehexchar(string.charAt(i + 2));
        if (d >= 0 && e >= 0) {
          c = (char) (d * 16 + e);
          i += 2;
        }
      }
      sb.append(c);
    }
    return sb.toString();
  }

  /**
   * Convert a web browser cookie list string to a Variant and back.
   *
   * @author JSON.org
   * @version 2015-12-09
   */
  public static class Batch {

    /**
     * Convert a cookie list into a Variant. A cookie list is a sequence
     * of name/value pairs. The names are separated from the values by
     * '='.
     * The pairs are separated by ';'. The names and the values
     * will be unescaped, possibly converting '+' and '%' sequences.
     * <p>
     * To add a cookie to a cookie list,
     * cookielistJSONValue.put(cookieJSONValue.getString("name"),
     * cookieJSONValue.getString("value"));
     *
     * @param string A cookie list string
     * @return A Variant
     * @throws Fault
     */
    public static Variant toJSONValue(String string) throws Fault {
      Variant jo = new Variant();
      Compiler x = new Compiler(string);
      while (x.more()) {
        String name = unescape(x.nextTo('='));
        x.next('=');
        jo.put(name, unescape(x.nextTo(';')));
        x.next();
      }
      return jo;
    }

    /**
     * Convert a Variant into a cookie list. A cookie list is a sequence
     * of name/value pairs. The names are separated from the values by '='.
     * The pairs are separated by ';'. The characters '%', '+', '=', and ';'
     * in the names and values are replaced by "%hh".
     *
     * @param jo A Variant
     * @return A cookie list string
     * @throws Fault
     */
    public static String toString(Variant jo) throws Fault {
      boolean b = false;
      StringBuilder sb = new StringBuilder();
      for (final Map.Entry<String, ?> entry : jo.entrySet()) {
        final String key = entry.getKey();
        final Object value = entry.getValue();
        if (!Variant.NULL.equals(value)) {
          if (b) {
            sb.append(';');
          }
          sb.append(escape(key));
          sb.append("=");
          sb.append(escape(value.toString()));
          b = true;
        }
      }
      return sb.toString();
    }
  }
}
