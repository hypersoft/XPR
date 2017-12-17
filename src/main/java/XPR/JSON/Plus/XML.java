package XPR.JSON.Plus;

import XPR.Fault;
import XPR.JSON.Type.Variant;
import XPR.JSON.Type.VariantList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This provides static methods to convert an XML text into a Variant, and to
 * covert a Variant into an XML text.
 *
 * @author JSON.org
 * @version 2016-08-10
 */
@SuppressWarnings("boxing")
public class XML {
  /**
   * The Character '&amp;'.
   */
  public static final Character AMP = '&';

  /**
   * The Character '''.
   */
  public static final Character APOS = '\'';

  /**
   * The Character '!'.
   */
  public static final Character BANG = '!';

  /**
   * The Character '='.
   */
  public static final Character EQ = '=';

  /**
   * The Character '>'.
   */
  public static final Character GT = '>';

  /**
   * The Character '&lt;'.
   */
  public static final Character LT = '<';

  /**
   * The Character '?'.
   */
  public static final Character QUEST = '?';

  /**
   * The Character '"'.
   */
  public static final Character QUOT = '"';

  /**
   * The Character '/'.
   */
  public static final Character SLASH = '/';

  /**
   * Creates an iterator for navigating Code Points in a string instead of
   * characters. Once Java7 support is dropped, this can be replaced with
   * <code>
   * string.codePoints()
   * </code>
   * which is available in Java8 and above.
   *
   * @see <a href=
   * "http://stackoverflow.com/a/21791059/6030888">http://stackoverflow
   * .com/a/21791059/6030888</a>
   */
  private static Iterable<Integer> codePointIterator(final String string) {
    return new Iterable<Integer>() {
      @Override
      public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
          private int nextIndex = 0;
          private int length = string.length();

          @Override
          public boolean hasNext() {
            return this.nextIndex < this.length;
          }

          @Override
          public Integer next() {
            int result = string.codePointAt(this.nextIndex);
            this.nextIndex += Character.charCount(result);
            return result;
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  /**
   * Replace special characters with XML escapes:
   * <p>
   * <pre>
   * &amp; <small>(ampersand)</small> is replaced by &amp;amp;
   * &lt; <small>(less than)</small> is replaced by &amp;lt;
   * &gt; <small>(greater than)</small> is replaced by &amp;gt;
   * &quot; <small>(double quote)</small> is replaced by &amp;quot;
   * &apos; <small>(single quote / apostrophe)</small> is replaced by &amp;apos;
   * </pre>
   *
   * @param string The string to be escaped.
   * @return The escaped string.
   */
  public static String escape(String string) {
    StringBuilder sb = new StringBuilder(string.length());
    for (final int cp : codePointIterator(string)) {
      switch (cp) {
        case '&':
          sb.append("&amp;");
          break;
        case '<':
          sb.append("&lt;");
          break;
        case '>':
          sb.append("&gt;");
          break;
        case '"':
          sb.append("&quot;");
          break;
        case '\'':
          sb.append("&apos;");
          break;
        default:
          if (mustEscape(cp)) {
            sb.append("&#x");
            sb.append(Integer.toHexString(cp));
            sb.append(';');
          } else {
            sb.appendCodePoint(cp);
          }
      }
    }
    return sb.toString();
  }

  /**
   * @param cp code point to test
   * @return true if the code point is not valid for an XML
   */
  private static boolean mustEscape(int cp) {
            /* Valid range from https://www.w3.org/TR/REC-xml/#charsets
             *
             * #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] |
             * [#x10000-#x10FFFF]
             *
             * any Unicode character, excluding the surrogate blocks, FFFE,
             * and FFFF.
             */
    // isISOControl is true when (cp >= 0 && cp <= 0x1F) || (cp >= 0x7F && cp
    // <= 0x9F)
    // all ISO control characters are out of range except tabs and new lines
    return (Character.isISOControl(cp)
      && cp != 0x9
      && cp != 0xA
      && cp != 0xD
    ) || !(
      // valid the range of acceptable characters that aren't
      // control
      (cp >= 0x20 && cp <= 0xD7FF)
        || (cp >= 0xE000 && cp <= 0xFFFD)
        || (cp >= 0x10000 && cp <= 0x10FFFF)
    )
      ;
  }

  /**
   * Removes XML escapes from the string.
   *
   * @param string string to remove escapes from
   * @return string with converted entities
   */
  public static String unescape(String string) {
    StringBuilder sb = new StringBuilder(string.length());
    for (int i = 0, length = string.length(); i < length; i++) {
      char c = string.charAt(i);
      if (c == '&') {
        final int semic = string.indexOf(';', i);
        if (semic > i) {
          final String entity = string.substring(i + 1, semic);
          sb.append(Compiler.unescapeEntity(entity));
          // skip past the entity we just parsed.
          i += entity.length() + 1;
        } else {
          // this shouldn't happen in most cases since the parser
          // errors on unclosed entries.
          sb.append(c);
        }
      } else {
        // not part of an entity
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * Throw an exception if the string contains whitespace. Whitespace is not
   * allowed in tagNames and attributes.
   *
   * @param string A string.
   * @throws Fault Thrown if the string contains whitespace or is empty.
   */
  public static void noSpace(String string) throws Fault {
    int i, length = string.length();
    if (length == 0) {
      throw new Fault("Empty string.");
    }
    for (i = 0; i < length; i += 1) {
      if (Character.isWhitespace(string.charAt(i))) {
        throw new Fault("'" + string
          + "' contains a space character.");
      }
    }
  }

  /**
   * Scan the content following the named tag, attaching it to the context.
   *
   * @param x       The Compiler containing the source string.
   * @param context The Variant that will include the new material.
   * @param name    The tag name.
   * @return true if the close tag is processed.
   * @throws Fault
   */
  private static boolean parse(Compiler x, Variant context, String name,
    boolean keepStrings)
    throws Fault
  {
    char c;
    int i;
    Variant jsonobject = null;
    String string;
    String tagName;
    Object token;

    // Test for and skip past these forms:
    // <!-- ... -->
    // <! ... >
    // <![ ... ]]>
    // <? ... ?>
    // Report errors for these forms:
    // <>
    // <=
    // <<

    token = x.nextToken();

    // <!

    if (token == BANG) {
      c = x.next();
      if (c == '-') {
        if (x.next() == '-') {
          x.skipPast("-->");
          return false;
        }
        x.back();
      } else if (c == '[') {
        token = x.nextToken();
        if ("CDATA".equals(token)) {
          if (x.next() == '[') {
            string = x.nextCDATA();
            if (string.length() > 0) {
              context.accumulate("content", string);
            }
            return false;
          }
        }
        throw x.syntaxError("Expected 'CDATA['");
      }
      i = 1;
      do {
        token = x.nextMeta();
        if (token == null) {
          throw x.syntaxError("Missing '>' after '<!'.");
        } else if (token == LT) {
          i += 1;
        } else if (token == GT) {
          i -= 1;
        }
      } while (i > 0);
      return false;
    } else if (token == QUEST) {

      // <?
      x.skipPast("?>");
      return false;
    } else if (token == SLASH) {

      // Close tag </

      token = x.nextToken();
      if (name == null) {
        throw x.syntaxError("Mismatched close tag " + token);
      }
      if (!token.equals(name)) {
        throw x.syntaxError("Mismatched " + name + " and " + token);
      }
      if (x.nextToken() != GT) {
        throw x.syntaxError("Misshaped close tag");
      }
      return true;

    } else if (token instanceof Character) {
      throw x.syntaxError("Misshaped tag");

      // Open tag <

    } else {
      tagName = (String) token;
      token = null;
      jsonobject = new Variant();
      for (; ; ) {
        if (token == null) {
          token = x.nextToken();
        }
        // attribute = value
        if (token instanceof String) {
          string = (String) token;
          token = x.nextToken();
          if (token == EQ) {
            token = x.nextToken();
            if (!(token instanceof String)) {
              throw x.syntaxError("Missing value");
            }
            jsonobject.accumulate(
              string,
              keepStrings ? ((String) token) : stringToValue((String) token)
            );
            token = null;
          } else {
            jsonobject.accumulate(string, "");
          }


        } else if (token == SLASH) {
          // Empty tag <.../>
          if (x.nextToken() != GT) {
            throw x.syntaxError("Misshaped tag");
          }
          if (jsonobject.length() > 0) {
            context.accumulate(tagName, jsonobject);
          } else {
            context.accumulate(tagName, "");
          }
          return false;

        } else if (token == GT) {
          // Content, between <...> and </...>
          for (; ; ) {
            token = x.nextContent();
            if (token == null) {
              if (tagName != null) {
                throw x.syntaxError("Unclosed tag " + tagName);
              }
              return false;
            } else if (token instanceof String) {
              string = (String) token;
              if (string.length() > 0) {
                jsonobject.accumulate(
                  "content",
                  keepStrings ? string : stringToValue(string)
                );
              }

            } else if (token == LT) {
              // Nested element
              if (parse(x, jsonobject, tagName, keepStrings)) {
                if (jsonobject.length() == 0) {
                  context.accumulate(tagName, "");
                } else if (jsonobject.length() == 1
                  && jsonobject.opt("content") != null) {
                  context.accumulate(
                    tagName,
                    jsonobject.opt("content")
                  );
                } else {
                  context.accumulate(tagName, jsonobject);
                }
                return false;
              }
            }
          }
        } else {
          throw x.syntaxError("Misshaped tag");
        }
      }
    }
  }

  /**
   * This method is the same as {@link Variant#stringToValue(String)}
   * except that this also tries to unescape String values.
   *
   * @param string String to convert
   * @return JSON value of this string or the string
   */
  public static Object stringToValue(String string) {
    return Variant.stringToValue(string);
  }

  /**
   * Convert a well-formed (but not necessarily valid) XML string into a
   * Variant. Some information may be lost in this transformation because
   * JSON is a data format and XML is a document format. XML uses
   * elements,
   * attributes, and content text, while JSON uses unordered
   * collections of
   * name/value pairs and arrays of values. JSON does not does not like to
   * distinguish between elements and attributes. Sequences of similar
   * elements are represented as JSONElementss. Content text may be
   * placed in a
   * "content" member. Comments, prologs, DTDs, and <code>&lt;[ [
   * ]]></code>
   * are ignored.
   *
   * @param string The source string.
   * @return A Variant containing the structured data from the XML string.
   * @throws Fault Thrown if there is an errors while parsing the string
   */
  public static Variant toJSONValue(String string) throws Fault {
    return toJSONValue(string, false);
  }


  /**
   * Convert a well-formed (but not necessarily valid) XML string into a
   * Variant. Some information may be lost in this transformation because
   * JSON is a data format and XML is a document format. XML uses
   * elements,
   * attributes, and content text, while JSON uses unordered
   * collections of
   * name/value pairs and arrays of values. JSON does not does not like to
   * distinguish between elements and attributes. Sequences of similar
   * elements are represented as JSONElementss. Content text may be
   * placed in a
   * "content" member. Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code>
   * are ignored.
   * <p>
   * All values are converted as strings, for 1, 01, 29.0 will not be coerced to
   * numbers but will instead be the exact value as seen in the XML document.
   *
   * @param string      The source string.
   * @param keepStrings If true, then values will not be coerced into
   *                    boolean
   *                    or numeric values and will instead be left as
   *                    strings
   * @return A Variant containing the structured data from the XML string.
   * @throws Fault Thrown if there is an errors while parsing the string
   */
  public static Variant toJSONValue(String string, boolean
    keepStrings) throws Fault
  {
    Variant jo = new Variant();
    Compiler x = new Compiler(string);
    while (x.more() && x.skipPast("<")) {
      parse(x, jo, null, keepStrings);
    }
    return jo;
  }

  /**
   * Convert a Variant into a well-formed, element-normal XML string.
   *
   * @param object A Variant.
   * @return A string.
   * @throws Fault Thrown if there is an error parsing the string
   */
  public static String toString(Object object) throws Fault {
    return toString(object, null);
  }

  /**
   * Convert a Variant into a well-formed, element-normal XML string.
   *
   * @param object  A Variant.
   * @param tagName The optional name of the enclosing tag.
   * @return A string.
   * @throws Fault Thrown if there is an error parsing the string
   */
  public static String toString(final Object object, final String tagName)
    throws Fault
  {
    StringBuilder sb = new StringBuilder();
    VariantList ja;
    Variant jo;
    String string;

    if (object instanceof Variant) {

      // Emit <tagName>
      if (tagName != null) {
        sb.append('<');
        sb.append(tagName);
        sb.append('>');
      }

      // Loop thru the keys.
      jo = (Variant) object;
      for (final Map.Entry<String, ?> entry : jo.entrySet()) {
        final String key = entry.getKey();
        Object value = entry.getValue();
        if (value == null) {
          value = "";
        } else if (value.getClass().isArray()) {
          value = new VariantList(value);
        }

        // Emit content in body
        if ("content".equals(key)) {
          if (value instanceof VariantList) {
            ja = (VariantList) value;
            int i = 0;
            for (Object val : ja) {
              if (i > 0) {
                sb.append('\n');
              }
              sb.append(escape(val.toString()));
              i++;
            }
          } else {
            sb.append(escape(value.toString()));
          }

          // Emit an array of similar keys

        } else if (value instanceof VariantList) {
          ja = (VariantList) value;
          for (Object val : ja) {
            if (val instanceof VariantList) {
              sb.append('<');
              sb.append(key);
              sb.append('>');
              sb.append(toString(val));
              sb.append("</");
              sb.append(key);
              sb.append('>');
            } else {
              sb.append(toString(val, key));
            }
          }
        } else if ("".equals(value)) {
          sb.append('<');
          sb.append(key);
          sb.append("/>");

          // Emit a new tag <k>

        } else {
          sb.append(toString(value, key));
        }
      }
      if (tagName != null) {

        // Emit the </tagname> close tag
        sb.append("</");
        sb.append(tagName);
        sb.append('>');
      }
      return sb.toString();

    }

    if (object != null && (object instanceof VariantList || object
      .getClass().isArray())) {
      if (object.getClass().isArray()) {
        ja = new VariantList(object);
      } else {
        ja = (VariantList) object;
      }
      for (Object val : ja) {
        // XML does not have good support for arrays. If an array
        // appears in a place where XML is lacking, synthesize an
        // <array> element.
        sb.append(toString(val, tagName == null ? "array" : tagName));
      }
      return sb.toString();
    }

    string = (object == null) ? "null" : escape(object.toString());
    return (tagName == null) ? "\"" + string + "\""
      : (string.length() == 0) ? "<" + tagName + "/>" : "<" +
      tagName
      + ">" + string + "</" + tagName + ">";

  }

  /**
   * The Compiler extends the Compiler to provide additional methods
   * for the parsing of XML texts.
   *
   * @author JSON.org
   * @version 2015-12-09
   */
  public static class Compiler extends XPR.JSON.Compiler {


    /**
     * The table of entity values. It initially contains Character values for
     * amp, apos, gt, lt, quot.
     */
    public static final HashMap<String, Character> entity;

    static {
      entity = new HashMap<String, Character>(8);
      entity.put("amp", AMP);
      entity.put("apos", APOS);
      entity.put("gt", GT);
      entity.put("lt", LT);
      entity.put("quot", QUOT);
    }

    /**
     * Construct an Compiler from a string.
     *
     * @param s A source string.
     */
    public Compiler(String s) {
      super(s);
    }

    /**
     * Unescapes an XML entity encoding;
     *
     * @param e entity (only the actual entity value, not the preceding
     *          & or ending ;
     * @return
     */
    static String unescapeEntity(String e) {
      // validate
      if (e == null || e.isEmpty()) {
        return "";
      }
      // if our entity is an encoded unicode point, parse it.
      if (e.charAt(0) == '#') {
        int cp;
        if (e.charAt(1) == 'x') {
          // hex encoded unicode
          cp = Integer.parseInt(e.substring(2), 16);
        } else {
          // decimal encoded unicode
          cp = Integer.parseInt(e.substring(1));
        }
        return new String(new int[]{cp}, 0, 1);
      }
      Character knownEntity = entity.get(e);
      if (knownEntity == null) {
        // we don't know the entity so keep it encoded
        return '&' + e + ';';
      }
      return knownEntity.toString();
    }

    /**
     * Get the text in the CDATA block.
     *
     * @return The string up to the <code>]]&gt;</code>.
     * @throws Fault If the <code>]]&gt;</code> is not found.
     */
    public String nextCDATA() throws Fault {
      char c;
      int i;
      StringBuilder sb = new StringBuilder();
      while (more()) {
        c = next();
        sb.append(c);
        i = sb.length() - 3;
        if (i >= 0 && sb.charAt(i) == ']' &&
          sb.charAt(i + 1) == ']' && sb.charAt(i + 2) == '>') {
          sb.setLength(i);
          return sb.toString();
        }
      }
      throw syntaxError("Unclosed CDATA");
    }

    /**
     * Get the next XML outer token, trimming whitespace. There are two kinds
     * of tokens: the '<' character which begins a markup tag, and the content
     * text between markup tags.
     *
     * @return A string, or a '<' Character, or null if there is no more
     * source text.
     * @throws Fault
     */
    public Object nextContent() throws Fault {
      char c;
      StringBuilder sb;
      do {
        c = next();
      } while (Character.isWhitespace(c));
      if (c == 0) {
        return null;
      }
      if (c == '<') {
        return LT;
      }
      sb = new StringBuilder();
      for (; ; ) {
        if (c == 0) {
          return sb.toString().trim();
        }
        if (c == '<') {
          back();
          return sb.toString().trim();
        }
        if (c == '&') {
          sb.append(nextEntity(c));
        } else {
          sb.append(c);
        }
        c = next();
      }
    }

    /**
     * Return the next entity. These entities are translated to
     * Characters:
     * <code>&amp;  &apos;  &gt;  &lt;  &quot;</code>.
     *
     * @param ampersand An ampersand character.
     * @return A Character or an entity String if the entity is not recognized.
     * @throws Fault If missing ';' in XML entity.
     */
    public Object nextEntity(char ampersand) throws Fault {
      StringBuilder sb = new StringBuilder();
      for (; ; ) {
        char c = next();
        if (Character.isLetterOrDigit(c) || c == '#') {
          sb.append(Character.toLowerCase(c));
        } else if (c == ';') {
          break;
        } else {
          throw syntaxError("Missing ';' in XML entity: &" + sb);
        }
      }
      String string = sb.toString();
      return unescapeEntity(string);
    }

    /**
     * Returns the next XML meta token. This is used for skipping over <!...>
     * and <?...?> structures.
     *
     * @return Syntax characters (<code>< > / = ! ?</code>) are returned as
     * Character, and strings and names are returned as Boolean. We don't care
     * what the values actually are.
     * @throws Fault If a string is not properly closed or if the XML
     *               is badly structured.
     */
    public Object nextMeta() throws Fault {
      char c;
      char q;
      do {
        c = next();
      } while (Character.isWhitespace(c));
      switch (c) {
        case 0:
          throw syntaxError("Misshaped meta tag");
        case '<':
          return LT;
        case '>':
          return GT;
        case '/':
          return SLASH;
        case '=':
          return EQ;
        case '!':
          return BANG;
        case '?':
          return QUEST;
        case '"':
        case '\'':
          q = c;
          for (; ; ) {
            c = next();
            if (c == 0) {
              throw syntaxError("Unterminated string");
            }
            if (c == q) {
              return Boolean.TRUE;
            }
          }
        default:
          for (; ; ) {
            c = next();
            if (Character.isWhitespace(c)) {
              return Boolean.TRUE;
            }
            switch (c) {
              case 0:
              case '<':
              case '>':
              case '/':
              case '=':
              case '!':
              case '?':
              case '"':
              case '\'':
                back();
                return Boolean.TRUE;
            }
          }
      }
    }


    /**
     * Get the next XML Token. These tokens are found inside of angle
     * brackets. It may be one of these characters: <code>/ > = ! ?</code> or it
     * may be a string wrapped in single quotes or double quotes, or it may be a
     * name.
     *
     * @return a String or a Character.
     * @throws Fault If the XML is not well formed.
     */
    public Object nextToken() throws Fault {
      char c;
      char q;
      StringBuilder sb;
      do {
        c = next();
      } while (Character.isWhitespace(c));
      switch (c) {
        case 0:
          throw syntaxError("Misshaped element");
        case '<':
          throw syntaxError("Misplaced '<'");
        case '>':
          return GT;
        case '/':
          return SLASH;
        case '=':
          return EQ;
        case '!':
          return BANG;
        case '?':
          return QUEST;

        // Quoted string

        case '"':
        case '\'':
          q = c;
          sb = new StringBuilder();
          for (; ; ) {
            c = next();
            if (c == 0) {
              throw syntaxError("Unterminated string");
            }
            if (c == q) {
              return sb.toString();
            }
            if (c == '&') {
              sb.append(nextEntity(c));
            } else {
              sb.append(c);
            }
          }
        default:

          // Name

          sb = new StringBuilder();
          for (; ; ) {
            sb.append(c);
            c = next();
            if (Character.isWhitespace(c)) {
              return sb.toString();
            }
            switch (c) {
              case 0:
                return sb.toString();
              case '>':
              case '/':
              case '=':
              case '!':
              case '?':
              case '[':
              case ']':
                back();
                return sb.toString();
              case '<':
              case '"':
              case '\'':
                throw syntaxError("Bad character in a name");
            }
          }
      }
    }


    /**
     * Skip characters until past the requested string.
     * If it is not found, we are left at the end of the source with a result
     * of false.
     *
     * @param to A string to skip past.
     * @throws Fault
     */
    public boolean skipPast(String to) throws Fault {
      boolean b;
      char c;
      int i;
      int j;
      int offset = 0;
      int length = to.length();
      char[] circle = new char[length];

              /*
               * First fill the circle buffer with as many characters as are
               * in the
               * to string. If we reach an early end, bail.
               */

      for (i = 0; i < length; i += 1) {
        c = next();
        if (c == 0) {
          return false;
        }
        circle[i] = c;
      }

              /* We will loop, possibly for all of the remaining characters. */

      for (; ; ) {
        j = offset;
        b = true;

                  /* Compare the circle buffer with the to string. */

        for (i = 0; i < length; i += 1) {
          if (circle[j] != to.charAt(i)) {
            b = false;
            break;
          }
          j += 1;
          if (j >= length) {
            j -= length;
          }
        }

                  /* If we exit the loop with b intact, then victory is ours. */

        if (b) {
          return true;
        }

                  /* Get the next character. If there isn't one, then defeat
                  is ours. */

        c = next();
        if (c == 0) {
          return false;
        }
                  /*
                   * Shove the character in the circle buffer and advance the
                   * circle offset. The offset is mod n.
                   */
        circle[offset] = c;
        offset += 1;
        if (offset >= length) {
          offset -= length;
        }
      }
    }
  }

  /**
   * This provides static methods to convert an XML text into a VariantList or
   * Variant, and to covert a VariantList or Variant into an XML text using
   * the JsonML transform.    [ ^ backwards^^logic    ^ ]
   *
   * @author JSON.org
   * @version 2016-01-30
   */
  public static class Kit {
    /**
     * Parse XML values and store them in a VariantList.
     *
     * @param x           The Compiler containing the source string.
     * @param arrayForm   true if array form, false if object form.
     * @param ja          The VariantList that is containing the current tag
     *                    or null
     *                    if we are at the outermost level.
     * @param keepStrings Don't type-convert text nodes and attribute values
     * @return A VariantList if the value is the outermost tag, otherwise null.
     * @throws Fault
     */
    private static Object parse(
      Compiler x,
      boolean arrayForm,
      VariantList ja,
      boolean keepStrings
    ) throws Fault
    {
      String attribute;
      char c;
      String closeTag = null;
      int i;
      VariantList newja = null;
      Variant newjo = null;
      Object token;
      String tagName = null;

      // Test for and skip past these forms:
      //      <!-- ... -->
      //      <![  ... ]]>
      //      <!   ...   >
      //      <?   ...  ?>

      while (true) {
        if (!x.more()) {
          throw x.syntaxError("Bad XML");
        }
        token = x.nextContent();
        if (token == LT) {
          token = x.nextToken();
          if (token instanceof Character) {
            if (token == SLASH) {

              // Close tag </

              token = x.nextToken();
              if (!(token instanceof String)) {
                throw new Fault(
                  "Expected a closing name instead of '" +
                    token + "'.");
              }
              if (x.nextToken() != GT) {
                throw x.syntaxError("Misshaped close tag");
              }
              return token;
            } else if (token == BANG) {

              // <!

              c = x.next();
              if (c == '-') {
                if (x.next() == '-') {
                  x.skipPast("-->");
                } else {
                  x.back();
                }
              } else if (c == '[') {
                token = x.nextToken();
                if (token.equals("CDATA") && x.next() == '[') {
                  if (ja != null) {
                    ja.put(x.nextCDATA());
                  }
                } else {
                  throw x.syntaxError("Expected 'CDATA['");
                }
              } else {
                i = 1;
                do {
                  token = x.nextMeta();
                  if (token == null) {
                    throw x.syntaxError("Missing '>' after '<!'.");
                  } else if (token == LT) {
                    i += 1;
                  } else if (token == GT) {
                    i -= 1;
                  }
                } while (i > 0);
              }
            } else if (token == QUEST) {

              // <?

              x.skipPast("?>");
            } else {
              throw x.syntaxError("Misshaped tag");
            }

            // Open tag <

          } else {
            if (!(token instanceof String)) {
              throw x.syntaxError("Bad tagName '" + token + "'.");
            }
            tagName = (String) token;
            newja = new VariantList();
            newjo = new Variant();
            if (arrayForm) {
              newja.put(tagName);
              if (ja != null) {
                ja.put(newja);
              }
            } else {
              newjo.put("tagName", tagName);
              if (ja != null) {
                ja.put(newjo);
              }
            }
            token = null;
            for (; ; ) {
              if (token == null) {
                token = x.nextToken();
              }
              if (token == null) {
                throw x.syntaxError("Misshaped tag");
              }
              if (!(token instanceof String)) {
                break;
              }

              // attribute = value

              attribute = (String) token;
              if (!arrayForm && ("tagName".equals(attribute) || "childNode"
                .equals(attribute))) {
                throw x.syntaxError("Reserved attribute.");
              }
              token = x.nextToken();
              if (token == EQ) {
                token = x.nextToken();
                if (!(token instanceof String)) {
                  throw x.syntaxError("Missing value");
                }
                newjo.accumulate(
                  attribute,
                  keepStrings ? ((String) token) : stringToValue((String) token)
                );
                token = null;
              } else {
                newjo.accumulate(attribute, "");
              }
            }
            if (arrayForm && newjo.length() > 0) {
              newja.put(newjo);
            }

            // Empty tag <.../>

            if (token == SLASH) {
              if (x.nextToken() != GT) {
                throw x.syntaxError("Misshaped tag");
              }
              if (ja == null) {
                if (arrayForm) {
                  return newja;
                }
                return newjo;
              }

              // Content, between <...> and </...>

            } else {
              if (token != GT) {
                throw x.syntaxError("Misshaped tag");
              }
              closeTag = (String) parse(x, arrayForm, newja,
                keepStrings
              );
              if (closeTag != null) {
                if (!closeTag.equals(tagName)) {
                  throw x.syntaxError("Mismatched '" +
                    tagName +
                    "' and '" + closeTag + "'");
                }
                tagName = null;
                if (!arrayForm && newja.length() > 0) {
                  newjo.put("childNodes", newja);
                }
                if (ja == null) {
                  if (arrayForm) {
                    return newja;
                  }
                  return newjo;
                }
              }
            }
          }
        } else {
          if (ja != null) {
            ja.put(token instanceof String
              ? keepStrings ? unescape((String) token) :
              stringToValue((String) token)
              : token);
          }
        }
      }
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string
     * into a
     * VariantList using the JsonML transform. Each XML tag is
     * represented as
     * a VariantList in which the first element is the tag name. If
     * the tag has
     * attributes, then the second element will be Variant containing
     * the
     * name/value pairs. If the tag contains children, then strings and
     * JSONElementss will represent the child tags.
     * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
     *
     * @param string The source string.
     * @return A VariantList containing the structured data from the
     * XML string.
     * @throws Fault Thrown on error converting to a VariantList
     */
    public static VariantList toJSONElements(String string)
      throws Fault
    {
      return (VariantList) parse(new Compiler(string), true,
        null, false
      );
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string
     * into a
     * VariantList using the JsonML transform. Each XML tag is
     * represented as
     * a VariantList in which the first element is the tag name. If
     * the tag has
     * attributes, then the second element will be Variant containing
     * the
     * name/value pairs. If the tag contains children, then strings and
     * JSONElementss will represent the child tags.
     * As opposed to toJSONElements this method does not attempt to
     * convert
     * any text node or attribute value to any type
     * but just leaves it as a string.
     * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are
     * ignored.
     *
     * @param string      The source string.
     * @param keepStrings If true, then values will not be coerced
     *                    into boolean
     *                    or numeric values and will instead be left
     *                    as strings
     * @return A VariantList containing the structured data from the
     * XML string.
     * @throws Fault Thrown on error converting to a VariantList
     */
    public static VariantList toJSONElements(String string,
      boolean keepStrings) throws Fault
    {
      return (VariantList) parse(new Compiler(string), true,
        null, keepStrings
      );
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string
     * into a
     * VariantList using the JsonML transform. Each XML tag is
     * represented as
     * a VariantList in which the first element is the tag name. If
     * the tag has
     * attributes, then the second element will be Variant containing
     * the
     * name/value pairs. If the tag contains children, then strings and
     * JSONElementss will represent the child content and tags.
     * As opposed to toJSONElements this method does not attempt to
     * convert
     * any text node or attribute value to any type
     * but just leaves it as a string.
     * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are
     * ignored.
     *
     * @param x           An Compiler.
     * @param keepStrings If true, then values will not be coerced
     *                    into boolean
     *                    or numeric values and will instead be left
     *                    as strings
     * @return A VariantList containing the structured data from the
     * XML string.
     * @throws Fault Thrown on error converting to a VariantList
     */
    public static VariantList toJSONElements(Compiler x, boolean
      keepStrings) throws Fault
    {
      return (VariantList) parse(x, true, null, keepStrings);
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string
     * into a
     * VariantList using the JsonML transform. Each XML tag is
     * represented as
     * a VariantList in which the first element is the tag name. If
     * the tag has
     * attributes, then the second element will be Variant containing
     * the
     * name/value pairs. If the tag contains children, then strings and
     * JSONElementss will represent the child content and tags.
     * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are
     * ignored.
     *
     * @param x An Compiler.
     * @return A VariantList containing the structured data from the
     * XML string.
     * @throws Fault Thrown on error converting to a VariantList
     */
    public static VariantList toJSONElements(Compiler x) throws
      Fault
    {
      return (VariantList) parse(x, true, null, false);
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string
     * into a
     * Variant using the JsonML transform. Each XML tag is
     * represented as
     * a Variant with a "tagName" property. If the tag has attributes, then
     * the attributes will be in the Variant as properties. If the tag
     * contains children, the object will have a "childNodes"
     * property which
     * will be an array of strings and JsonML VariantList.
     * <p>
     * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are
     * ignored.
     *
     * @param string The XML source text.
     * @return A Variant containing the structured data from the XML
     * string.
     * @throws Fault Thrown on error converting to a Variant
     */
    public static Variant toJSONValue(String string) throws Fault {
      return (Variant) parse(new Compiler(string), false, null,
        false
      );
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string
     * into a
     * Variant using the JsonML transform. Each XML tag is represented as
     * a Variant with a "tagName" property. If the tag has attributes, then
     * the attributes will be in the Variant as properties. If the tag
     * contains children, the object will have a "childNodes"
     * property which
     * will be an array of strings and JsonML VariantList.
     * <p>
     * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
     *
     * @param string      The XML source text.
     * @param keepStrings If true, then values will not be coerced into boolean
     *                    or numeric values and will instead be left as strings
     * @return A Variant containing the structured data from the XML string.
     * @throws Fault Thrown on error converting to a Variant
     */
    public static Variant toJSONValue(String string,
      boolean keepStrings) throws Fault
    {
      return (Variant) parse(
        new Compiler(string), false, null, keepStrings);
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string into a
     * Variant using the JsonML transform. Each XML tag is represented as
     * a Variant with a "tagName" property. If the tag has attributes, then
     * the attributes will be in the Variant as properties. If the tag
     * contains children, the object will have a "childNodes" property which
     * will be an array of strings and JsonML VariantList.
     * <p>
     * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
     *
     * @param x An Compiler of the XML source text.
     * @return A Variant containing the structured data from the XML string.
     * @throws Fault Thrown on error converting to a Variant
     */
    public static Variant toJSONValue(Compiler x) throws Fault {
      return (Variant) parse(x, false, null, false);
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string into a
     * Variant using the JsonML transform. Each XML tag is represented as
     * a Variant with a "tagName" property. If the tag has attributes, then
     * the attributes will be in the Variant as properties. If the tag
     * contains children, the object will have a "childNodes" property which
     * will be an array of strings and JsonML VariantList.
     * <p>
     * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
     *
     * @param x           An Compiler of the XML source text.
     * @param keepStrings If true, then values will not be coerced into boolean
     *                    or numeric values and will instead be left as strings
     * @return A Variant containing the structured data from the XML string.
     * @throws Fault Thrown on error converting to a Variant
     */
    public static Variant toJSONValue(Compiler x, boolean
      keepStrings) throws Fault
    {
      return (Variant) parse(x, false, null, keepStrings);
    }


    /**
     * Reverse the Kit transformation, making an XML text from a
     * VariantList.
     *
     * @param ja A VariantList.
     * @return An XML string.
     * @throws Fault Thrown on error converting to a string
     */
    public static String toString(VariantList ja) throws Fault {
      int i;
      Variant jo;
      int length;
      Object object;
      StringBuilder sb = new StringBuilder();
      String tagName;

      // Emit <tagName

      tagName = ja.getString(0);
      noSpace(tagName);
      tagName = escape(tagName);
      sb.append('<');
      sb.append(tagName);

      object = ja.opt(1);
      if (object instanceof Variant) {
        i = 2;
        jo = (Variant) object;

        // Emit the attributes

        for (final Map.Entry<String, ?> entry : jo.entrySet()) {
          final String key = entry.getKey();
          noSpace(key);
          final Object value = entry.getValue();
          if (value != null) {
            sb.append(' ');
            sb.append(escape(key));
            sb.append('=');
            sb.append('"');
            sb.append(escape(value.toString()));
            sb.append('"');
          }
        }
      } else {
        i = 1;
      }

      // Emit content in body

      length = ja.length();
      if (i >= length) {
        sb.append('/');
        sb.append('>');
      } else {
        sb.append('>');
        do {
          object = ja.get(i);
          i += 1;
          if (object != null) {
            if (object instanceof String) {
              sb.append(escape(object.toString()));
            } else if (object instanceof Variant) {
              sb.append(toString((Variant) object));
            } else if (object instanceof VariantList) {
              sb.append(toString((VariantList) object));
            } else {
              sb.append(object.toString());
            }
          }
        } while (i < length);
        sb.append('<');
        sb.append('/');
        sb.append(tagName);
        sb.append('>');
      }
      return sb.toString();
    }

    /**
     * Reverse the Kit transformation, making an XML text from a Variant.
     * The Variant must contain a "tagName" property. If it has children,
     * then it must have a "childNodes" property containing an array of objects.
     * The other properties are attributes with string values.
     *
     * @param jo A Variant.
     * @return An XML string.
     * @throws Fault Thrown on error converting to a string
     */
    public static String toString(Variant jo) throws Fault {
      StringBuilder sb = new StringBuilder();
      int i;
      VariantList ja;
      int length;
      Object object;
      String tagName;
      Object value;

      //Emit <tagName

      tagName = jo.optString("tagName");
      if (tagName == null) {
        return escape(jo.toString());
      }
      noSpace(tagName);
      tagName = escape(tagName);
      sb.append('<');
      sb.append(tagName);

      //Emit the attributes

      for (final Map.Entry<String, ?> entry : jo.entrySet()) {
        final String key = entry.getKey();
        if (!"tagName".equals(key) && !"childNodes".equals(key)) {
          noSpace(key);
          value = entry.getValue();
          if (value != null) {
            sb.append(' ');
            sb.append(escape(key));
            sb.append('=');
            sb.append('"');
            sb.append(escape(value.toString()));
            sb.append('"');
          }
        }
      }

      //Emit content in body

      ja = jo.optJSONElements("childNodes");
      if (ja == null) {
        sb.append('/');
        sb.append('>');
      } else {
        sb.append('>');
        length = ja.length();
        for (i = 0; i < length; i += 1) {
          object = ja.get(i);
          if (object != null) {
            if (object instanceof String) {
              sb.append(escape(object.toString()));
            } else if (object instanceof Variant) {
              sb.append(toString((Variant) object));
            } else if (object instanceof VariantList) {
              sb.append(toString((VariantList) object));
            } else {
              sb.append(object.toString());
            }
          }
        }
        sb.append('<');
        sb.append('/');
        sb.append(tagName);
        sb.append('>');
      }
      return sb.toString();
    }
  }
}