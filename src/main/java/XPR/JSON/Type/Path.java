package XPR.JSON.Type;

import XPR.Fault;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

/**
 * A JSON Pointer is a simple query language defined for JSON documents by
 * <a href="https://tools.ietf.org/html/rfc6901">RFC 6901</a>.
 * <p>
 * In a nutshell, Path allows the user to navigate into a JSON document
 * using strings, and retrieve targeted objects, like a simple form of XPATH.
 * Path segments are separated by the '/' char, which signifies the root of
 * the document when it appears as the first char of the string. Array
 * elements are navigated using ordinals, counting from 0. Path strings
 * may be extended to any arbitrary number of segments. If the navigation
 * is successful, the matched item is returned. A matched item may be a
 * Variant, a VariantList, or a JSON value. If the Path string building
 * fails, an appropriate exception is thrown. If the navigation fails to
 * find
 * a match, a Fault is thrown.
 *
 * @author JSON.org
 * @version 2016-05-14
 */
public class Path {

  // used for URL encoding and decoding
  private static final String ENCODING = "utf-8";
  // Segments for the Path string
  private final List<String> refTokens;

  /**
   * Pre-parses and initializes a new {@code Path} instance. If you want to
   * evaluate the same JSON Pointer on different JSON documents then it is
   * recommended
   * to keep the {@code Path} instances due to performance considerations.
   *
   * @param pointer the JSON String or URI Fragment representation of the
   *                JSON pointer.
   * @throws IllegalArgumentException if {@code pointer} is not a valid
   *                                  JSON pointer
   */
  public Path(final String pointer) {
    if (pointer == null) {
      throw new NullPointerException("pointer cannot be null");
    }
    if (pointer.isEmpty() || pointer.equals("#")) {
      this.refTokens = Collections.emptyList();
      return;
    }
    String refs;
    if (pointer.startsWith("#/")) {
      refs = pointer.substring(2);
      try {
        refs = URLDecoder.decode(refs, ENCODING);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    } else if (pointer.startsWith("/")) {
      refs = pointer.substring(1);
    } else {
      throw new IllegalArgumentException(
        "a JSON pointer should start with '/' or '#/'");
    }
    this.refTokens = new ArrayList<String>();
    for (String token : refs.split("/")) {
      this.refTokens.add(unescape(token));
    }
  }

  public Path(List<String> refTokens) {
    this.refTokens = new ArrayList<String>(refTokens);
  }

  /**
   * Static factory method for {@link Builder}. Example usage:
   * <p>
   * <pre><code>
   * Path pointer = Path.builder()
   *       .add("obj")
   *       .add("other~key").add("another/key")
   *       .add("\"")
   *       .add(0)
   *       .build();
   * </code></pre>
   *
   * @return a builder instance which can be used to construct a {@code
   * Path} instance by chained
   * {@link Builder#add(String)} calls.
   */
  public static Builder builder() {
    return new Builder();
  }

  private String unescape(String token) {
    return token.replace("~1", "/").replace("~0", "~")
      .replace("\\\"", "\"")
      .replace("\\\\", "\\");
  }

  /**
   * Evaluates this JSON Pointer on the given {@code document}. The {@code
   * document}
   * is usually a {@link Variant} or a {@link VariantList} instance, but
   * the empty
   * JSON Pointer ({@code ""}) can be evaluated on any JSON values and in
   * such case the
   * returned value will be {@code document} itself.
   *
   * @param document the JSON document which should be the subject of
   *                 querying.
   * @return the result of the evaluation
   * @throws Fault if an error occurs during evaluation
   */
  public Object queryFrom(Object document) {
    if (this.refTokens.isEmpty()) {
      return document;
    }
    Object current = document;
    for (String token : this.refTokens) {
      if (current instanceof Variant) {
        current = ((Variant) current).opt(unescape(token));
      } else if (current instanceof VariantList) {
        current = readByIndexToken(current, token);
      } else {
        throw new Fault(format(
          "value [%s] is not an array or object therefore its key %s cannot" +
            " be resolved",
          current,
          token
        ));
      }
    }
    return current;
  }

  /**
   * Matches a VariantList element by ordinal position
   *
   * @param current    the VariantList to be evaluated
   * @param indexToken the array index in string form
   * @return the matched object. If no matching item is found a
   * Fault is thrown
   */
  @SuppressWarnings("boxing")
  private Object readByIndexToken(Object current, String indexToken) {
    try {
      int index = Integer.parseInt(indexToken);
      VariantList currentArr = (VariantList) current;
      if (index >= currentArr.length()) {
        throw new Fault(
          format(
            "index %d is out of bounds - the array has %d elements",
            index,
            currentArr.length()
          ));
      }
      return currentArr.get(index);
    } catch (NumberFormatException e) {
      throw new Fault(format("%s is not an array index", indexToken), e);
    }
  }

  /**
   * Returns a string representing the Path path value using string
   * representation
   */
  @Override
  public String toString() {
    StringBuilder rval = new StringBuilder("");
    for (String token : this.refTokens) {
      rval.append('/').append(escape(token));
    }
    return rval.toString();
  }

  /**
   * Escapes path segment values to an unambiguous form.
   * The escape char to be inserted is '~'. The chars to be escaped
   * are ~, which maps to ~0, and /, which maps to ~1. Backslashes
   * and double quote chars are also escaped.
   *
   * @param token the Path segment value to be escaped
   * @return the escaped value for the token
   */
  private String escape(String token) {
    return token.replace("~", "~0")
      .replace("/", "~1")
      .replace("\\", "\\\\")
      .replace("\"", "\\\"");
  }

  /**
   * Returns a string representing the Path path value using URI
   * fragment identifier representation
   */
  public String toURIFragment() {
    try {
      StringBuilder rval = new StringBuilder("#");
      for (String token : this.refTokens) {
        rval.append('/').append(URLEncoder.encode(token, ENCODING));
      }
      return rval.toString();
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This class allows the user to build a Path in steps, using
   * exactly one segment in each step.
   */
  public static class Builder {

    // Segments for the eventual Path string
    private final List<String> refTokens = new ArrayList<String>();

    /**
     * Creates a {@code Path} instance using the tokens previously
     * set using the
     * {@link #add(String)} method calls.
     */
    public Path build() {
      return new Path(this.refTokens);
    }

    /**
     * Adds an arbitrary token to the list of reference tokens. It can be
     * any non-null value.
     * <p>
     * Unlike in the case of JSON string or URI fragment
     * representation of JSON pointers, the
     * argument of this method MUST NOT be escaped. If you want to query
     * the property called
     * {@code "a~b"} then you should simply pass the {@code "a~b"} string
     * as-is, there is no
     * need to escape it as {@code "a~0b"}.
     *
     * @param token the new token to be appended to the list
     * @return {@code this}
     * @throws NullPointerException if {@code token} is null
     */
    public Builder add(String token) {
      if (token == null) {
        throw new NullPointerException("token cannot be null");
      }
      this.refTokens.add(token);
      return this;
    }

    /**
     * Adds an integer to the reference token list. Although not
     * necessarily, mostly this token will
     * denote an array index.
     *
     * @param arrayIndex the array index to be added to the token list
     * @return {@code this}
     */
    public Builder add(int arrayIndex) {
      this.refTokens.add(String.valueOf(arrayIndex));
      return this;
    }
  }

}