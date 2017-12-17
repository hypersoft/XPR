package XPR.JSON.Plus;

import XPR.Fault;
import XPR.JSON.Type.Variant;

import java.util.Locale;
import java.util.Map;

/**
 * Convert an HTTP header to a Variant and back.
 *
 * @author JSON.org
 * @version 2015-12-09
 */
public class HTTP {

  /**
   * Carriage return/line feed.
   */
  public static final String CRLF = "\r" + "\n";

  /**
   * Convert an HTTP header string into a Variant. It can be a request
   * header or a response header. A request header will contain
   * <pre>{
   *    Method: "POST" (for example),
   *    "Request-URI": "/" (for example),
   *    "HTTP-Version": "HTTP/1.1" (for example)
   * }</pre>
   * A response header will contain
   * <pre>{
   *    "HTTP-Version": "HTTP/1.1" (for example),
   *    "Status-Code": "200" (for example),
   *    "Reason-Phrase": "OK" (for example)
   * }</pre>
   * In addition, the other parameters in the header will be captured,
   * using
   * the HTTP field names as JSON names, so that <pre>
   *    Date: Sun, 26 May 2002 18:06:04 GMT
   *    Cookie: Q=q2=PPEAsg--; B=677gi6ouf29bn&b=2&f=s
   *    Cache-Control: no-cache</pre>
   * become
   * <pre>{...
   *    Date: "Sun, 26 May 2002 18:06:04 GMT",
   *    Cookie: "Q=q2=PPEAsg--; B=677gi6ouf29bn&b=2&f=s",
   *    "Cache-Control": "no-cache",
   * ...}</pre>
   * It does no further checking or conversion. It does not parse dates.
   * It does not do '%' transforms on URLs.
   *
   * @param string An HTTP header string.
   * @return A Variant containing the elements and attributes
   * of the XML string.
   * @throws Fault
   */
  public static Variant toJSONValue(String string) throws Fault {
    Variant jo = new Variant();
    Compiler x = new Compiler(string);
    String token;

    token = x.nextToken();
    if (token.toUpperCase(Locale.ROOT).startsWith("HTTP")) {

      // Response

      jo.put("HTTP-Version", token);
      jo.put("Status-Code", x.nextToken());
      jo.put("Reason-Phrase", x.nextTo('\0'));
      x.next();

    } else {

      // Request

      jo.put("Method", token);
      jo.put("Request-URI", x.nextToken());
      jo.put("HTTP-Version", x.nextToken());
    }

    // Fields

    while (x.more()) {
      String name = x.nextTo(':');
      x.next(':');
      jo.put(name, x.nextTo('\0'));
      x.next();
    }
    return jo;
  }


  /**
   * Convert a Variant into an HTTP header. A request header must contain
   * <pre>{
   *    Method: "POST" (for example),
   *    "Request-URI": "/" (for example),
   *    "HTTP-Version": "HTTP/1.1" (for example)
   * }</pre>
   * A response header must contain
   * <pre>{
   *    "HTTP-Version": "HTTP/1.1" (for example),
   *    "Status-Code": "200" (for example),
   *    "Reason-Phrase": "OK" (for example)
   * }</pre>
   * Any other members of the Variant will be output as HTTP fields.
   * The result will end with two CRLF pairs.
   *
   * @param jo A Variant
   * @return An HTTP header string.
   * @throws Fault if the object does not contain enough
   *               information.
   */
  public static String toString(Variant jo) throws Fault {
    StringBuilder sb = new StringBuilder();
    if (jo.has("Status-Code") && jo.has("Reason-Phrase")) {
      sb.append(jo.getString("HTTP-Version"));
      sb.append(' ');
      sb.append(jo.getString("Status-Code"));
      sb.append(' ');
      sb.append(jo.getString("Reason-Phrase"));
    } else if (jo.has("Method") && jo.has("Request-URI")) {
      sb.append(jo.getString("Method"));
      sb.append(' ');
      sb.append('"');
      sb.append(jo.getString("Request-URI"));
      sb.append('"');
      sb.append(' ');
      sb.append(jo.getString("HTTP-Version"));
    } else {
      throw new Fault("Not enough material for an HTTP header.");
    }
    sb.append(CRLF);
    for (final Map.Entry<String, ?> entry : jo.entrySet()) {
      final String key = entry.getKey();
      if (!"HTTP-Version".equals(key) && !"Status-Code".equals(key) &&
        !"Reason-Phrase".equals(key) && !"Method".equals(key) &&
        !"Request-URI".equals(key) && !Variant.NULL
        .equals(entry.getValue())) {
        sb.append(key);
        sb.append(": ");
        sb.append(jo.optString(key));
        sb.append(CRLF);
      }
    }
    sb.append(CRLF);
    return sb.toString();
  }

  /**
   * The Compiler extends the Compiler to provide additional methods
   * for the parsing of HTTP headers.
   *
   * @author JSON.org
   * @version 2015-12-09
   */
  public static class Compiler extends XPR.JSON.Compiler {

    /**
     * Construct an Compiler from a string.
     *
     * @param string A source string.
     */
    public Compiler(String string) {
      super(string);
    }


    /**
     * Get the next token or string. This is used in parsing HTTP headers.
     *
     * @return A String.
     * @throws Fault
     */
    public String nextToken() throws Fault {
      char c;
      char q;
      StringBuilder sb = new StringBuilder();
      do {
        c = next();
      } while (Character.isWhitespace(c));
      if (c == '"' || c == '\'') {
        q = c;
        for (; ; ) {
          c = next();
          if (c < ' ') {
            throw syntaxError("Unterminated string.");
          }
          if (c == q) {
            return sb.toString();
          }
          sb.append(c);
        }
      }
      for (; ; ) {
        if (c == 0 || Character.isWhitespace(c)) {
          return sb.toString();
        }
        sb.append(c);
        c = next();
      }
    }
  }
}
