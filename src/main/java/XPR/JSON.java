package XPR;

import java.io.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

import static java.lang.String.format;

public class JSON {

    /**
   * Composer provides a quick and convenient way of producing JSON text.
   * The texts produced strictly conform to JSON syntax rules. No whitespace is
   * added, so the results are ready for transmission or storage. Each instance of
   * Composer can produce one JSON text.
   * <p>
   * A Composer instance provides a <code>value</code> method for appending
   * values to the
   * text, and a <code>key</code>
   * method for adding keys before values in objects. There are <code>array</code>
   * and <code>endArray</code> methods that make and bound array values, and
   * <code>object</code> and <code>endObject</code> methods which make and bound
   * object values. All of these methods return the Composer instance,
   * permitting a cascade style. For example, <pre>
   * new Composer(myWriter)
   *     .object()
   *         .key("JSON")
   *         .value("Hello, World!")
   *     .endObject();</pre> which writes <pre>
   * {"JSON":"Hello, World!"}</pre>
   * <p>
   * The first method called must be <code>array</code> or <code>object</code>.
   * There are no methods for adding commas or colons. Composer adds them for
   * you. Objects and arrays can be nested up to 200 levels deep.
   * <p>
   * This can sometimes be easier than using a Variant to build a string.
   * @author JSON.org
   * @version 2016-08-08
   */
  public static class Composer {
      private static final int maxdepth = 200;

      /**
       * The comma flag determines if a comma should be output before the next
       * value.
       */
      private boolean comma;

      /**
       * The current mode. Values:
       * 'a' (array),
       * 'd' (done),
       * 'i' (initial),
       * 'k' (key),
       * 'o' (object).
       */
      protected char mode;

      /**
       * The object/array stack.
       */
      private final Type.Variant stack[];

      /**
       * The stack top index. A value of 0 indicates that the stack is empty.
       */
      private int top;

      /**
       * The writer that will receive the output.
       */
      protected Appendable writer;

      /**
       * Make a fresh Composer. It can be used to build one JSON text.
       */
      public Composer(Appendable w) {
          this.comma = false;
          this.mode = 'i';
          this.stack = new Type.Variant[maxdepth];
          this.top = 0;
          this.writer = w;
      }

      /**
       * Append a value.
       * @param string A string value.
       * @return this
       * @throws Fault If the value is out of sequence.
       */
      private Composer append(String string) throws Fault {
          if (string == null) {
              throw new Fault("Null pointer");
          }
          if (this.mode == 'o' || this.mode == 'a') {
              try {
                  if (this.comma && this.mode == 'a') {
                      this.writer.append(',');
                  }
                  this.writer.append(string);
              } catch (IOException e) {
                  throw new Fault(e);
              }
              if (this.mode == 'o') {
                  this.mode = 'k';
              }
              this.comma = true;
              return this;
          }
          throw new Fault("Value out of sequence.");
      }

      /**
       * Begin appending a new array. All values until the balancing
       * <code>endArray</code> will be appended to this array. The
       * <code>endArray</code> method must be called to mark the array's end.
       * @return this
       * @throws Fault If the nesting is too deep, or if the object is
       * started in the wrong place (for example as a key or after the end of the
       * outermost array or object).
       */
      public Composer array() throws Fault {
          if (this.mode == 'i' || this.mode == 'o' || this.mode == 'a') {
              this.push(null);
              this.append("[");
              this.comma = false;
              return this;
          }
          throw new Fault("Misplaced array.");
      }

      /**
       * End something.
       * @param m Mode
       * @param c Closing character
       * @return this
       * @throws Fault If unbalanced.
       */
      private Composer end(char m, char c) throws Fault {
          if (this.mode != m) {
              throw new Fault(m == 'a'
                  ? "Misplaced endArray."
                  : "Misplaced endObject.");
          }
          this.pop(m);
          try {
              this.writer.append(c);
          } catch (IOException e) {
              throw new Fault(e);
          }
          this.comma = true;
          return this;
      }

      /**
       * End an array. This method most be called to balance calls to
       * <code>array</code>.
       * @return this
       * @throws Fault If incorrectly nested.
       */
      public Composer endArray() throws Fault {
          return this.end('a', ']');
      }

      /**
       * End an object. This method most be called to balance calls to
       * <code>object</code>.
       * @return this
       * @throws Fault If incorrectly nested.
       */
      public Composer endObject() throws Fault {
          return this.end('k', '}');
      }

      /**
       * Append a key. The key will be associated with the next value. In an
       * object, every value must be preceded by a key.
       * @param string A key string.
       * @return this
       * @throws Fault If the key is out of place. For example, keys
       *  do not belong in arrays or if the key is null.
       */
      public Composer key(String string) throws Fault {
          if (string == null) {
              throw new Fault("Null key.");
          }
          if (this.mode == 'k') {
              try {
                  this.stack[this.top - 1].putOnce(string, Boolean.TRUE);
                  if (this.comma) {
                      this.writer.append(',');
                  }
                  this.writer.append(Type.Variant.quote(string));
                  this.writer.append(':');
                  this.comma = false;
                  this.mode = 'o';
                  return this;
              } catch (IOException e) {
                  throw new Fault(e);
              }
          }
          throw new Fault("Misplaced key.");
      }


      /**
       * Begin appending a new object. All keys and values until the balancing
       * <code>endObject</code> will be appended to this object. The
       * <code>endObject</code> method must be called to mark the object's end.
       * @return this
       * @throws Fault If the nesting is too deep, or if the object is
       * started in the wrong place (for example as a key or after the end of the
       * outermost array or object).
       */
      public Composer object() throws Fault {
          if (this.mode == 'i') {
              this.mode = 'o';
          }
          if (this.mode == 'o' || this.mode == 'a') {
              this.append("{");
              this.push(new Type.Variant());
              this.comma = false;
              return this;
          }
          throw new Fault("Misplaced object.");

      }


      /**
       * Pop an array or object scope.
       * @param c The scope to close.
       * @throws Fault If nesting is wrong.
       */
      private void pop(char c) throws Fault {
          if (this.top <= 0) {
              throw new Fault("Nesting error.");
          }
          char m = this.stack[this.top - 1] == null ? 'a' : 'k';
          if (m != c) {
              throw new Fault("Nesting error.");
          }
          this.top -= 1;
          this.mode = this.top == 0
              ? 'd'
              : this.stack[this.top - 1] == null
              ? 'a'
              : 'k';
      }

      /**
       * Push an array or object scope.
       * @param jo The scope to open.
       * @throws Fault If nesting is too deep.
       */
      private void push(Type.Variant jo) throws Fault {
          if (this.top >= maxdepth) {
              throw new Fault("Nesting too deep.");
          }
          this.stack[this.top] = jo;
          this.mode = jo == null ? 'a' : 'k';
          this.top += 1;
      }


      /**
       * Append either the value <code>true</code> or the value
       * <code>false</code>.
       * @param b A boolean.
       * @return this
       * @throws Fault
       */
      public Composer value(boolean b) throws Fault {
          return this.append(b ? "true" : "false");
      }

      /**
       * Append a double value.
       * @param d A double.
       * @return this
       * @throws Fault If the number is not finite.
       */
      public Composer value(double d) throws Fault {
          return this.value(new Double(d));
      }

      /**
       * Append a long value.
       * @param l A long.
       * @return this
       * @throws Fault
       */
      public Composer value(long l) throws Fault {
          return this.append(Long.toString(l));
      }


      /**
       * Append an object value.
       * @param object The object to add. It can be null, or a Boolean, Number,
       *   String, Variant, or VariantList, or an object that implements Serialization.
       * @return this
       * @throws Fault If the value is out of sequence.
       */
      public Composer value(Object object) throws Fault {
          return this.append(Type.Variant.valueToString(object));
      }
  }

    /**
   * The <code>Serialization</code> interface allows a <code>toJSON()</code>
   * method so that a class can change the behavior of
   * <code>Variant.toString()</code>, <code>VariantList.toString()</code>,
   * and <code>Composer.value(</code>Object<code>)</code>. The
   * <code>toJSON</code> method will be used instead of the default behavior
   * of using the Object's <code>toString()</code> method and quoting the result.
   */
  public static interface Serialization {
      /**
       * The <code>toJSON</code> method allows a class to produce its own JSON
       * serialization.
       *
       * @return A strictly syntactically correct JSON text.
       */
      public String toJSON();
  }

  /**
   * Builder provides a quick and convenient way of producing JSON text.
   * The texts produced strictly conform to JSON syntax rules. No whitespace is
   * added, so the results are ready for transmission or storage. Each instance of
   * Builder can produce one JSON text.
   * <p>
   * A Builder instance provides a <code>value</code> method for appending
   * values to the
   * text, and a <code>key</code>
   * method for adding keys before values in objects. There are <code>array</code>
   * and <code>endArray</code> methods that make and bound array values, and
   * <code>object</code> and <code>endObject</code> methods which make and bound
   * object values. All of these methods return the Composer instance,
   * permitting cascade style. For example, <pre>
   * myString = new Builder()
   *     .object()
   *         .key("JSON")
   *         .value("Hello, World!")
   *     .endObject()
   *     .toString();</pre> which produces the string <pre>
   * {"JSON":"Hello, World!"}</pre>
   * <p>
   * The first method called must be <code>array</code> or <code>object</code>.
   * There are no methods for adding commas or colons. Builder adds them for
   * you. Objects and arrays can be nested up to 20 levels deep.
   * <p>
   * This can sometimes be easier than using a Variant to build a string.
   * @author JSON.org
   * @version 2015-12-09
   */
  public static class Builder extends Composer {
      /**
       * Make a fresh Builder. It can be used to build one JSON text.
       */
      public Builder() {
          super(new StringWriter());
      }

      /**
       * Return the JSON text. This method is used to obtain the product of the
       * Builder instance. It will return <code>null</code> if there was a
       * problem in the construction of the JSON text (such as the calls to
       * <code>array</code> were not properly balanced with calls to
       * <code>endArray</code>).
       * @return The JSON text.
       */
      @Override
      public String toString() {
          return this.mode == 'd' ? this.writer.toString() : null;
      }
  }

    public static class Type {
        /**
         * A VariantList is an ordered sequence of values. Its external text form is a
         * string wrapped in square brackets with commas separating the values. The
         * internal form is an object having <code>getKey</code> and <code>opt</code>
         * methods for accessing the values by index, and <code>put</code> methods for
         * adding or replacing values. The values can be any of these types:
         * <code>Boolean</code>, <code>VariantList</code>, <code>Variant</code>,
         * <code>Number</code>, <code>String</code>, or the
         * <code>Variant.NULL object</code>.
         * <p>
         * The constructor can convert a JSON text into a Java object. The
         * <code>toString</code> method converts to JSON text.
         * <p>
         * A <code>getKey</code> method returns a value if one can be found, and throws an
         * exception if one cannot be found. An <code>opt</code> method returns a
         * default value instead of throwing an exception, and so is useful for
         * obtaining optional values.
         * <p>
         * The generic <code>getKey()</code> and <code>opt()</code> methods return an
         * object which you can cast or query for type. There are also typed
         * <code>getKey</code> and <code>opt</code> methods that do type checking and type
         * coercion for you.
         * <p>
         * The texts produced by the <code>toString</code> methods strictly conform to
         * JSON syntax rules. The constructors are more forgiving in the texts they will
         * accept:
         * <ul>
         * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just
         * before the closing bracket.</li>
         * <li>The <code>null</code> value will be inserted when there is <code>,</code>
         * &nbsp;<small>(comma)</small> elision.</li>
         * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single
         * quote)</small>.</li>
         * <li>Strings do not need to be quoted at all if they do not begin with a quote
         * or single quote, and if they do not contain leading or trailing spaces, and
         * if they do not contain any of these characters:
         * <code>{ } [ ] / \ : , #</code> and if they do not look like numbers and
         * if they are not the reserved words <code>true</code>, <code>false</code>, or
         * <code>null</code>.</li>
         * </ul>
         *
         * @author JSON.org
         * @version 2016-08/15
         */
        public static class VariantList implements Iterable<Object> {

            /**
             * The java ArrayList where the JSON.VariantList values are kept.
             */
            private final ArrayList<Object> myArrayList;

            /**
             * Construct an empty VariantList.
             */
            public VariantList() {
                this.myArrayList = new ArrayList<Object>();
            }

            /**
             * Construct a VariantList from a Compiler.
             *
             * @param x
             *            A Compiler
             * @throws Fault
             *             If there is a syntax error.
             */
            public VariantList(Compiler x) throws Fault {
                this();
                if (x.nextClean() != '[') {
                    throw x.syntaxError("A VariantList text must start with '['");
                }
                if (x.nextClean() != ']') {
                    x.back();
                    for (;;) {
                        if (x.nextClean() == ',') {
                            x.back();
                            this.myArrayList.add(Variant.NULL);
                        } else {
                            x.back();
                            this.myArrayList.add(x.nextValue());
                        }
                        switch (x.nextClean()) {
                        case ',':
                            if (x.nextClean() == ']') {
                                return;
                            }
                            x.back();
                            break;
                        case ']':
                            return;
                        default:
                            throw x.syntaxError("Expected a ',' or ']'");
                        }
                    }
                }
            }

            /**
             * Construct a VariantList from a source JSON text.
             *
             * @param source
             *            A string that begins with <code>[</code>&nbsp;<small>(left
             *            bracket)</small> and ends with <code>]</code>
             *            &nbsp;<small>(right bracket)</small>.
             * @throws Fault
             *             If there is a syntax error.
             */
            public VariantList(String source) throws Fault {
                this(new Compiler(source));
            }

            /**
             * Construct a VariantList from a Collection.
             *
             * @param collection
             *            A Collection.
             */
            public VariantList(Collection<?> collection) {
                if (collection == null) {
                    this.myArrayList = new ArrayList<Object>();
                } else {
                    this.myArrayList = new ArrayList<Object>(collection.size());
                  for (Object o: collection){
                    this.myArrayList.add(Variant.wrap(o));
                  }
                }
            }

            /**
             * Construct a VariantList from an array
             *
             * @throws Fault
             *             If not an array.
             */
            public VariantList(Object array) throws Fault {
                this();
                if (array.getClass().isArray()) {
                    int length = Array.getLength(array);
                    this.myArrayList.ensureCapacity(length);
                    for (int i = 0; i < length; i += 1) {
                        this.put(Variant.wrap(Array.get(array, i)));
                    }
                } else {
                    throw new Fault(
                            "VariantList initial value should be a string or collection or array.");
                }
            }

            @Override
            public Iterator<Object> iterator() {
                return this.myArrayList.iterator();
            }

            /**
             * Get the object value associated with an index.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return An object value.
             * @throws Fault
             *             If there is no value for the index.
             */
            public Object get(int index) throws Fault {
                Object object = this.opt(index);
                if (object == null) {
                    throw new Fault("VariantList[" + index + "] not found.");
                }
                return object;
            }

            /**
             * Get the boolean value associated with an index. The string values "true"
             * and "false" are converted to boolean.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return The truth.
             * @throws Fault
             *             If there is no value for the index or if the value is not
             *             convertible to boolean.
             */
            public boolean getBoolean(int index) throws Fault {
                Object object = this.get(index);
                if (object.equals(Boolean.FALSE)
                        || (object instanceof String && ((String) object)
                                .equalsIgnoreCase("false"))) {
                    return false;
                } else if (object.equals(Boolean.TRUE)
                        || (object instanceof String && ((String) object)
                                .equalsIgnoreCase("true"))) {
                    return true;
                }
                throw new Fault("VariantList[" + index + "] is not a boolean.");
            }

            /**
             * Get the double value associated with an index.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return The value.
             * @throws Fault
             *             If the key is not found or if the value cannot be converted
             *             to a number.
             */
            public double getDouble(int index) throws Fault {
                Object object = this.get(index);
                try {
                    return object instanceof Number ? ((Number) object).doubleValue()
                            : Double.parseDouble((String) object);
                } catch (Exception e) {
                    throw new Fault("VariantList[" + index + "] is not a number.", e);
                }
            }

            /**
             * Get the float value associated with a key.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return The numeric value.
             * @throws Fault
             *             if the key is not found or if the value is not a Number
             *             object and cannot be converted to a number.
             */
            public float getFloat(int index) throws Fault {
                Object object = this.get(index);
                try {
                    return object instanceof Number ? ((Number) object).floatValue()
                            : Float.parseFloat(object.toString());
                } catch (Exception e) {
                    throw new Fault("VariantList[" + index
                            + "] is not a number.", e);
                }
            }

            /**
             * Get the Number value associated with a key.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return The numeric value.
             * @throws Fault
             *             if the key is not found or if the value is not a Number
             *             object and cannot be converted to a number.
             */
            public Number getNumber(int index) throws Fault {
                Object object = this.get(index);
                try {
                    if (object instanceof Number) {
                        return (Number)object;
                    }
                    return Variant.stringToNumber(object.toString());
                } catch (Exception e) {
                    throw new Fault("VariantList[" + index + "] is not a number.", e);
                }
            }

            /**
            * Get the enum value associated with an index.
            *
            * @param clazz
            *            The type of enum to retrieve.
            * @param index
            *            The index must be between 0 and length() - 1.
            * @return The enum value at the index location
            * @throws Fault
            *            if the key is not found or if the value cannot be converted
            *            to an enum.
            */
            public <E extends Enum<E>> E getEnum(Class<E> clazz, int index) throws Fault {
                E val = optEnum(clazz, index);
                if(val==null) {
                    // Fault should really take a throwable argument.
                    // If it did, I would re-implement this with the Enum.valueOf
                    // method and place any thrown exception in the Fault
                    throw new Fault("VariantList[" + index + "] is not an enum of type "
                            + Variant.quote(clazz.getSimpleName()) + ".");
                }
                return val;
            }

            /**
             * Get the BigDecimal value associated with an index.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return The value.
             * @throws Fault
             *             If the key is not found or if the value cannot be converted
             *             to a BigDecimal.
             */
            public BigDecimal getBigDecimal (int index) throws Fault {
                Object object = this.get(index);
                try {
                    return new BigDecimal(object.toString());
                } catch (Exception e) {
                    throw new Fault("VariantList[" + index +
                            "] could not convert to BigDecimal.", e);
                }
            }

            /**
             * Get the BigInteger value associated with an index.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return The value.
             * @throws Fault
             *             If the key is not found or if the value cannot be converted
             *             to a BigInteger.
             */
            public BigInteger getBigInteger (int index) throws Fault {
                Object object = this.get(index);
                try {
                    return new BigInteger(object.toString());
                } catch (Exception e) {
                    throw new Fault("VariantList[" + index +
                            "] could not convert to BigInteger.", e);
                }
            }

            /**
             * Get the int value associated with an index.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return The value.
             * @throws Fault
             *             If the key is not found or if the value is not a number.
             */
            public int getInt(int index) throws Fault {
                Object object = this.get(index);
                try {
                    return object instanceof Number ? ((Number) object).intValue()
                            : Integer.parseInt((String) object);
                } catch (Exception e) {
                    throw new Fault("VariantList[" + index + "] is not a number.", e);
                }
            }

            /**
             * Get the VariantList associated with an index.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return A VariantList value.
             * @throws Fault
             *             If there is no value for the index. or if the value is not a
             *             VariantList
             */
            public VariantList getJSONElements(int index) throws Fault {
                Object object = this.get(index);
                if (object instanceof VariantList) {
                    return (VariantList) object;
                }
                throw new Fault("VariantList[" + index + "] is not a VariantList.");
            }

            /**
             * Get the Variant associated with an index.
             *
             * @param index
             *            subscript
             * @return A Variant value.
             * @throws Fault
             *             If there is no value for the index or if the value is not a
             *             Variant
             */
            public Variant getJSONValue(int index) throws Fault {
                Object object = this.get(index);
                if (object instanceof Variant) {
                    return (Variant) object;
                }
                throw new Fault("VariantList[" + index + "] is not a Variant.");
            }

            /**
             * Get the long value associated with an index.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return The value.
             * @throws Fault
             *             If the key is not found or if the value cannot be converted
             *             to a number.
             */
            public long getLong(int index) throws Fault {
                Object object = this.get(index);
                try {
                    return object instanceof Number ? ((Number) object).longValue()
                            : Long.parseLong((String) object);
                } catch (Exception e) {
                    throw new Fault("VariantList[" + index + "] is not a number.", e);
                }
            }

            /**
             * Get the string associated with an index.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return A string value.
             * @throws Fault
             *             If there is no string value for the index.
             */
            public String getString(int index) throws Fault {
                Object object = this.get(index);
                if (object instanceof String) {
                    return (String) object;
                }
                throw new Fault("VariantList[" + index + "] not a string.");
            }

            /**
             * Determine if the value is null.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return true if the value at the index is null, or if there is no value.
             */
            public boolean isNull(int index) {
                return Variant.NULL.equals(this.opt(index));
            }

            /**
             * Make a string from the contents of this VariantList. The
             * <code>separator</code> string is inserted between each element. Warning:
             * This method assumes that the data structure is acyclical.
             *
             * @param separator
             *            A string that will be inserted between the elements.
             * @return a string.
             * @throws Fault
             *             If the array contains an invalid number.
             */
            public String join(String separator) throws Fault {
                int len = this.length();
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < len; i += 1) {
                    if (i > 0) {
                        sb.append(separator);
                    }
                    sb.append(Variant.valueToString(this.myArrayList.get(i)));
                }
                return sb.toString();
            }

            /**
             * Get the number of elements in the VariantList, included nulls.
             *
             * @return The length (or size).
             */
            public int length() {
                return this.myArrayList.size();
            }

            /**
             * Get the optional object value associated with an index.
             *
             * @param index
             *            The index must be between 0 and length() - 1. If not, null is returned.
             * @return An object value, or null if there is no object at that index.
             */
            public Object opt(int index) {
                return (index < 0 || index >= this.length()) ? null : this.myArrayList
                        .get(index);
            }

            /**
             * Get the optional boolean value associated with an index. It returns false
             * if there is no value at that index, or if the value is not Boolean.TRUE
             * or the String "true".
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return The truth.
             */
            public boolean optBoolean(int index) {
                return this.optBoolean(index, false);
            }

            /**
             * Get the optional boolean value associated with an index. It returns the
             * defaultValue if there is no value at that index or if it is not a Boolean
             * or the String "true" or "false" (case insensitive).
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @param defaultValue
             *            A boolean default.
             * @return The truth.
             */
            public boolean optBoolean(int index, boolean defaultValue) {
                try {
                    return this.getBoolean(index);
                } catch (Exception e) {
                    return defaultValue;
                }
            }

            /**
             * Get the optional double value associated with an index. NaN is returned
             * if there is no value for the index, or if the value is not a number and
             * cannot be converted to a number.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return The value.
             */
            public double optDouble(int index) {
                return this.optDouble(index, Double.NaN);
            }

            /**
             * Get the optional double value associated with an index. The defaultValue
             * is returned if there is no value for the index, or if the value is not a
             * number and cannot be converted to a number.
             *
             * @param index
             *            subscript
             * @param defaultValue
             *            The default value.
             * @return The value.
             */
            public double optDouble(int index, double defaultValue) {
                Object val = this.opt(index);
                if (Variant.NULL.equals(val)) {
                    return defaultValue;
                }
                if (val instanceof Number){
                    return ((Number) val).doubleValue();
                }
                if (val instanceof String) {
                    try {
                        return Double.parseDouble((String) val);
                    } catch (Exception e) {
                        return defaultValue;
                    }
                }
                return defaultValue;
            }

            /**
             * Get the optional float value associated with an index. NaN is returned
             * if there is no value for the index, or if the value is not a number and
             * cannot be converted to a number.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return The value.
             */
            public float optFloat(int index) {
                return this.optFloat(index, Float.NaN);
            }

            /**
             * Get the optional float value associated with an index. The defaultValue
             * is returned if there is no value for the index, or if the value is not a
             * number and cannot be converted to a number.
             *
             * @param index
             *            subscript
             * @param defaultValue
             *            The default value.
             * @return The value.
             */
            public float optFloat(int index, float defaultValue) {
                Object val = this.opt(index);
                if (Variant.NULL.equals(val)) {
                    return defaultValue;
                }
                if (val instanceof Number){
                    return ((Number) val).floatValue();
                }
                if (val instanceof String) {
                    try {
                        return Float.parseFloat((String) val);
                    } catch (Exception e) {
                        return defaultValue;
                    }
                }
                return defaultValue;
            }

            /**
             * Get the optional int value associated with an index. Zero is returned if
             * there is no value for the index, or if the value is not a number and
             * cannot be converted to a number.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return The value.
             */
            public int optInt(int index) {
                return this.optInt(index, 0);
            }

            /**
             * Get the optional int value associated with an index. The defaultValue is
             * returned if there is no value for the index, or if the value is not a
             * number and cannot be converted to a number.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @param defaultValue
             *            The default value.
             * @return The value.
             */
            public int optInt(int index, int defaultValue) {
                Object val = this.opt(index);
                if (Variant.NULL.equals(val)) {
                    return defaultValue;
                }
                if (val instanceof Number){
                    return ((Number) val).intValue();
                }

                if (val instanceof String) {
                    try {
                        return new BigDecimal(val.toString()).intValue();
                    } catch (Exception e) {
                        return defaultValue;
                    }
                }
                return defaultValue;
            }

            /**
             * Get the enum value associated with a key.
             *
             * @param clazz
             *            The type of enum to retrieve.
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return The enum value at the index location or null if not found
             */
            public <E extends Enum<E>> E optEnum(Class<E> clazz, int index) {
                return this.optEnum(clazz, index, null);
            }

            /**
             * Get the enum value associated with a key.
             *
             * @param clazz
             *            The type of enum to retrieve.
             * @param index
             *            The index must be between 0 and length() - 1.
             * @param defaultValue
             *            The default in case the value is not found
             * @return The enum value at the index location or defaultValue if
             *            the value is not found or cannot be assigned to clazz
             */
            public <E extends Enum<E>> E optEnum(Class<E> clazz, int index, E defaultValue) {
                try {
                    Object val = this.opt(index);
                    if (Variant.NULL.equals(val)) {
                        return defaultValue;
                    }
                    if (clazz.isAssignableFrom(val.getClass())) {
                        // we just checked it!
                        @SuppressWarnings("unchecked")
                        E myE = (E) val;
                        return myE;
                    }
                    return Enum.valueOf(clazz, val.toString());
                } catch (IllegalArgumentException e) {
                    return defaultValue;
                } catch (NullPointerException e) {
                    return defaultValue;
                }
            }


            /**
             * Get the optional BigInteger value associated with an index. The
             * defaultValue is returned if there is no value for the index, or if the
             * value is not a number and cannot be converted to a number.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @param defaultValue
             *            The default value.
             * @return The value.
             */
            public BigInteger optBigInteger(int index, BigInteger defaultValue) {
                Object val = this.opt(index);
                if (Variant.NULL.equals(val)) {
                    return defaultValue;
                }
                if (val instanceof BigInteger){
                    return (BigInteger) val;
                }
                if (val instanceof BigDecimal){
                    return ((BigDecimal) val).toBigInteger();
                }
                if (val instanceof Double || val instanceof Float){
                    return new BigDecimal(((Number) val).doubleValue()).toBigInteger();
                }
                if (val instanceof Long || val instanceof Integer
                        || val instanceof Short || val instanceof Byte){
                    return BigInteger.valueOf(((Number) val).longValue());
                }
                try {
                    final String valStr = val.toString();
                    if(Variant.isDecimalNotation(valStr)) {
                        return new BigDecimal(valStr).toBigInteger();
                    }
                    return new BigInteger(valStr);
                } catch (Exception e) {
                    return defaultValue;
                }
            }

            /**
             * Get the optional BigDecimal value associated with an index. The
             * defaultValue is returned if there is no value for the index, or if the
             * value is not a number and cannot be converted to a number.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @param defaultValue
             *            The default value.
             * @return The value.
             */
            public BigDecimal optBigDecimal(int index, BigDecimal defaultValue) {
                Object val = this.opt(index);
                if (Variant.NULL.equals(val)) {
                    return defaultValue;
                }
                if (val instanceof BigDecimal){
                    return (BigDecimal) val;
                }
                if (val instanceof BigInteger){
                    return new BigDecimal((BigInteger) val);
                }
                if (val instanceof Double || val instanceof Float){
                    return new BigDecimal(((Number) val).doubleValue());
                }
                if (val instanceof Long || val instanceof Integer
                        || val instanceof Short || val instanceof Byte){
                    return new BigDecimal(((Number) val).longValue());
                }
                try {
                    return new BigDecimal(val.toString());
                } catch (Exception e) {
                    return defaultValue;
                }
            }

            /**
             * Get the optional VariantList associated with an index.
             *
             * @param index
             *            subscript
             * @return A VariantList value, or null if the index has no value, or if the
             *         value is not a VariantList.
             */
            public VariantList optJSONElements(int index) {
                Object o = this.opt(index);
                return o instanceof VariantList ? (VariantList) o : null;
            }

            /**
             * Get the optional Variant associated with an index. Null is returned if
             * the key is not found, or null if the index has no value, or if the value
             * is not a Variant.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return A Variant value.
             */
            public Variant optJSONValue(int index) {
                Object o = this.opt(index);
                return o instanceof Variant ? (Variant) o : null;
            }

            /**
             * Get the optional long value associated with an index. Zero is returned if
             * there is no value for the index, or if the value is not a number and
             * cannot be converted to a number.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return The value.
             */
            public long optLong(int index) {
                return this.optLong(index, 0);
            }

            /**
             * Get the optional long value associated with an index. The defaultValue is
             * returned if there is no value for the index, or if the value is not a
             * number and cannot be converted to a number.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @param defaultValue
             *            The default value.
             * @return The value.
             */
            public long optLong(int index, long defaultValue) {
                Object val = this.opt(index);
                if (Variant.NULL.equals(val)) {
                    return defaultValue;
                }
                if (val instanceof Number){
                    return ((Number) val).longValue();
                }

                if (val instanceof String) {
                    try {
                        return new BigDecimal(val.toString()).longValue();
                    } catch (Exception e) {
                        return defaultValue;
                    }
                }
                return defaultValue;
            }

            /**
             * Get an optional {@link Number} value associated with a key, or <code>null</code>
             * if there is no such key or if the value is not a number. If the value is a string,
             * an attempt will be made to evaluate it as a number ({@link BigDecimal}). This method
             * would be used in cases where type coercion of the number value is unwanted.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return An object which is the value.
             */
            public Number optNumber(int index) {
                return this.optNumber(index, null);
            }

            /**
             * Get an optional {@link Number} value associated with a key, or the default if there
             * is no such key or if the value is not a number. If the value is a string,
             * an attempt will be made to evaluate it as a number ({@link BigDecimal}). This method
             * would be used in cases where type coercion of the number value is unwanted.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @param defaultValue
             *            The default.
             * @return An object which is the value.
             */
            public Number optNumber(int index, Number defaultValue) {
                Object val = this.opt(index);
                if (Variant.NULL.equals(val)) {
                    return defaultValue;
                }
                if (val instanceof Number){
                    return (Number) val;
                }

                if (val instanceof String) {
                    try {
                        return Variant.stringToNumber((String) val);
                    } catch (Exception e) {
                        return defaultValue;
                    }
                }
                return defaultValue;
            }

            /**
             * Get the optional string value associated with an index. It returns an
             * empty string if there is no value at that index. If the value is not a
             * string and is not null, then it is converted to a string.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @return A String value.
             */
            public String optString(int index) {
                return this.optString(index, "");
            }

            /**
             * Get the optional string associated with an index. The defaultValue is
             * returned if the key is not found.
             *
             * @param index
             *            The index must be between 0 and length() - 1.
             * @param defaultValue
             *            The default value.
             * @return A String value.
             */
            public String optString(int index, String defaultValue) {
                Object object = this.opt(index);
                return Variant.NULL.equals(object) ? defaultValue : object
                        .toString();
            }

            /**
             * Append a boolean value. This increases the array's length by one.
             *
             * @param value
             *            A boolean value.
             * @return this.
             */
            public VariantList put(boolean value) {
                this.put(value ? Boolean.TRUE : Boolean.FALSE);
                return this;
            }

            /**
             * Put a value in the VariantList, where the value will be a VariantList which
             * is produced from a Collection.
             *
             * @param value
             *            A Collection value.
             * @return this.
             */
            public VariantList put(Collection<?> value) {
                this.put(new VariantList(value));
                return this;
            }

            public VariantList append(Compiler x) {
              if (x.nextClean() != '[') {
                throw x.syntaxError("A VariantList text must start with '['");
              }
              if (x.nextClean() != ']') {
                x.back();
                for (;;) {
                  if (x.nextClean() == ',') {
                    x.back();
                    this.myArrayList.add(Variant.NULL);
                  } else {
                    x.back();
                    this.myArrayList.add(x.nextValue());
                  }
                  switch (x.nextClean()) {
                    case ',':
                      if (x.nextClean() == ']') {
                        return this;
                      }
                      x.back();
                      break;
                    case ']':
                      return this;
                    default:
                      throw x.syntaxError("Expected a ',' or ']'");
                  }
                }
              }
              return this;
            }
            /**
             * Append a double value. This increases the array's length by one.
             *
             * @param value
             *            A double value.
             * @throws Fault
             *             if the value is not finite.
             * @return this.
             */
            public VariantList put(double value) throws Fault {
                Double d = new Double(value);
                Variant.testValidity(d);
                this.put(d);
                return this;
            }

            /**
             * Append an int value. This increases the array's length by one.
             *
             * @param value
             *            An int value.
             * @return this.
             */
            public VariantList put(int value) {
                this.put(new Integer(value));
                return this;
            }

            /**
             * Append an long value. This increases the array's length by one.
             *
             * @param value
             *            A long value.
             * @return this.
             */
            public VariantList put(long value) {
                this.put(new Long(value));
                return this;
            }

            /**
             * Put a value in the VariantList, where the value will be a Variant which
             * is produced from a Map.
             *
             * @param value
             *            A Map value.
             * @return this.
             */
            public VariantList put(Map<?, ?> value) {
                this.put(new Variant(value));
                return this;
            }

            /**
             * Append an object value. This increases the array's length by one.
             *
             * @param value
             *            An object value. The value should be a Boolean, Double,
             *            Integer, VariantList, Variant, Long, or String, or the
             *            Variant.NULL object.
             * @return this.
             */
            public VariantList put(Object value) {
                this.myArrayList.add(value);
                return this;
            }

            /**
             * Put or replace a boolean value in the VariantList. If the index is greater
             * than the length of the VariantList, then null elements will be added as
             * necessary to pad it out.
             *
             * @param index
             *            The subscript.
             * @param value
             *            A boolean value.
             * @return this.
             * @throws Fault
             *             If the index is negative.
             */
            public VariantList put(int index, boolean value) throws Fault {
                this.put(index, value ? Boolean.TRUE : Boolean.FALSE);
                return this;
            }

            /**
             * Put a value in the VariantList, where the value will be a VariantList which
             * is produced from a Collection.
             *
             * @param index
             *            The subscript.
             * @param value
             *            A Collection value.
             * @return this.
             * @throws Fault
             *             If the index is negative or if the value is not finite.
             */
            public VariantList put(int index, Collection<?> value) throws Fault {
                this.put(index, new VariantList(value));
                return this;
            }

            /**
             * Put or replace a double value. If the index is greater than the length of
             * the VariantList, then null elements will be added as necessary to pad it
             * out.
             *
             * @param index
             *            The subscript.
             * @param value
             *            A double value.
             * @return this.
             * @throws Fault
             *             If the index is negative or if the value is not finite.
             */
            public VariantList put(int index, double value) throws Fault {
                this.put(index, new Double(value));
                return this;
            }

            /**
             * Put or replace an int value. If the index is greater than the length of
             * the VariantList, then null elements will be added as necessary to pad it
             * out.
             *
             * @param index
             *            The subscript.
             * @param value
             *            An int value.
             * @return this.
             * @throws Fault
             *             If the index is negative.
             */
            public VariantList put(int index, int value) throws Fault {
                this.put(index, new Integer(value));
                return this;
            }

            /**
             * Put or replace a long value. If the index is greater than the length of
             * the VariantList, then null elements will be added as necessary to pad it
             * out.
             *
             * @param index
             *            The subscript.
             * @param value
             *            A long value.
             * @return this.
             * @throws Fault
             *             If the index is negative.
             */
            public VariantList put(int index, long value) throws Fault {
                this.put(index, new Long(value));
                return this;
            }

            /**
             * Put a value in the VariantList, where the value will be a Variant that
             * is produced from a Map.
             *
             * @param index
             *            The subscript.
             * @param value
             *            The Map value.
             * @return this.
             * @throws Fault
             *             If the index is negative or if the the value is an invalid
             *             number.
             */
            public VariantList put(int index, Map<?, ?> value) throws Fault {
                this.put(index, new Variant(value));
                return this;
            }

            /**
             * Put or replace an object value in the VariantList. If the index is greater
             * than the length of the VariantList, then null elements will be added as
             * necessary to pad it out.
             *
             * @param index
             *            The subscript.
             * @param value
             *            The value to put into the array. The value should be a
             *            Boolean, Double, Integer, VariantList, Variant, Long, or
             *            String, or the Variant.NULL object.
             * @return this.
             * @throws Fault
             *             If the index is negative or if the the value is an invalid
             *             number.
             */
            public VariantList put(int index, Object value) throws Fault {
                Variant.testValidity(value);
                if (index < 0) {
                    throw new Fault("VariantList[" + index + "] not found.");
                }
                if (index < this.length()) {
                    this.myArrayList.set(index, value);
                } else if(index == this.length()){
                    // simple add
                    this.put(value);
                } else {
                    // if we are inserting past the length, we want to grow the array all at once
                    // instead of incrementally.
                    this.myArrayList.ensureCapacity(index + 1);
                    while (index != this.length()) {
                        this.put(Variant.NULL);
                    }
                    this.put(value);
                }
                return this;
            }

            /**
             * Creates a Path using an initialization string and tries to
             * match it to an item within this VariantList. For example, given a
             * VariantList initialized with this document:
             * <pre>
             * [
             *     {"b":"c"}
             * ]
             * </pre>
             * and this Path string:
             * <pre>
             * "/0/b"
             * </pre>
             * Then this method will return the String "c"
             * A Fault may be thrown from code called by this method.
             *
             * @param jsonPointer string that can be used to create a Path
             * @return the item matched by the Path, otherwise null
             */
            public Object query(String jsonPointer) {
                return query(new Path(jsonPointer));
            }

            /**
             * Uses a uaer initialized Path  and tries to
             * match it to an item whithin this VariantList. For example, given a
             * VariantList initialized with this document:
             * <pre>
             * [
             *     {"b":"c"}
             * ]
             * </pre>
             * and this Path:
             * <pre>
             * "/0/b"
             * </pre>
             * Then this method will return the String "c"
             * A Fault may be thrown from code called by this method.
             *
             * @param path string that can be used to create a Path
             * @return the item matched by the Path, otherwise null
             */
            public Object query(Path path) {
                return path.queryFrom(this);
            }

            /**
             * Queries and returns a value from this object using {@code jsonPointer}, or
             * returns null if the query fails due to a missing key.
             *
             * @param jsonPointer the string representation of the JSON pointer
             * @return the queried value or {@code null}
             * @throws IllegalArgumentException if {@code jsonPointer} has invalid syntax
             */
            public Object optQuery(String jsonPointer) {
              return optQuery(new Path(jsonPointer));
            }

            /**
             * Queries and returns a value from this object using {@code path}, or
             * returns null if the query fails due to a missing key.
             *
             * @param path The JSON pointer
             * @return the queried value or {@code null}
             * @throws IllegalArgumentException if {@code path} has invalid syntax
             */
            public Object optQuery(Path path) {
                try {
                    return path.queryFrom(this);
                } catch (XPR.Fault e) {
                    return null;
                }
            }

            /**
             * Remove an index and close the hole.
             *
             * @param index
             *            The index of the element to be removed.
             * @return The value that was associated with the index, or null if there
             *         was no value.
             */
            public Object remove(int index) {
                return index >= 0 && index < this.length()
                    ? this.myArrayList.remove(index)
                    : null;
            }

            /**
             * Determine if two JSONElementss are similar.
             * They must contain similar sequences.
             *
             * @param other The other VariantList
             * @return true if they are equal
             */
            public boolean similar(Object other) {
                if (!(other instanceof VariantList)) {
                    return false;
                }
                int len = this.length();
                if (len != ((VariantList)other).length()) {
                    return false;
                }
                for (int i = 0; i < len; i += 1) {
                    Object valueThis = this.myArrayList.get(i);
                    Object valueOther = ((VariantList)other).myArrayList.get(i);
                    if(valueThis == valueOther) {
                      return true;
                    }
                    if(valueThis == null) {
                      return false;
                    }
                    if (valueThis instanceof Variant) {
                        if (!((Variant)valueThis).similar(valueOther)) {
                            return false;
                        }
                    } else if (valueThis instanceof VariantList) {
                        if (!((VariantList)valueThis).similar(valueOther)) {
                            return false;
                        }
                    } else if (!valueThis.equals(valueOther)) {
                        return false;
                    }
                }
                return true;
            }

            /**
             * Produce a Variant by combining a VariantList of names with the values of
             * this VariantList.
             *
             * @param names
             *            A VariantList containing a list of key strings. These will be
             *            paired with the values.
             * @return A Variant, or null if there are no names or if this VariantList
             *         has no values.
             * @throws Fault
             *             If any of the names are null.
             */
            public Variant toJSONValue(VariantList names) throws Fault {
                if (names == null || names.length() == 0 || this.length() == 0) {
                    return null;
                }
                Variant jo = new Variant(names.length());
                for (int i = 0; i < names.length(); i += 1) {
                    jo.put(names.getString(i), this.opt(i));
                }
                return jo;
            }

            /**
             * Make a JSON text of this VariantList. For compactness, no unnecessary
             * whitespace is added. If it is not possible to produce a syntactically
             * correct JSON text then null will be returned instead. This could occur if
             * the array contains an invalid number.
             * <p><b>
             * Warning: This method assumes that the data structure is acyclical.
             * </b>
             *
             * @return a printable, displayable, transmittable representation of the
             *         array.
             */
            @Override
            public String toString() {
                try {
                    return this.toString(0);
                } catch (Exception e) {
                    return null;
                }
            }

            /**
             * Make a pretty-printed JSON text of this VariantList.
             *
             * <p>If <code>indentFactor > 0</code> and the {@link VariantList} has only
             * one element, then the array will be output on a single line:
             * <pre>{@code [1]}</pre>
             *
             * <p>If an array has 2 or more elements, then it will be output across
             * multiple lines: <pre>{@code
             * [
             * 1,
             * "value 2",
             * 3
             * ]
             * }</pre>
             * <p><b>
             * Warning: This method assumes that the data structure is acyclical.
             * </b>
             *
             * @param indentFactor
             *            The number of spaces to add to each level of indentation.
             * @return a printable, displayable, transmittable representation of the
             *         object, beginning with <code>[</code>&nbsp;<small>(left
             *         bracket)</small> and ending with <code>]</code>
             *         &nbsp;<small>(right bracket)</small>.
             * @throws Fault
             */
            public String toString(int indentFactor) throws Fault {
                StringWriter sw = new StringWriter();
                synchronized (sw.getBuffer()) {
                    return this.write(sw, indentFactor, 0).toString();
                }
            }

            /**
             * Write the contents of the VariantList as JSON text to a writer. For
             * compactness, no whitespace is added.
             * <p><b>
             * Warning: This method assumes that the data structure is acyclical.
             *</b>
             *
             * @return The writer.
             * @throws Fault
             */
            public java.io.Writer write(java.io.Writer writer) throws Fault {
                return this.write(writer, 0, 0);
            }

            /**
             * Write the contents of the VariantList as JSON text to a writer.
             *
             * <p>If <code>indentFactor > 0</code> and the {@link VariantList} has only
             * one element, then the array will be output on a single line:
             * <pre>{@code [1]}</pre>
             *
             * <p>If an array has 2 or more elements, then it will be output across
             * multiple lines: <pre>{@code
             * [
             * 1,
             * "value 2",
             * 3
             * ]
             * }</pre>
             * <p><b>
             * Warning: This method assumes that the data structure is acyclical.
             * </b>
             *
             * @param writer
             *            Writes the serialized JSON
             * @param indentFactor
             *            The number of spaces to add to each level of indentation.
             * @param indent
             *            The indentation of the top level.
             * @return The writer.
             * @throws Fault
             */
            public java.io.Writer write(java.io.Writer writer, int indentFactor, int indent)
                    throws Fault
            {
                try {
                    boolean commanate = false;
                    int length = this.length();
                    writer.write('[');

                    if (length == 1) {
                        try {
                            Variant.writeValue(writer, this.myArrayList.get(0),
                                    indentFactor, indent);
                        } catch (Exception e) {
                            throw new Fault("Unable to write VariantList value at index: 0", e);
                        }
                    } else if (length != 0) {
                        final int newindent = indent + indentFactor;

                        for (int i = 0; i < length; i += 1) {
                            if (commanate) {
                                writer.write(',');
                            }
                            if (indentFactor > 0) {
                                writer.write('\n');
                            }
                            Variant.indent(writer, newindent);
                            try {
                                Variant.writeValue(writer, this.myArrayList.get(i),
                                        indentFactor, newindent);
                            } catch (Exception e) {
                                throw new Fault("Unable to write VariantList value at index: " + i, e);
                            }
                            commanate = true;
                        }
                        if (indentFactor > 0) {
                            writer.write('\n');
                        }
                        Variant.indent(writer, indent);
                    }
                    writer.write(']');
                    return writer;
                } catch (IOException e) {
                    throw new Fault(e);
                }
            }

            /**
             * Returns a java.util.List containing all of the elements in this array.
             * If an element in the array is a VariantList or Variant it will also
             * be converted.
             * <p>
             * Warning: This method assumes that the data structure is acyclical.
             *
             * @return a java.util.List containing the elements of this array
             */
            public List<Object> toList() {
                List<Object> results = new ArrayList<Object>(this.myArrayList.size());
                for (Object element : this.myArrayList) {
                    if (element == null || Variant.NULL.equals(element)) {
                        results.add(null);
                    } else if (element instanceof VariantList) {
                        results.add(((VariantList) element).toList());
                    } else if (element instanceof Variant) {
                        results.add(((Variant) element).toMap());
                    } else {
                        results.add(element);
                    }
                }
                return results;
            }

          public List<String> toStringList() {
            List<String> results = new ArrayList<String>(this.myArrayList.size());
            for (Object element : this.myArrayList) {
              if (element == null || Variant.NULL.equals(element)) {
                results.add(null);
              } else {
                results.add(element.toString());
              }
            }
            return results;
          }

        }

        /**
         * A Variant is an unordered collection of name/value pairs. Its external
         * form is a string wrapped in curly braces with colons between the names and
         * values, and commas between the values and names. The internal form is an
         * object having <code>getKey</code> and <code>opt</code> methods for accessing
         * the values by name, and <code>put</code> methods for adding or replacing
         * values by name. The values can be any of these types: <code>Boolean</code>,
         * <code>VariantList</code>, <code>Variant</code>, <code>Number</code>,
         * <code>String</code>, or the <code>Variant.NULL</code> object. A
         * Variant constructor can be used to convert an external form JSON text
         * into an internal form whose values can be retrieved with the
         * <code>getKey</code> and <code>opt</code> methods, or to convert values into a
         * JSON text using the <code>put</code> and <code>toString</code> methods. A
         * <code>getKey</code> method returns a value if one can be found, and throws an
         * exception if one cannot be found. An <code>opt</code> method returns a
         * default value instead of throwing an exception, and so is useful for
         * obtaining optional values.
         * <p>
         * The generic <code>getKey()</code> and <code>opt()</code> methods return an
         * object, which you can cast or query for type. There are also typed
         * <code>getKey</code> and <code>opt</code> methods that do type checking and type
         * coercion for you. The opt methods differ from the getKey methods in that they
         * do not throw. Instead, they return a specified value, such as null.
         * <p>
         * The <code>put</code> methods add or replace values in an object. For
         * example,
         *
         * <pre>
         * myString = new Variant()
         *         .put(&quot;JSON&quot;, &quot;Hello, World!&quot;).toString();
         * </pre>
         *
         * produces the string <code>{"JSON": "Hello, World"}</code>.
         * <p>
         * The texts produced by the <code>toString</code> methods strictly conform to
         * the JSON syntax rules. The constructors are more forgiving in the texts they
         * will accept:
         * <ul>
         * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just
         * before the closing brace.</li>
         * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single
         * quote)</small>.</li>
         * <li>Strings do not need to be quoted at all if they do not begin with a
         * quote or single quote, and if they do not contain leading or trailing
         * spaces, and if they do not contain any of these characters:
         * <code>{ } [ ] / \ : , #</code> and if they do not look like numbers and
         * if they are not the reserved words <code>true</code>, <code>false</code>,
         * or <code>null</code>.</li>
         * </ul>
         *
         * @author JSON.org
         * @version 2016-08-15
         */
        public static class Variant {
            /**
             * Variant.NULL is equivalent to the value that JavaScript calls null,
             * whilst Java's null is equivalent to the value that JavaScript calls
             * undefined.
             */
            private static final class Null {

                /**
                 * There is only intended to be a single instance of the NULL object,
                 * so the clone method returns itself.
                 *
                 * @return NULL.
                 */
                @Override
                protected final Object clone() {
                    return this;
                }

                /**
                 * A Null object is equal to the null value and to itself.
                 *
                 * @param object
                 *            An object to test for nullness.
                 * @return true if the object parameter is the Variant.NULL object or
                 *         null.
                 */
                @Override
                public boolean equals(Object object) {
                    return object == null || object == this;
                }
                /**
                 * A Null object is equal to the null value and to itself.
                 *
                 * @return always returns 0.
                 */
                @Override
                public int hashCode() {
                    return 0;
                }

                /**
                 * Get the "null" string value.
                 *
                 * @return The string "null".
                 */
                @Override
                public String toString() {
                    return "null";
                }
            }

            /**
             * The map where the Variant's values are kept.
             */
            private final Map<String, Object> map;

            /**
             * It is sometimes more convenient and less ambiguous to have a
             * <code>NULL</code> object than to use Java's <code>null</code> value.
             * <code>Variant.NULL.equals(null)</code> returns <code>true</code>.
             * <code>Variant.NULL.toString()</code> returns <code>"null"</code>.
             */
            public static final Object NULL = new Null();

            /**
             * Construct an empty Variant.
             */
            public Variant() {
                // HashMap is used on purpose to ensure that elements are unordered by
                // the specification.
                // JSON tends to be a portable transfer format to allows the container
                // implementations to rearrange their items for a faster element
                // retrieval based on associative access.
                // Therefore, an implementation mustn't rely on the order of the item.
                this.map = new HashMap<String, Object>();
            }

            /**
             * Construct a Variant from a subset of another Variant. An array of
             * strings is used to identify the keys that should be copied. Missing keys
             * are ignored.
             *
             * @param jo
             *            A Variant.
             * @param names
             *            An array of strings.
             */
            public Variant(Variant jo, String[] names) {
                this(names.length);
                for (int i = 0; i < names.length; i += 1) {
                    try {
                        this.putOnce(names[i], jo.opt(names[i]));
                    } catch (Exception ignore) {
                    }
                }
            }

            /**
             * Construct a Variant from a Compiler.
             *
             * @param x
             *            A Compiler object containing the source string.
             * @throws Fault
             *             If there is a syntax error in the source string or a
             *             duplicated key.
             */
            public Variant(Compiler x) throws Fault {
                this();
                char c;
                String key;

                if (x.nextClean() != '{') {
                    throw x.syntaxError("A Variant text must begin with '{'");
                }
                for (;;) {
                    c = x.nextClean();
                    switch (c) {
                    case 0:
                        throw x.syntaxError("A Variant text must end with '}'");
                    case '}':
                        return;
                    default:
                        x.back();
                        key = x.nextValue().toString();
                    }

                    // The key is followed by ':'.

                    c = x.nextClean();
                    if (c != ':') {
                        throw x.syntaxError("Expected a ':' after a key");
                    }

                    // Use syntaxError(..) to include error location

                    if (key != null) {
                        // Check if key exists
                        if (this.opt(key) != null) {
                            // key already exists
                            throw x.syntaxError("Duplicate key \"" + key + "\"");
                        }
                        // Only add value if non-null
                        Object value = x.nextValue();
                        if (value!=null) {
                            this.put(key, value);
                        }
                    }

                    // Pairs are separated by ','.

                    switch (x.nextClean()) {
                    case ';':
                    case ',':
                        if (x.nextClean() == '}') {
                            return;
                        }
                        x.back();
                        break;
                    case '}':
                        return;
                    default:
                        throw x.syntaxError("Expected a ',' or '}'");
                    }
                }
            }

            /**
             * Construct a Variant from a Map.
             *
             * @param m
             *            A map object that can be used to initialize the contents of
             *            the Variant.
             */
            public Variant(Map<?, ?> m) {
                if (m == null) {
                    this.map = new HashMap<String, Object>();
                } else {
                    this.map = new HashMap<String, Object>(m.size());
                  for (final Map.Entry<?, ?> e : m.entrySet()) {
                        final Object value = e.getValue();
                        if (value != null) {
                            this.map.put(String.valueOf(e.getKey()), wrap(value));
                        }
                    }
                }
            }

            /**
             * Construct a Variant from an Object using bean getters. It reflects on
             * all of the public methods of the object. For each of the methods with no
             * parameters and a name starting with <code>"getKey"</code> or
             * <code>"is"</code> followed by an uppercase letter, the method is invoked,
             * and a key and the value returned from the getter method are put into the
             * new Variant.
             * <p>
             * The key is formed by removing the <code>"getKey"</code> or <code>"is"</code>
             * prefix. If the second remaining character is not upper case, then the
             * first character is converted to lower case.
             * <p>
             * For example, if an object has a method named <code>"getName"</code>, and
             * if the result of calling <code>object.getName()</code> is
             * <code>"Larry Fine"</code>, then the Variant will contain
             * <code>"name": "Larry Fine"</code>.
             * <p>
             * Methods that return <code>void</code> as well as <code>static</code>
             * methods are ignored.
             *
             * @param bean
             *            An object that has getter methods that should be used to make
             *            a Variant.
             */
            public Variant(Object bean) {
                this();
                this.populateMap(bean);
            }

            /**
             * Construct a Variant from an Object, using reflection to find the
             * public members. The resulting Variant's keys will be the strings from
             * the names array, and the values will be the field values associated with
             * those keys in the object. If a key is not found or not visible, then it
             * will not be copied into the new Variant.
             *
             * @param object
             *            An object that has fields that should be used to make a
             *            Variant.
             * @param names
             *            An array of strings, the names of the fields to be obtained
             *            from the object.
             */
            public Variant(Object object, String names[]) {
                this(names.length);
                Class<?> c = object.getClass();
                for (int i = 0; i < names.length; i += 1) {
                    String name = names[i];
                    try {
                        this.putOpt(name, c.getField(name).get(object));
                    } catch (Exception ignore) {
                    }
                }
            }

            /**
             * Construct a Variant from a source JSON text string. This is the most
             * commonly used Variant constructor.
             *
             * @param source
             *            A string beginning with <code>{</code>&nbsp;<small>(left
             *            brace)</small> and ending with <code>}</code>
             *            &nbsp;<small>(right brace)</small>.
             * @exception Fault
             *                If there is a syntax error in the source string or a
             *                duplicated key.
             */
            public Variant(String source) throws Fault {
                this(new Compiler(source));
            }

            /**
             * Construct a Variant from a ResourceBundle.
             *
             * @param baseName
             *            The ResourceBundle base name.
             * @param locale
             *            The Locale to load the ResourceBundle for.
             * @throws Fault
             *             If any JSONExceptions are detected.
             */
            public Variant(String baseName, Locale locale) throws Fault {
                this();
                ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale,
                        Thread.currentThread().getContextClassLoader());

        // Iterate through the keys in the pluginBundle.

                Enumeration<String> keys = bundle.getKeys();
                while (keys.hasMoreElements()) {
                    Object key = keys.nextElement();
                    if (key != null) {

        // Go through the path, ensuring that there is a nested Variant for each
        // segment except the last. Add the value using the last segment's name into
        // the deepest nested Variant.

                        String[] path = ((String) key).split("\\.");
                        int last = path.length - 1;
                        Variant target = this;
                        for (int i = 0; i < last; i += 1) {
                            String segment = path[i];
                            Variant nextTarget = target.optJSONValue(segment);
                            if (nextTarget == null) {
                                nextTarget = new Variant();
                                target.put(segment, nextTarget);
                            }
                            target = nextTarget;
                        }
                        target.put(path[last], bundle.getString((String) key));
                    }
                }
            }

            /**
             * Constructor to specify an initial capacity of the internal map. Useful for library
             * internal calls where we know, or at least can best guess, how big this Variant
             * will be.
             *
             * @param initialCapacity initial capacity of the internal map.
             */
            protected Variant(int initialCapacity){
                this.map = new HashMap<String, Object>(initialCapacity);
            }

            /**
             * Accumulate values under a key. It is similar to the put method except
             * that if there is already an object stored under the key then a VariantList
             * is stored under the key to hold all of the accumulated values. If there
             * is already a VariantList, then the new value is appended to it. In
             * contrast, the put method replaces the previous value.
             *
             * If only one value is accumulated that is not a VariantList, then the result
             * will be the same as using put. But if multiple values are accumulated,
             * then the result will be like add.
             *
             * @param key
             *            A key string.
             * @param value
             *            An object to be accumulated under the key.
             * @return this.
             * @throws Fault
             *             If the value is an invalid number or if the key is null.
             */
            public Variant accumulate(String key, Object value) throws Fault {
                testValidity(value);
                Object object = this.opt(key);
                if (object == null) {
                    this.put(key,
                            value instanceof VariantList ? new VariantList().put(value)
                                    : value);
                } else if (object instanceof VariantList) {
                    ((VariantList) object).put(value);
                } else {
                    this.put(key, new VariantList().put(object).put(value));
                }
                return this;
            }

            /**
             * Append values to the array under a key. If the key does not exist in the
             * Variant, then the key is put in the Variant with its value being a
             * VariantList containing the value parameter. If the key was already
             * associated with a VariantList, then the value parameter is appended to it.
             *
             * @param key
             *            A key string.
             * @param value
             *            An object to be accumulated under the key.
             * @return this.
             * @throws Fault
             *             If the key is null or if the current value associated with
             *             the key is not a VariantList.
             */
            public Variant append(String key, Object value) throws Fault {
                testValidity(value);
                Object object = this.opt(key);
                if (object == null) {
                    this.put(key, new VariantList().put(value));
                } else if (object instanceof VariantList) {
                    this.put(key, ((VariantList) object).put(value));
                } else {
                    throw new Fault("Variant[" + key
                            + "] is not a VariantList.");
                }
                return this;
            }

            /**
             * Produce a string from a double. The string "null" will be returned if the
             * number is not finite.
             *
             * @param d
             *            A double.
             * @return A String.
             */
            public static String doubleToString(double d) {
                if (Double.isInfinite(d) || Double.isNaN(d)) {
                    return "null";
                }

        // Shave off trailing zeros and decimal point, if possible.

                String string = Double.toString(d);
                if (string.indexOf('.') > 0 && string.indexOf('e') < 0
                        && string.indexOf('E') < 0) {
                    while (string.endsWith("0")) {
                        string = string.substring(0, string.length() - 1);
                    }
                    if (string.endsWith(".")) {
                        string = string.substring(0, string.length() - 1);
                    }
                }
                return string;
            }

            /**
             * Get the value object associated with a key.
             *
             * @param key
             *            A key string.
             * @return The object associated with the key.
             * @throws Fault
             *             if the key is not found.
             */
            public Object get(String key) throws Fault {
                if (key == null) {
                    throw new Fault("Null key.");
                }
                Object object = this.opt(key);
                if (object == null) {
                    throw new Fault("Variant[" + quote(key) + "] not found.");
                }
                return object;
            }

            /**
            * Get the enum value associated with a key.
            *
            * @param clazz
            *           The type of enum to retrieve.
            * @param key
            *           A key string.
            * @return The enum value associated with the key
            * @throws Fault
            *             if the key is not found or if the value cannot be converted
            *             to an enum.
            */
            public <E extends Enum<E>> E getEnum(Class<E> clazz, String key) throws Fault {
                E val = optEnum(clazz, key);
                if(val==null) {
                    // Fault should really take a throwable argument.
                    // If it did, I would re-implement this with the Enum.valueOf
                    // method and place any thrown exception in the Fault
                    throw new Fault("Variant[" + quote(key)
                            + "] is not an enum of type " + quote(clazz.getSimpleName())
                            + ".");
                }
                return val;
            }

            /**
             * Get the boolean value associated with a key.
             *
             * @param key
             *            A key string.
             * @return The truth.
             * @throws Fault
             *             if the value is not a Boolean or the String "true" or
             *             "false".
             */
            public boolean getBoolean(String key) throws Fault {
                Object object = this.get(key);
                if (object.equals(Boolean.FALSE)
                        || (object instanceof String && ((String) object)
                                .equalsIgnoreCase("false"))) {
                    return false;
                } else if (object.equals(Boolean.TRUE)
                        || (object instanceof String && ((String) object)
                                .equalsIgnoreCase("true"))) {
                    return true;
                }
                throw new Fault("Variant[" + quote(key)
                        + "] is not a Boolean.");
            }

            /**
             * Get the BigInteger value associated with a key.
             *
             * @param key
             *            A key string.
             * @return The numeric value.
             * @throws Fault
             *             if the key is not found or if the value cannot
             *             be converted to BigInteger.
             */
            public BigInteger getBigInteger(String key) throws Fault {
                Object object = this.get(key);
                try {
                    return new BigInteger(object.toString());
                } catch (Exception e) {
                    throw new Fault("Variant[" + quote(key)
                            + "] could not be converted to BigInteger.", e);
                }
            }

            /**
             * Get the BigDecimal value associated with a key.
             *
             * @param key
             *            A key string.
             * @return The numeric value.
             * @throws Fault
             *             if the key is not found or if the value
             *             cannot be converted to BigDecimal.
             */
            public BigDecimal getBigDecimal(String key) throws Fault {
                Object object = this.get(key);
                if (object instanceof BigDecimal) {
                    return (BigDecimal)object;
                }
                try {
                    return new BigDecimal(object.toString());
                } catch (Exception e) {
                    throw new Fault("Variant[" + quote(key)
                            + "] could not be converted to BigDecimal.", e);
                }
            }

            /**
             * Get the double value associated with a key.
             *
             * @param key
             *            A key string.
             * @return The numeric value.
             * @throws Fault
             *             if the key is not found or if the value is not a Number
             *             object and cannot be converted to a number.
             */
            public double getDouble(String key) throws Fault {
                Object object = this.get(key);
                try {
                    return object instanceof Number ? ((Number) object).doubleValue()
                            : Double.parseDouble(object.toString());
                } catch (Exception e) {
                    throw new Fault("Variant[" + quote(key)
                            + "] is not a number.", e);
                }
            }

            /**
             * Get the float value associated with a key.
             *
             * @param key
             *            A key string.
             * @return The numeric value.
             * @throws Fault
             *             if the key is not found or if the value is not a Number
             *             object and cannot be converted to a number.
             */
            public float getFloat(String key) throws Fault {
                Object object = this.get(key);
                try {
                    return object instanceof Number ? ((Number) object).floatValue()
                            : Float.parseFloat(object.toString());
                } catch (Exception e) {
                    throw new Fault("Variant[" + quote(key)
                            + "] is not a number.", e);
                }
            }

            /**
             * Get the Number value associated with a key.
             *
             * @param key
             *            A key string.
             * @return The numeric value.
             * @throws Fault
             *             if the key is not found or if the value is not a Number
             *             object and cannot be converted to a number.
             */
            public Number getNumber(String key) throws Fault {
                Object object = this.get(key);
                try {
                    if (object instanceof Number) {
                        return (Number)object;
                    }
                    return stringToNumber(object.toString());
                } catch (Exception e) {
                    throw new Fault("Variant[" + quote(key)
                            + "] is not a number.", e);
                }
            }

            /**
             * Get the int value associated with a key.
             *
             * @param key
             *            A key string.
             * @return The integer value.
             * @throws Fault
             *             if the key is not found or if the value cannot be converted
             *             to an integer.
             */
            public int getInt(String key) throws Fault {
                Object object = this.get(key);
                try {
                    return object instanceof Number ? ((Number) object).intValue()
                            : Integer.parseInt((String) object);
                } catch (Exception e) {
                    throw new Fault("Variant[" + quote(key)
                            + "] is not an int.", e);
                }
            }

            /**
             * Get the VariantList value associated with a key.
             *
             * @param key
             *            A key string.
             * @return A VariantList which is the value.
             * @throws Fault
             *             if the key is not found or if the value is not a VariantList.
             */
            public VariantList getJSONElements(String key) throws Fault {
                Object object = this.get(key);
                if (object instanceof VariantList) {
                    return (VariantList) object;
                }
                throw new Fault("Variant[" + quote(key)
                        + "] is not a VariantList.");
            }

            /**
             * Get the Variant value associated with a key.
             *
             * @param key
             *            A key string.
             * @return A Variant which is the value.
             * @throws Fault
             *             if the key is not found or if the value is not a Variant.
             */
            public Variant getJSONValue(String key) throws Fault {
                Object object = this.get(key);
                if (object instanceof Variant) {
                    return (Variant) object;
                }
                throw new Fault("Variant[" + quote(key)
                        + "] is not a Variant.");
            }

            /**
             * Get the long value associated with a key.
             *
             * @param key
             *            A key string.
             * @return The long value.
             * @throws Fault
             *             if the key is not found or if the value cannot be converted
             *             to a long.
             */
            public long getLong(String key) throws Fault {
                Object object = this.get(key);
                try {
                    return object instanceof Number ? ((Number) object).longValue()
                            : Long.parseLong((String) object);
                } catch (Exception e) {
                    throw new Fault("Variant[" + quote(key)
                            + "] is not a long.", e);
                }
            }

            /**
             * Get an array of field names from a Variant.
             *
             * @return An array of field names, or null if there are no names.
             */
            public static String[] getNames(Variant jo) {
                int length = jo.length();
                if (length == 0) {
                    return null;
                }
                return jo.keySet().toArray(new String[length]);
            }

            /**
             * Get an array of field names from an Object.
             *
             * @return An array of field names, or null if there are no names.
             */
            public static String[] getNames(Object object) {
                if (object == null) {
                    return null;
                }
                Class<?> klass = object.getClass();
                Field[] fields = klass.getFields();
                int length = fields.length;
                if (length == 0) {
                    return null;
                }
                String[] names = new String[length];
                for (int i = 0; i < length; i += 1) {
                    names[i] = fields[i].getName();
                }
                return names;
            }

            /**
             * Get the string associated with a key.
             *
             * @param key
             *            A key string.
             * @return A string which is the value.
             * @throws Fault
             *             if there is no string value for the key.
             */
            public String getString(String key) throws Fault {
                Object object = this.get(key);
                if (object instanceof String) {
                    return (String) object;
                }
                throw new Fault("Variant[" + quote(key) + "] not a string.");
            }

            /**
             * Determine if the Variant contains a specific key.
             *
             * @param key
             *            A key string.
             * @return true if the key exists in the Variant.
             */
            public boolean has(String key) {
                return this.map.containsKey(key);
            }

            /**
             * Increment a property of a Variant. If there is no such property,
             * create one with a value of 1. If there is such a property, and if it is
             * an Integer, Long, Double, or Float, then add one to it.
             *
             * @param key
             *            A key string.
             * @return this.
             * @throws Fault
             *             If there is already a property with this name that is not an
             *             Integer, Long, Double, or Float.
             */
            public Variant increment(String key) throws Fault {
                Object value = this.opt(key);
                if (value == null) {
                    this.put(key, 1);
                } else if (value instanceof BigInteger) {
                    this.put(key, ((BigInteger)value).add(BigInteger.ONE));
                } else if (value instanceof BigDecimal) {
                    this.put(key, ((BigDecimal)value).add(BigDecimal.ONE));
                } else if (value instanceof Integer) {
                    this.put(key, ((Integer) value).intValue() + 1);
                } else if (value instanceof Long) {
                    this.put(key, ((Long) value).longValue() + 1L);
                } else if (value instanceof Double) {
                    this.put(key, ((Double) value).doubleValue() + 1.0d);
                } else if (value instanceof Float) {
                    this.put(key, ((Float) value).floatValue() + 1.0f);
                } else {
                    throw new Fault("Unable to increment [" + quote(key) + "].");
                }
                return this;
            }

            /**
             * Determine if the value associated with the key is null or if there is no
             * value.
             *
             * @param key
             *            A key string.
             * @return true if there is no value associated with the key or if the value
             *         is the Variant.NULL object.
             */
            public boolean isNull(String key) {
                return Variant.NULL.equals(this.opt(key));
            }

            /**
             * Get an enumeration of the keys of the Variant. Modifying this key Set will also
             * modify the Variant. Use with caution.
             *
             * @see Set#iterator()
             *
             * @return An iterator of the keys.
             */
            public Iterator<String> keys() {
                return this.keySet().iterator();
            }

            /**
             * Get a set of keys of the Variant. Modifying this key Set will also modify the
             * Variant. Use with caution.
             *
             * @see Map#keySet()
             *
             * @return A keySet.
             */
            public Set<String> keySet() {
                return this.map.keySet();
            }

            /**
             * Get a set of entries of the Variant. These are raw values and may not
             * match what is returned by the Variant getKey* and opt* functions. Modifying
             * the returned EntrySet or the Entry objects contained therein will modify the
             * backing Variant. This does not return a clone or a read-only view.
             *
             * Use with caution.
             *
             * @see Map#entrySet()
             *
             * @return An Entry Set
             */
            protected Set<Map.Entry<String, Object>> entrySet() {
                return this.map.entrySet();
            }

            /**
             * Get the number of keys stored in the Variant.
             *
             * @return The number of keys in the Variant.
             */
            public int length() {
                return this.map.size();
            }

            /**
             * Produce a VariantList containing the names of the elements of this
             * Variant.
             *
             * @return A VariantList containing the key strings, or null if the Variant
             *         is empty.
             */
            public VariantList names() {
              if(this.map.isEmpty()) {
                return null;
              }
                return new VariantList(this.map.keySet());
            }

            /**
             * Produce a string from a Number.
             *
             * @param number
             *            A Number
             * @return A String.
             * @throws Fault
             *             If n is a non-finite number.
             */
            public static String numberToString(Number number) throws Fault {
                if (number == null) {
                    throw new Fault("Null pointer");
                }
                testValidity(number);

                // Shave off trailing zeros and decimal point, if possible.

                String string = number.toString();
                if (string.indexOf('.') > 0 && string.indexOf('e') < 0
                        && string.indexOf('E') < 0) {
                    while (string.endsWith("0")) {
                        string = string.substring(0, string.length() - 1);
                    }
                    if (string.endsWith(".")) {
                        string = string.substring(0, string.length() - 1);
                    }
                }
                return string;
            }

            /**
             * Get an optional value associated with a key.
             *
             * @param key
             *            A key string.
             * @return An object which is the value, or null if there is no value.
             */
            public Object opt(String key) {
                return key == null ? null : this.map.get(key);
            }

            /**
             * Get the enum value associated with a key.
             *
             * @param clazz
             *            The type of enum to retrieve.
             * @param key
             *            A key string.
             * @return The enum value associated with the key or null if not found
             */
            public <E extends Enum<E>> E optEnum(Class<E> clazz, String key) {
                return this.optEnum(clazz, key, null);
            }

            /**
             * Get the enum value associated with a key.
             *
             * @param clazz
             *            The type of enum to retrieve.
             * @param key
             *            A key string.
             * @param defaultValue
             *            The default in case the value is not found
             * @return The enum value associated with the key or defaultValue
             *            if the value is not found or cannot be assigned to <code>clazz</code>
             */
            public <E extends Enum<E>> E optEnum(Class<E> clazz, String key, E defaultValue) {
                try {
                    Object val = this.opt(key);
                    if (NULL.equals(val)) {
                        return defaultValue;
                    }
                    if (clazz.isAssignableFrom(val.getClass())) {
                        // we just checked it!
                        @SuppressWarnings("unchecked")
                        E myE = (E) val;
                        return myE;
                    }
                    return Enum.valueOf(clazz, val.toString());
                } catch (IllegalArgumentException e) {
                    return defaultValue;
                } catch (NullPointerException e) {
                    return defaultValue;
                }
            }

            /**
             * Get an optional boolean associated with a key. It returns false if there
             * is no such key, or if the value is not Boolean.TRUE or the String "true".
             *
             * @param key
             *            A key string.
             * @return The truth.
             */
            public boolean optBoolean(String key) {
                return this.optBoolean(key, false);
            }

            /**
             * Get an optional boolean associated with a key. It returns the
             * defaultValue if there is no such key, or if it is not a Boolean or the
             * String "true" or "false" (case insensitive).
             *
             * @param key
             *            A key string.
             * @param defaultValue
             *            The default.
             * @return The truth.
             */
            public boolean optBoolean(String key, boolean defaultValue) {
                Object val = this.opt(key);
                if (NULL.equals(val)) {
                    return defaultValue;
                }
                if (val instanceof Boolean){
                    return ((Boolean) val).booleanValue();
                }
                try {
                    // we'll use the getKey anyway because it does string conversion.
                    return this.getBoolean(key);
                } catch (Exception e) {
                    return defaultValue;
                }
            }

            /**
             * Get an optional BigDecimal associated with a key, or the defaultValue if
             * there is no such key or if its value is not a number. If the value is a
             * string, an attempt will be made to evaluate it as a number.
             *
             * @param key
             *            A key string.
             * @param defaultValue
             *            The default.
             * @return An object which is the value.
             */
            public BigDecimal optBigDecimal(String key, BigDecimal defaultValue) {
                Object val = this.opt(key);
                if (NULL.equals(val)) {
                    return defaultValue;
                }
                if (val instanceof BigDecimal){
                    return (BigDecimal) val;
                }
                if (val instanceof BigInteger){
                    return new BigDecimal((BigInteger) val);
                }
                if (val instanceof Double || val instanceof Float){
                    return new BigDecimal(((Number) val).doubleValue());
                }
                if (val instanceof Long || val instanceof Integer
                        || val instanceof Short || val instanceof Byte){
                    return new BigDecimal(((Number) val).longValue());
                }
                // don't check if it's a string in case of unchecked Number subclasses
                try {
                    return new BigDecimal(val.toString());
                } catch (Exception e) {
                    return defaultValue;
                }
            }

            /**
             * Get an optional BigInteger associated with a key, or the defaultValue if
             * there is no such key or if its value is not a number. If the value is a
             * string, an attempt will be made to evaluate it as a number.
             *
             * @param key
             *            A key string.
             * @param defaultValue
             *            The default.
             * @return An object which is the value.
             */
            public BigInteger optBigInteger(String key, BigInteger defaultValue) {
                Object val = this.opt(key);
                if (NULL.equals(val)) {
                    return defaultValue;
                }
                if (val instanceof BigInteger){
                    return (BigInteger) val;
                }
                if (val instanceof BigDecimal){
                    return ((BigDecimal) val).toBigInteger();
                }
                if (val instanceof Double || val instanceof Float){
                    return new BigDecimal(((Number) val).doubleValue()).toBigInteger();
                }
                if (val instanceof Long || val instanceof Integer
                        || val instanceof Short || val instanceof Byte){
                    return BigInteger.valueOf(((Number) val).longValue());
                }
                // don't check if it's a string in case of unchecked Number subclasses
                try {
                    // the other opt functions handle implicit conversions, i.e.
                    // jo.put("double",1.1d);
                    // jo.optInt("double"); -- will return 1, not an error
                    // this conversion to BigDecimal then to BigInteger is to maintain
                    // that type cast support that may truncate the decimal.
                    final String valStr = val.toString();
                    if(isDecimalNotation(valStr)) {
                        return new BigDecimal(valStr).toBigInteger();
                    }
                    return new BigInteger(valStr);
                } catch (Exception e) {
                    return defaultValue;
                }
            }

            /**
             * Get an optional double associated with a key, or NaN if there is no such
             * key or if its value is not a number. If the value is a string, an attempt
             * will be made to evaluate it as a number.
             *
             * @param key
             *            A string which is the key.
             * @return An object which is the value.
             */
            public double optDouble(String key) {
                return this.optDouble(key, Double.NaN);
            }

            /**
             * Get an optional double associated with a key, or the defaultValue if
             * there is no such key or if its value is not a number. If the value is a
             * string, an attempt will be made to evaluate it as a number.
             *
             * @param key
             *            A key string.
             * @param defaultValue
             *            The default.
             * @return An object which is the value.
             */
            public double optDouble(String key, double defaultValue) {
                Object val = this.opt(key);
                if (NULL.equals(val)) {
                    return defaultValue;
                }
                if (val instanceof Number){
                    return ((Number) val).doubleValue();
                }
                if (val instanceof String) {
                    try {
                        return Double.parseDouble((String) val);
                    } catch (Exception e) {
                        return defaultValue;
                    }
                }
                return defaultValue;
            }

            /**
             * Get the optional double value associated with an index. NaN is returned
             * if there is no value for the index, or if the value is not a number and
             * cannot be converted to a number.
             *
             * @param key
             *            A key string.
             * @return The value.
             */
            public float optFloat(String key) {
                return this.optFloat(key, Float.NaN);
            }

            /**
             * Get the optional double value associated with an index. The defaultValue
             * is returned if there is no value for the index, or if the value is not a
             * number and cannot be converted to a number.
             *
             * @param key
             *            A key string.
             * @param defaultValue
             *            The default value.
             * @return The value.
             */
            public float optFloat(String key, float defaultValue) {
                Object val = this.opt(key);
                if (Variant.NULL.equals(val)) {
                    return defaultValue;
                }
                if (val instanceof Number){
                    return ((Number) val).floatValue();
                }
                if (val instanceof String) {
                    try {
                        return Float.parseFloat((String) val);
                    } catch (Exception e) {
                        return defaultValue;
                    }
                }
                return defaultValue;
            }

            /**
             * Get an optional int value associated with a key, or zero if there is no
             * such key or if the value is not a number. If the value is a string, an
             * attempt will be made to evaluate it as a number.
             *
             * @param key
             *            A key string.
             * @return An object which is the value.
             */
            public int optInt(String key) {
                return this.optInt(key, 0);
            }

            /**
             * Get an optional int value associated with a key, or the default if there
             * is no such key or if the value is not a number. If the value is a string,
             * an attempt will be made to evaluate it as a number.
             *
             * @param key
             *            A key string.
             * @param defaultValue
             *            The default.
             * @return An object which is the value.
             */
            public int optInt(String key, int defaultValue) {
                Object val = this.opt(key);
                if (NULL.equals(val)) {
                    return defaultValue;
                }
                if (val instanceof Number){
                    return ((Number) val).intValue();
                }

                if (val instanceof String) {
                    try {
                        return new BigDecimal((String) val).intValue();
                    } catch (Exception e) {
                        return defaultValue;
                    }
                }
                return defaultValue;
            }

            /**
             * Get an optional VariantList associated with a key. It returns null if there
             * is no such key, or if its value is not a VariantList.
             *
             * @param key
             *            A key string.
             * @return A VariantList which is the value.
             */
            public VariantList optJSONElements(String key) {
                Object o = this.opt(key);
                return o instanceof VariantList ? (VariantList) o : null;
            }

            /**
             * Get an optional Variant associated with a key. It returns null if
             * there is no such key, or if its value is not a Variant.
             *
             * @param key
             *            A key string.
             * @return A Variant which is the value.
             */
            public Variant optJSONValue(String key) {
                Object object = this.opt(key);
                return object instanceof Variant ? (Variant) object : null;
            }

            /**
             * Get an optional long value associated with a key, or zero if there is no
             * such key or if the value is not a number. If the value is a string, an
             * attempt will be made to evaluate it as a number.
             *
             * @param key
             *            A key string.
             * @return An object which is the value.
             */
            public long optLong(String key) {
                return this.optLong(key, 0);
            }

            /**
             * Get an optional long value associated with a key, or the default if there
             * is no such key or if the value is not a number. If the value is a string,
             * an attempt will be made to evaluate it as a number.
             *
             * @param key
             *            A key string.
             * @param defaultValue
             *            The default.
             * @return An object which is the value.
             */
            public long optLong(String key, long defaultValue) {
                Object val = this.opt(key);
                if (NULL.equals(val)) {
                    return defaultValue;
                }
                if (val instanceof Number){
                    return ((Number) val).longValue();
                }

                if (val instanceof String) {
                    try {
                        return new BigDecimal((String) val).longValue();
                    } catch (Exception e) {
                        return defaultValue;
                    }
                }
                return defaultValue;
            }

            /**
             * Get an optional {@link Number} value associated with a key, or <code>null</code>
             * if there is no such key or if the value is not a number. If the value is a string,
             * an attempt will be made to evaluate it as a number ({@link BigDecimal}). This method
             * would be used in cases where type coercion of the number value is unwanted.
             *
             * @param key
             *            A key string.
             * @return An object which is the value.
             */
            public Number optNumber(String key) {
                return this.optNumber(key, null);
            }

            /**
             * Get an optional {@link Number} value associated with a key, or the default if there
             * is no such key or if the value is not a number. If the value is a string,
             * an attempt will be made to evaluate it as a number. This method
             * would be used in cases where type coercion of the number value is unwanted.
             *
             * @param key
             *            A key string.
             * @param defaultValue
             *            The default.
             * @return An object which is the value.
             */
            public Number optNumber(String key, Number defaultValue) {
                Object val = this.opt(key);
                if (NULL.equals(val)) {
                    return defaultValue;
                }
                if (val instanceof Number){
                    return (Number) val;
                }

                if (val instanceof String) {
                    try {
                        return stringToNumber((String) val);
                    } catch (Exception e) {
                        return defaultValue;
                    }
                }
                return defaultValue;
            }

            /**
             * Get an optional string associated with a key. It returns an empty string
             * if there is no such key. If the value is not a string and is not null,
             * then it is converted to a string.
             *
             * @param key
             *            A key string.
             * @return A string which is the value.
             */
            public String optString(String key) {
                return this.optString(key, "");
            }

            /**
             * Get an optional string associated with a key. It returns the defaultValue
             * if there is no such key.
             *
             * @param key
             *            A key string.
             * @param defaultValue
             *            The default.
             * @return A string which is the value.
             */
            public String optString(String key, String defaultValue) {
                Object object = this.opt(key);
                return NULL.equals(object) ? defaultValue : object.toString();
            }

            /**
             * Populates the internal map of the Variant with the bean values.
             * The bean can not be recursive.
             *
             * @see Variant#Variant(Object)
             *
             * @param bean
             *            the bean
             */
            private void populateMap(Object bean) {
                Class<?> klass = bean.getClass();

        // If klass is a System class then set includeSuperClass to false.

                boolean includeSuperClass = klass.getClassLoader() != null;

                Method[] methods = includeSuperClass ? klass.getMethods() : klass
                        .getDeclaredMethods();
                for (final Method method : methods) {
                    final int modifiers = method.getModifiers();
                    if (Modifier.isPublic(modifiers)
                            && !Modifier.isStatic(modifiers)
                            && method.getParameterTypes().length == 0
                            && !method.isBridge()
                            && method.getReturnType() != Void.TYPE ) {
                        final String name = method.getName();
                        String key;
                        if (name.startsWith("getKey")) {
                            if ("getClass".equals(name) || "getDeclaringClass".equals(name)) {
                                continue;
                            }
                            key = name.substring(3);
                        } else if (name.startsWith("is")) {
                            key = name.substring(2);
                        } else {
                            continue;
                        }
                        if (key.length() > 0
                                && Character.isUpperCase(key.charAt(0))) {
                            if (key.length() == 1) {
                                key = key.toLowerCase(Locale.ROOT);
                            } else if (!Character.isUpperCase(key.charAt(1))) {
                                key = key.substring(0, 1).toLowerCase(Locale.ROOT)
                                        + key.substring(1);
                            }

                            try {
                                final Object result = method.invoke(bean);
                                if (result != null) {
                                    this.map.put(key, wrap(result));
                                    // we don't use the result anywhere outside of wrap
                                    // if it's a resource we should be sure to close it after calling toString
                                    if(result instanceof Closeable) {
                                        try {
                                            ((Closeable)result).close();
                                        } catch (IOException ignore) {
                                        }
                                    }
                                }
                            } catch (IllegalAccessException ignore) {
                            } catch (IllegalArgumentException ignore) {
                            } catch (InvocationTargetException ignore) {
                            }
                        }
                    }
                }
            }

            /**
             * Put a key/boolean pair in the Variant.
             *
             * @param key
             *            A key string.
             * @param value
             *            A boolean which is the value.
             * @return this.
             * @throws Fault
             *             If the key is null.
             */
            public Variant put(String key, boolean value) throws Fault {
                this.put(key, value ? Boolean.TRUE : Boolean.FALSE);
                return this;
            }

            /**
             * Put a key/value pair in the Variant, where the value will be a
             * VariantList which is produced from a Collection.
             *
             * @param key
             *            A key string.
             * @param value
             *            A Collection value.
             * @return this.
             * @throws Fault
             */
            public Variant put(String key, Collection<?> value) throws Fault {
                this.put(key, new VariantList(value));
                return this;
            }

            /**
             * Put a key/double pair in the Variant.
             *
             * @param key
             *            A key string.
             * @param value
             *            A double which is the value.
             * @return this.
             * @throws Fault
             *             If the key is null or if the number is invalid.
             */
            public Variant put(String key, double value) throws Fault {
                this.put(key, Double.valueOf(value));
                return this;
            }

            /**
             * Put a key/float pair in the Variant.
             *
             * @param key
             *            A key string.
             * @param value
             *            A float which is the value.
             * @return this.
             * @throws Fault
             *             If the key is null or if the number is invalid.
             */
            public Variant put(String key, float value) throws Fault {
                this.put(key, Float.valueOf(value));
                return this;
            }

            /**
             * Put a key/int pair in the Variant.
             *
             * @param key
             *            A key string.
             * @param value
             *            An int which is the value.
             * @return this.
             * @throws Fault
             *             If the key is null.
             */
            public Variant put(String key, int value) throws Fault {
                this.put(key, Integer.valueOf(value));
                return this;
            }

            /**
             * Put a key/long pair in the Variant.
             *
             * @param key
             *            A key string.
             * @param value
             *            A long which is the value.
             * @return this.
             * @throws Fault
             *             If the key is null.
             */
            public Variant put(String key, long value) throws Fault {
                this.put(key, Long.valueOf(value));
                return this;
            }

            /**
             * Put a key/value pair in the Variant, where the value will be a
             * Variant which is produced from a Map.
             *
             * @param key
             *            A key string.
             * @param value
             *            A Map value.
             * @return this.
             * @throws Fault
             */
            public Variant put(String key, Map<?, ?> value) throws Fault {
                this.put(key, new Variant(value));
                return this;
            }

            /**
             * Put a key/value pair in the Variant, where the value will be a
             * Variant which is produced from an Extended Map.
             *
             * @param key
             *            A key string.
             * @param value
             *            A Map value.
             * @return this.
             * @throws Fault
             */
            public Variant put(String key, Class<? extends HashMap<?, ?>> value) throws Fault {
                this.put(key, new Variant(value));
                return this;
            }

            /**
             * Put a key/value pair in the Variant. If the value is null, then the
             * key will be removed from the Variant if it is present.
             *
             * @param key
             *            A key string.
             * @param value
             *            An object which is the value. It should be of one of these
             *            types: Boolean, Double, Integer, VariantList, Variant, Long,
             *            String, or the Variant.NULL object.
             * @return this.
             * @throws Fault
             *             If the value is non-finite number or if the key is null.
             */
            public Variant put(String key, Object value) throws Fault {
                if (key == null) {
                    throw new NullPointerException("Null key.");
                }
                if (value != null) {
                    testValidity(value);
                    this.map.put(key, value);
                } else {
                    this.remove(key);
                }
                return this;
            }

            /**
             * Put a key/value pair in the Variant, but only if the key and the value
             * are both non-null, and only if there is not already a member with that
             * name.
             *
             * @param key string
             * @param value object
             * @return this.
             * @throws Fault
             *             if the key is a duplicate
             */
            public Variant putOnce(String key, Object value) throws Fault {
                if (key != null && value != null) {
                    if (this.opt(key) != null) {
                        throw new Fault("Duplicate key \"" + key + "\"");
                    }
                    this.put(key, value);
                }
                return this;
            }

            /**
             * Put a key/value pair in the Variant, but only if the key and the value
             * are both non-null.
             *
             * @param key
             *            A key string.
             * @param value
             *            An object which is the value. It should be of one of these
             *            types: Boolean, Double, Integer, VariantList, Variant, Long,
             *            String, or the Variant.NULL object.
             * @return this.
             * @throws Fault
             *             If the value is a non-finite number.
             */
            public Variant putOpt(String key, Object value) throws Fault {
                if (key != null && value != null) {
                    this.put(key, value);
                }
                return this;
            }

            /**
             * Creates a Path using an initialization string and tries to
             * match it to an item within this Variant. For example, given a
             * Variant initialized with this document:
             * <pre>
             * {
             *     "a":{"b":"c"}
             * }
             * </pre>
             * and this Path string:
             * <pre>
             * "/a/b"
             * </pre>
             * Then this method will return the String "c".
             * A Fault may be thrown from code called by this method.
             *
             * @param jsonPointer string that can be used to create a Path
             * @return the item matched by the Path, otherwise null
             */
            public Object query(String jsonPointer) {
                return query(new Path(jsonPointer));
            }
            /**
             * Uses a user initialized Path  and tries to
             * match it to an item within this Variant. For example, given a
             * Variant initialized with this document:
             * <pre>
             * {
             *     "a":{"b":"c"}
             * }
             * </pre>
             * and this Path:
             * <pre>
             * "/a/b"
             * </pre>
             * Then this method will return the String "c".
             * A Fault may be thrown from code called by this method.
             *
             * @param path string that can be used to create a Path
             * @return the item matched by the Path, otherwise null
             */
            public Object query(Path path) {
                return path.queryFrom(this);
            }

            /**
             * Queries and returns a value from this object using {@code jsonPointer}, or
             * returns null if the query fails due to a missing key.
             *
             * @param jsonPointer the string representation of the JSON pointer
             * @return the queried value or {@code null}
             * @throws IllegalArgumentException if {@code jsonPointer} has invalid syntax
             */
            public Object optQuery(String jsonPointer) {
              return optQuery(new Path(jsonPointer));
            }

            /**
             * Queries and returns a value from this object using {@code path}, or
             * returns null if the query fails due to a missing key.
             *
             * @param path The JSON pointer
             * @return the queried value or {@code null}
             * @throws IllegalArgumentException if {@code path} has invalid syntax
             */
            public Object optQuery(Path path) {
                try {
                    return path.queryFrom(this);
                } catch (XPR.Fault e) {
                    return null;
                }
            }

            /**
             * Produce a string in double quotes with backslash sequences in all the
             * right places. A backslash will be inserted within </, producing <\/,
             * allowing JSON text to be delivered in HTML. In JSON text, a string cannot
             * contain a control character or an unescaped quote or backslash.
             *
             * @param string
             *            A String
             * @return A String correctly formatted for insertion in a JSON text.
             */
            public static String quote(String string) {
                StringWriter sw = new StringWriter();
                synchronized (sw.getBuffer()) {
                    try {
                        return quote(string, sw).toString();
                    } catch (IOException ignored) {
                        // will never happen - we are writing to a string writer
                        return "";
                    }
                }
            }

            public static java.io.Writer quote(String string, java.io.Writer w) throws IOException {
                if (string == null || string.length() == 0) {
                    w.write("\"\"");
                    return w;
                }

                char b;
                char c = 0;
                String hhhh;
                int i;
                int len = string.length();

                w.write('"');
                for (i = 0; i < len; i += 1) {
                    b = c;
                    c = string.charAt(i);
                    switch (c) {
                    case '\\':
                    case '"':
                        w.write('\\');
                        w.write(c);
                        break;
                    case '/':
                        if (b == '<') {
                            w.write('\\');
                        }
                        w.write(c);
                        break;
                    case '\b':
                        w.write("\\b");
                        break;
                    case '\t':
                        w.write("\\t");
                        break;
                    case '\n':
                        w.write("\\n");
                        break;
                    case '\f':
                        w.write("\\f");
                        break;
                    case '\r':
                        w.write("\\r");
                        break;
                    default:
                        if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
                                || (c >= '\u2000' && c < '\u2100')) {
                            w.write("\\u");
                            hhhh = Integer.toHexString(c);
                            w.write("0000", 0, 4 - hhhh.length());
                            w.write(hhhh);
                        } else {
                            w.write(c);
                        }
                    }
                }
                w.write('"');
                return w;
            }

            /**
             * Remove a name and its value, if present.
             *
             * @param key
             *            The name to be removed.
             * @return The value that was associated with the name, or null if there was
             *         no value.
             */
            public Object remove(String key) {
                return this.map.remove(key);
            }

            /**
             * Determine if two VariantList are similar.
             * They must contain the same set of names which must be associated with
             * similar values.
             *
             * @param other The other Variant
             * @return true if they are equal
             */
            public boolean similar(Object other) {
                try {
                    if (!(other instanceof Variant)) {
                        return false;
                    }
                    if (!this.keySet().equals(((Variant)other).keySet())) {
                        return false;
                    }
                    for (final Map.Entry<String,?> entry : this.entrySet()) {
                        String name = entry.getKey();
                        Object valueThis = entry.getValue();
                        Object valueOther = ((Variant)other).get(name);
                        if(valueThis == valueOther) {
                          return true;
                        }
                        if(valueThis == null) {
                          return false;
                        }
                        if (valueThis instanceof Variant) {
                            if (!((Variant)valueThis).similar(valueOther)) {
                                return false;
                            }
                        } else if (valueThis instanceof VariantList) {
                            if (!((VariantList)valueThis).similar(valueOther)) {
                                return false;
                            }
                        } else if (!valueThis.equals(valueOther)) {
                            return false;
                        }
                    }
                    return true;
                } catch (Throwable exception) {
                    return false;
                }
            }

            /**
             * Tests if the value should be tried as a decimal. It makes no test if there are actual digits.
             *
             * @param val value to test
             * @return true if the string is "-0" or if it contains '.', 'e', or 'E', false otherwise.
             */
            protected static boolean isDecimalNotation(final String val) {
                return val.indexOf('.') > -1 || val.indexOf('e') > -1
                        || val.indexOf('E') > -1 || "-0".equals(val);
            }

            /**
             * Converts a string to a number using the narrowest possible type. Possible
             * returns for this function are BigDecimal, Double, BigInteger, Long, and Integer.
             * When a Double is returned, it should always be a valid Double and not NaN or +-infinity.
             *
             * @param val value to convert
             * @return Number representation of the value.
             * @throws NumberFormatException thrown if the value is not a valid number. A public
             *      caller should catch this and wrap it in a {@link Fault} if applicable.
             */
            protected static Number stringToNumber(final String val) throws NumberFormatException {
                char initial = val.charAt(0);
                if ((initial >= '0' && initial <= '9') || initial == '-') {
                    // decimal representation
                    if (isDecimalNotation(val)) {
                        // quick dirty way to see if we need a BigDecimal instead of a Double
                        // this only handles some cases of overflow or underflow
                        if (val.length()>14) {
                            return new BigDecimal(val);
                        }
                        final Double d = Double.valueOf(val);
                        if (d.isInfinite() || d.isNaN()) {
                            // if we can't parse it as a double, go up to BigDecimal
                            // this is probably due to underflow like 4.32e-678
                            // or overflow like 4.65e5324. The size of the string is small
                            // but can't be held in a Double.
                            return new BigDecimal(val);
                        }
                        return d;
                    }
                    // integer representation.
                    // This will narrow any values to the smallest reasonable Object representation
                    // (Integer, Long, or BigInteger)

                    // string version
                    // The compare string length method reduces GC,
                    // but leads to smaller integers being placed in larger wrappers even though not
                    // needed. i.e. 1,000,000,000 -> Long even though it's an Integer
                    // 1,000,000,000,000,000,000 -> BigInteger even though it's a Long
                    //if(val.length()<=9){
                    //    return Integer.valueOf(val);
                    //}
                    //if(val.length()<=18){
                    //    return Long.valueOf(val);
                    //}
                    //return new BigInteger(val);

                    // BigInteger version: We use a similar bitLenth compare as
                    // BigInteger#intValueExact uses. Increases GC, but objects hold
                    // only what they need. i.e. Less runtime overhead if the value is
                    // long lived. Which is the better tradeoff? This is closer to what's
                    // in stringToValue.
                    BigInteger bi = new BigInteger(val);
                    if(bi.bitLength()<=31){
                        return Integer.valueOf(bi.intValue());
                    }
                    if(bi.bitLength()<=63){
                        return Long.valueOf(bi.longValue());
                    }
                    return bi;
                }
                throw new NumberFormatException("val ["+val+"] is not a valid number.");
            }

            /**
             * Try to convert a string into a number, boolean, or null. If the string
             * can't be converted, return the string.
             *
             * @param string
             *            A String.
             * @return A simple JSON value.
             */
            public static Object stringToValue(String string) {
                if (string.equals("")) {
                    return string;
                }
                if (string.equalsIgnoreCase("true")) {
                    return Boolean.TRUE;
                }
                if (string.equalsIgnoreCase("false")) {
                    return Boolean.FALSE;
                }
                if (string.equalsIgnoreCase("null")) {
                    return Variant.NULL;
                }

                /*
                 * If it might be a number, try converting it. If a number cannot be
                 * produced, then the value will just be a string.
                 */

                char initial = string.charAt(0);
                if ((initial >= '0' && initial <= '9') || initial == '-') {
                    try {
                        // if we want full Big Number support this block can be replaced with:
                        // return stringToNumber(string);
                        if (isDecimalNotation(string)) {
                            Double d = Double.valueOf(string);
                            if (!d.isInfinite() && !d.isNaN()) {
                                return d;
                            }
                        } else {
                            Long myLong = Long.valueOf(string);
                            if (string.equals(myLong.toString())) {
                                if (myLong.longValue() == myLong.intValue()) {
                                    return Integer.valueOf(myLong.intValue());
                                }
                                return myLong;
                            }
                        }
                    } catch (Exception ignore) {
                    }
                }
                return string;
            }

            /**
             * Throw an exception if the object is a NaN or infinite number.
             *
             * @param o
             *            The object to test.
             * @throws Fault
             *             If o is a non-finite number.
             */
            public static void testValidity(Object o) throws Fault {
                if (o != null) {
                    if (o instanceof Double) {
                        if (((Double) o).isInfinite() || ((Double) o).isNaN()) {
                            throw new Fault(
                                    "JSON does not allow non-finite numbers.");
                        }
                    } else if (o instanceof Float) {
                        if (((Float) o).isInfinite() || ((Float) o).isNaN()) {
                            throw new Fault(
                                    "JSON does not allow non-finite numbers.");
                        }
                    }
                }
            }

            /**
             * Produce a VariantList containing the values of the members of this
             * Variant.
             *
             * @param names
             *            A VariantList containing a list of key strings. This determines
             *            the sequence of the values in the result.
             * @return A VariantList of values.
             * @throws Fault
             *             If any of the values are non-finite numbers.
             */
            public VariantList toJSONElements(VariantList names) throws Fault {
                if (names == null || names.length() == 0) {
                    return null;
                }
                VariantList ja = new VariantList();
                for (int i = 0; i < names.length(); i += 1) {
                    ja.put(this.opt(names.getString(i)));
                }
                return ja;
            }

            /**
             * Make a JSON text of this Variant. For compactness, no whitespace is
             * added. If this would not result in a syntactically correct JSON text,
             * then null will be returned instead.
             * <p><b>
             * Warning: This method assumes that the data structure is acyclical.
             * </b>
             *
             * @return a printable, displayable, portable, transmittable representation
             *         of the object, beginning with <code>{</code>&nbsp;<small>(left
             *         brace)</small> and ending with <code>}</code>&nbsp;<small>(right
             *         brace)</small>.
             */
            @Override
            public String toString() {
                try {
                    return this.toString(0);
                } catch (Exception e) {
                    return null;
                }
            }

            /**
             * Make a pretty-printed JSON text of this Variant.
             *
             * <p>If <code>indentFactor > 0</code> and the {@link Variant}
             * has only one key, then the object will be output on a single line:
             * <pre>{@code {"key": 1}}</pre>
             *
             * <p>If an object has 2 or more keys, then it will be output across
             * multiple lines: <code><pre>{
             *  "key1": 1,
             *  "key2": "value 2",
             *  "key3": 3
             * }</pre></code>
             * <p><b>
             * Warning: This method assumes that the data structure is acyclical.
             * </b>
             *
             * @param indentFactor
             *            The number of spaces to add to each level of indentation.
             * @return a printable, displayable, portable, transmittable representation
             *         of the object, beginning with <code>{</code>&nbsp;<small>(left
             *         brace)</small> and ending with <code>}</code>&nbsp;<small>(right
             *         brace)</small>.
             * @throws Fault
             *             If the object contains an invalid number.
             */
            public String toString(int indentFactor) throws Fault {
                StringWriter w = new StringWriter();
                synchronized (w.getBuffer()) {
                    return this.write(w, indentFactor, 0).toString();
                }
            }

            /**
             * Make a JSON text of an Object value. If the object has an
             * value.toJSON() method, then that method will be used to produce the
             * JSON text. The method is required to produce a strictly conforming text.
             * If the object does not contain a toJSON method (which is the most
             * common case), then a text will be produced by other means. If the value
             * is an array or Collection, then a VariantList will be made from it and its
             * toJSON method will be called. If the value is a MAP, then a
             * Variant will be made from it and its toJSON method will be
             * called. Otherwise, the value's toString method will be called, and the
             * result will be quoted.
             *
             * <p>
             * Warning: This method assumes that the data structure is acyclical.
             *
             * @param value
             *            The value to be serialized.
             * @return a printable, displayable, transmittable representation of the
             *         object, beginning with <code>{</code>&nbsp;<small>(left
             *         brace)</small> and ending with <code>}</code>&nbsp;<small>(right
             *         brace)</small>.
             * @throws Fault
             *             If the value is or contains an invalid number.
             */
            public static String valueToString(Object value) throws Fault {
                if (value == null || value.equals(null)) {
                    return "null";
                }
                //if (value instanceof JSONDeflatable)
                  //  return valueToString(JSONDeflatable.class.cast(value).onJsonDeflate());
                if (value instanceof Serialization) {
                    Object object;
                    try {
                        object = ((Serialization) value).toJSON();
                    } catch (Exception e) {
                        throw new Fault(e);
                    }
                    if (object instanceof String) {
                        return (String) object;
                    }
                    throw new Fault("Bad value from toJSON: " + object);
                }
                if (value instanceof Number) {
                    // not all Numbers may match actual JSON Numbers. i.e. Fractions or Complex
                    final String numberAsString = numberToString((Number) value);
                    try {
                        // Use the BigDecimal constructor for it's parser to validate the format.
                        @SuppressWarnings("unused")
                        BigDecimal unused = new BigDecimal(numberAsString);
                        // Close enough to a JSON number that we will return it unquoted
                        return numberAsString;
                    } catch (NumberFormatException ex){
                        // The Number value is not a valid JSON number.
                        // Instead we will quote it as a string
                        return quote(numberAsString);
                    }
                }
                if (value instanceof Boolean || value instanceof Variant
                        || value instanceof VariantList) {
                    return value.toString();
                }
                if (value instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) value;
                    return new Variant(map).toString();
                }
                if (value instanceof Collection) {
                    Collection<?> coll = (Collection<?>) value;
                    return new VariantList(coll).toString();
                }
                if (value.getClass().isArray()) {
                    return new VariantList(value).toString();
                }
                if(value instanceof Enum<?>){
                    return quote(((Enum<?>)value).name());
                }
                return quote(value.toString());
            }

            /**
             * Wrap an object, if necessary. If the object is null, return the NULL
             * object. If it is a JSONBubble, recursively burst it. If it is an array or collection,
             * wrap it in a VariantList. If it is a map, wrap it in a Variant. If it is a standard
             * property (Double, String, et al) then it is already wrapped. Otherwise, if it comes from
             * one of the java packages, turn it into a string. And if it doesn't, try
             * to wrap it in a Variant. If the wrapping fails, then null is returned.
             *
             * @param object
             *            The object to wrap
             * @return The wrapped value
             */
            public static Object wrap(Object object) {
                try {
                    if (object == null) {
                        return NULL;
                    }
                    if (object instanceof Variant || object instanceof VariantList
                            || NULL.equals(object) || object instanceof Serialization
                            || object instanceof Byte || object instanceof Character
                            || object instanceof Short || object instanceof Integer
                            || object instanceof Long || object instanceof Boolean
                            || object instanceof Float || object instanceof Double
                            || object instanceof String || object instanceof BigInteger
                            || object instanceof BigDecimal || object instanceof Enum) {
                        return object;
                    }

                    if (object instanceof Collection) {
                        Collection<?> coll = (Collection<?>) object;
                        return new VariantList(coll);
                    }
                    if (object.getClass().isArray()) {
                        return new VariantList(object);
                    }
                    if (object instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) object;
                        return new Variant(map);
                    }
                    Package objectPackage = object.getClass().getPackage();
                    String objectPackageName = objectPackage != null ? objectPackage
                            .getName() : "";
                    if (objectPackageName.startsWith("java.")
                            || objectPackageName.startsWith("javax.")
                            || object.getClass().getClassLoader() == null) {
                        return object.toString();
                    }
                    return new Variant(object);
                } catch (Exception exception) {
                    return null;
                }
            }

            /**
             * Write the contents of the Variant as JSON text to a writer. For
             * compactness, no whitespace is added.
             * <p><b>
             * Warning: This method assumes that the data structure is acyclical.
             * </b>
             *
             * @return The writer.
             * @throws Fault
             */
            public java.io.Writer write(java.io.Writer writer) throws Fault {
                return this.write(writer, 0, 0);
            }

            static final java.io.Writer writeValue(java.io.Writer writer, Object value,
                    int indentFactor, int indent) throws Fault, IOException {
                if (value == null || value.equals(null)) {
                    writer.write("null");
                } else if (value instanceof Serialization) {
                    Object o;
                    try {
                        o = ((Serialization) value).toJSON();
                    } catch (Exception e) {
                        throw new Fault(e);
                    }
                    writer.write(o != null ? o.toString() : quote(value.toString()));
                } else if (value instanceof Number) {
                    // not all Numbers may match actual JSON Numbers. i.e. fractions or Imaginary
                    final String numberAsString = numberToString((Number) value);
                    try {
                        // Use the BigDecimal constructor for it's parser to validate the format.
                        @SuppressWarnings("unused")
                        BigDecimal testNum = new BigDecimal(numberAsString);
                        // Close enough to a JSON number that we will use it unquoted
                        writer.write(numberAsString);
                    } catch (NumberFormatException ex){
                        // The Number value is not a valid JSON number.
                        // Instead we will quote it as a string
                        quote(numberAsString, writer);
                    }
                } else if (value instanceof Boolean) {
                    writer.write(value.toString());
                } else if (value instanceof Enum<?>) {
                    writer.write(quote(((Enum<?>)value).name()));
                } else if (value instanceof Variant) {
                    ((Variant) value).write(writer, indentFactor, indent);
                } else if (value instanceof VariantList) {
                    ((VariantList) value).write(writer, indentFactor, indent);
                } else if (value instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) value;
                    new Variant(map).write(writer, indentFactor, indent);
                } else if (value instanceof Collection) {
                    Collection<?> coll = (Collection<?>) value;
                    new VariantList(coll).write(writer, indentFactor, indent);
                } else if (value.getClass().isArray()) {
                    new VariantList(value).write(writer, indentFactor, indent);
                } else {
                    quote(value.toString(), writer);
                }
                return writer;
            }

            static final void indent(java.io.Writer writer, int indent) throws IOException {
                for (int i = 0; i < indent; i += 1) {
                    writer.write(' ');
                }
            }

            /**
             * Write the contents of the Variant as JSON text to a writer.
             *
             * <p>If <code>indentFactor > 0</code> and the {@link Variant}
             * has only one key, then the object will be output on a single line:
             * <pre>{@code {"key": 1}}</pre>
             *
             * <p>If an object has 2 or more keys, then it will be output across
             * multiple lines: <code><pre>{
             *  "key1": 1,
             *  "key2": "value 2",
             *  "key3": 3
             * }</pre></code>
             * <p><b>
             * Warning: This method assumes that the data structure is acyclical.
             * </b>
             *
             * @param writer
             *            Writes the serialized JSON
             * @param indentFactor
             *            The number of spaces to add to each level of indentation.
             * @param indent
             *            The indentation of the top level.
             * @return The writer.
             * @throws Fault
             */
            public java.io.Writer write(java.io.Writer writer, int indentFactor, int indent)
                    throws Fault
            {
                try {
                    boolean commanate = false;
                    final int length = this.length();
                    writer.write('{');

                    if (length == 1) {
                      final Map.Entry<String,?> entry = this.entrySet().iterator().next();
                        final String key = entry.getKey();
                        writer.write(quote(key));
                        writer.write(':');
                        if (indentFactor > 0) {
                            writer.write(' ');
                        }
                        try{
                            writeValue(writer, entry.getValue(), indentFactor, indent);
                        } catch (Exception e) {
                            throw new Fault("Unable to write Variant value for key: " + key, e);
                        }
                    } else if (length != 0) {
                        final int newindent = indent + indentFactor;
                        for (final Map.Entry<String,?> entry : this.entrySet()) {
                            if (commanate) {
                                writer.write(',');
                            }
                            if (indentFactor > 0) {
                                writer.write('\n');
                            }
                            indent(writer, newindent);
                            final String key = entry.getKey();
                            writer.write(quote(key));
                            writer.write(':');
                            if (indentFactor > 0) {
                                writer.write(' ');
                            }
                            try {
                                writeValue(writer, entry.getValue(), indentFactor, newindent);
                            } catch (Exception e) {
                                throw new Fault("Unable to write Variant value for key: " + key, e);
                            }
                            commanate = true;
                        }
                        if (indentFactor > 0) {
                            writer.write('\n');
                        }
                        indent(writer, indent);
                    }
                    writer.write('}');
                    return writer;
                } catch (IOException exception) {
                    throw new Fault(exception);
                }
            }

            /**
             * Returns a java.util.Map containing all of the entries in this object.
             * If an entry in the object is a VariantList or Variant it will also
             * be converted.
             * <p>
             * Warning: This method assumes that the data structure is acyclical.
             *
             * @return a java.util.Map containing the entries of this object
             */
            public Map<String, Object> toMap() {
                Map<String, Object> results = new HashMap<String, Object>();
                for (Map.Entry<String, Object> entry : this.entrySet()) {
                    Object value;
                    if (entry.getValue() == null || NULL.equals(entry.getValue())) {
                        value = null;
                    } else if (entry.getValue() instanceof Variant) {
                        value = ((Variant) entry.getValue()).toMap();
                    } else if (entry.getValue() instanceof VariantList) {
                        value = ((VariantList) entry.getValue()).toList();
                    } else {
                        value = entry.getValue();
                    }
                    results.put(entry.getKey(), value);
                }
                return results;
            }

        }

        /**
         * A JSON Pointer is a simple query language defined for JSON documents by
         * <a href="https://tools.ietf.org/html/rfc6901">RFC 6901</a>.
         *
         * In a nutshell, Path allows the user to navigate into a JSON document
         * using strings, and retrieve targeted objects, like a simple form of XPATH.
         * Path segments are separated by the '/' char, which signifies the root of
         * the document when it appears as the first char of the string. Array
         * elements are navigated using ordinals, counting from 0. Path strings
         * may be extended to any arbitrary number of segments. If the navigation
         * is successful, the matched item is returned. A matched item may be a
         * Variant, a VariantList, or a JSON value. If the Path string building
         * fails, an appropriate exception is thrown. If the navigation fails to find
         * a match, a Fault is thrown.
         *
         * @author JSON.org
         * @version 2016-05-14
         */
        public static class Path {

            // used for URL encoding and decoding
            private static final String ENCODING = "utf-8";

            /**
             * This class allows the user to build a Path in steps, using
             * exactly one segment in each step.
             */
            public static class Builder {

                // Segments for the eventual Path string
                private final List<String> refTokens = new ArrayList<String>();

                /**
                 * Creates a {@code Path} instance using the tokens previously set using the
                 * {@link #add(String)} method calls.
                 */
                public Path build() {
                    return new Path(this.refTokens);
                }

                /**
                 * Adds an arbitrary token to the list of reference tokens. It can be any non-null value.
                 *
                 * Unlike in the case of JSON string or URI fragment representation of JSON pointers, the
                 * argument of this method MUST NOT be escaped. If you want to query the property called
                 * {@code "a~b"} then you should simply pass the {@code "a~b"} string as-is, there is no
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
                 * Adds an integer to the reference token list. Although not necessarily, mostly this token will
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

            /**
             * Static factory method for {@link Builder}. Example usage:
             *
             * <pre><code>
             * Path pointer = Path.builder()
             *       .add("obj")
             *       .add("other~key").add("another/key")
             *       .add("\"")
             *       .add(0)
             *       .build();
             * </code></pre>
             *
             *  @return a builder instance which can be used to construct a {@code Path} instance by chained
             *  {@link Builder#add(String)} calls.
             */
            public static Builder builder() {
                return new Builder();
            }

            // Segments for the Path string
            private final List<String> refTokens;

            /**
             * Pre-parses and initializes a new {@code Path} instance. If you want to
             * evaluate the same JSON Pointer on different JSON documents then it is recommended
             * to keep the {@code Path} instances due to performance considerations.
             *
             * @param pointer the JSON String or URI Fragment representation of the JSON pointer.
             * @throws IllegalArgumentException if {@code pointer} is not a valid JSON pointer
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
                    throw new IllegalArgumentException("a JSON pointer should start with '/' or '#/'");
                }
                this.refTokens = new ArrayList<String>();
                for (String token : refs.split("/")) {
                    this.refTokens.add(unescape(token));
                }
            }

            public Path(List<String> refTokens) {
                this.refTokens = new ArrayList<String>(refTokens);
            }

            private String unescape(String token) {
                return token.replace("~1", "/").replace("~0", "~")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
            }

            /**
             * Evaluates this JSON Pointer on the given {@code document}. The {@code document}
             * is usually a {@link Variant} or a {@link VariantList} instance, but the empty
             * JSON Pointer ({@code ""}) can be evaluated on any JSON values and in such case the
             * returned value will be {@code document} itself.
             *
             * @param document the JSON document which should be the subject of querying.
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
                                "value [%s] is not an array or object therefore its key %s cannot be resolved", current,
                                token));
                    }
                }
                return current;
            }

            /**
             * Matches a VariantList element by ordinal position
             * @param current the VariantList to be evaluated
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
                        throw new Fault(format("index %d is out of bounds - the array has %d elements", index,
                                currentArr.length()));
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
                for (String token: this.refTokens) {
                    rval.append('/').append(escape(token));
                }
                return rval.toString();
            }

            /**
             * Escapes path segment values to an unambiguous form.
             * The escape char to be inserted is '~'. The chars to be escaped
             * are ~, which maps to ~0, and /, which maps to ~1. Backslashes
             * and double quote chars are also escaped.
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

        }
    }

    /**
     * A Compiler takes a source string and extracts characters and tokens from
     * it. It is used by the Variant and VariantList constructors to parse
     * JSON source strings.
     * @author JSON.org
     * @version 2014-05-03
     */
    public static class Compiler {
        /** current read character position on the current line. */
        private long character;
        /** flag to indicate if the end of the input has been found. */
        private boolean eof;
        /** current read index of the input. */
        private long index;
        /** current line of the input. */
        private long line;
        /** previous character read from the input. */
        private char previous;
        /** Reader for the input. */
        private final Reader reader;
        /** flag to indicate that a previous character was requested. */
        private boolean usePrevious;
        /** the number of characters read in the previous line. */
        private long characterPreviousLine;
    
    
        /**
         * Construct a Compiler from a Reader. The caller must close the Reader.
         *
         * @param reader     A reader.
         */
        public Compiler(Reader reader) {
            this.reader = reader.markSupported()
                    ? reader
                            : new BufferedReader(reader);
            this.eof = false;
            this.usePrevious = false;
            this.previous = 0;
            this.index = 0;
            this.character = 1;
            this.characterPreviousLine = 0;
            this.line = 1;
        }
    
    
        /**
         * Construct a Compiler from an InputStream. The caller must close the input stream.
         * @param inputStream The source.
         */
        public Compiler(InputStream inputStream) {
            this(new InputStreamReader(inputStream));
        }
    
    
        /**
         * Construct a Compiler from a string.
         *
         * @param s     A source string.
         */
        public Compiler(String s) {
            this(new StringReader(s));
        }
    
    
        /**
         * Back up one character. This provides a sort of lookahead capability,
         * so that you can test for a digit or letter before attempting to parse
         * the next number or identifier.
         * @throws Fault Thrown if trying to step back more than 1 step
         *  or if already at the start of the string
         */
        public void back() throws Fault {
            if (this.usePrevious || this.index <= 0) {
                throw new Fault("Stepping back two steps is not supported");
            }
            this.decrementIndexes();
            this.usePrevious = true;
            this.eof = false;
        }
    
        /**
         * Decrements the indexes for the {@link #back()} method based on the previous character read.
         */
        private void decrementIndexes() {
            this.index--;
            if(this.previous=='\r' || this.previous == '\n') {
                this.line--;
                this.character=this.characterPreviousLine ;
            } else if(this.character > 0){
                this.character--;
            }
        }
    
        /**
         * Get the hex value of a character (base16).
         * @param c A character between '0' and '9' or between 'A' and 'F' or
         * between 'a' and 'f'.
         * @return  An int between 0 and 15, or -1 if c was not a hex digit.
         */
        public static int dehexchar(char c) {
            if (c >= '0' && c <= '9') {
                return c - '0';
            }
            if (c >= 'A' && c <= 'F') {
                return c - ('A' - 10);
            }
            if (c >= 'a' && c <= 'f') {
                return c - ('a' - 10);
            }
            return -1;
        }
    
        /**
         * Checks if the end of the input has been reached.
         *
         * @return true if at the end of the file and we didn't step back
         */
        public boolean end() {
            return this.eof && !this.usePrevious;
        }
    
    
        /**
         * Determine if the source string still contains characters that next()
         * can consume.
         * @return true if not yet at the end of the source.
         * @throws Fault thrown if there is an error stepping forward
         *  or backward while checking for more data.
         */
        public boolean more() throws Fault {
            if(this.usePrevious) {
                return true;
            }
            try {
                this.reader.mark(1);
            } catch (IOException e) {
                throw new Fault("Unable to preserve stream position", e);
            }
            try {
                // -1 is EOF, but next() can not consume the null character '\0'
                if(this.reader.read() <= 0) {
                    this.eof = true;
                    return false;
                }
                this.reader.reset();
            } catch (IOException e) {
                throw new Fault("Unable to read the next character from the stream", e);
            }
            return true;
        }
    
    
        /**
         * Get the next character in the source string.
         *
         * @return The next character, or 0 if past the end of the source string.
         * @throws Fault Thrown if there is an error reading the source string.
         */
        public char next() throws Fault {
            int c;
            if (this.usePrevious) {
                this.usePrevious = false;
                c = this.previous;
            } else {
                try {
                    c = this.reader.read();
                } catch (IOException exception) {
                    throw new Fault(exception);
                }
            }
            if (c <= 0) { // End of stream
                this.eof = true;
                return 0;
            }
            this.incrementIndexes(c);
            this.previous = (char) c;
            return this.previous;
        }
    
        /**
         * Increments the internal indexes according to the previous character
         * read and the character passed as the current character.
         * @param c the current character read.
         */
        private void incrementIndexes(int c) {
            if(c > 0) {
                this.index++;
                if(c=='\r') {
                    this.line++;
                    this.characterPreviousLine = this.character;
                    this.character=0;
                }else if (c=='\n') {
                    if(this.previous != '\r') {
                        this.line++;
                        this.characterPreviousLine = this.character;
                    }
                    this.character=0;
                } else {
                    this.character++;
                }
            }
        }
    
        /**
         * Consume the next character, and check that it matches a specified
         * character.
         * @param c The character to match.
         * @return The character.
         * @throws Fault if the character does not match.
         */
        public char next(char c) throws Fault {
            char n = this.next();
            if (n != c) {
                if(n > 0) {
                    throw this.syntaxError("Expected '" + c + "' and instead saw '" +
                            n + "'");
                }
                throw this.syntaxError("Expected '" + c + "' and instead saw ''");
            }
            return n;
        }
    
    
        /**
         * Get the next n characters.
         *
         * @param n     The number of characters to take.
         * @return      A string of n characters.
         * @throws Fault
         *   Substring bounds error if there are not
         *   n characters remaining in the source string.
         */
        public String next(int n) throws Fault {
            if (n == 0) {
                return "";
            }
    
            char[] chars = new char[n];
            int pos = 0;
    
            while (pos < n) {
                chars[pos] = this.next();
                if (this.end()) {
                    throw this.syntaxError("Substring bounds error");
                }
                pos += 1;
            }
            return new String(chars);
        }
    
    
        /**
         * Get the next char in the string, skipping whitespace.
         * @throws Fault Thrown if there is an error reading the source string.
         * @return  A character, or 0 if there are no more characters.
         */
        public char nextClean() throws Fault {
            for (;;) {
                char c = this.next();
                if (c == 0 || c > ' ') {
                    return c;
                }
            }
        }
    
    
        /**
         * Return the characters up to the next close quote character.
         * Backslash processing is done. The formal JSON format does not
         * allow strings in single quotes, but an implementation is allowed to
         * accept them.
         * @param quote The quoting character, either
         *      <code>"</code>&nbsp;<small>(double quote)</small> or
         *      <code>'</code>&nbsp;<small>(single quote)</small>.
         * @return      A String.
         * @throws Fault Unterminated string.
         */
        public String nextString(char quote) throws Fault {
            char c;
            StringBuilder sb = new StringBuilder();
            for (;;) {
                c = this.next();
                switch (c) {
                case 0:
                case '\n':
                case '\r':
                    throw this.syntaxError("Unterminated string");
                case '\\':
                    c = this.next();
                    switch (c) {
                    case 'b':
                        sb.append('\b');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 'u':
                        try {
                            sb.append((char)Integer.parseInt(this.next(4), 16));
                        } catch (NumberFormatException e) {
                            throw this.syntaxError("Illegal escape.", e);
                        }
                        break;
                    case '"':
                    case '\'':
                    case '\\':
                    case '/':
                        sb.append(c);
                        break;
                    default:
                        throw this.syntaxError("Illegal escape.");
                    }
                    break;
                default:
                    if (c == quote) {
                        return sb.toString();
                    }
                    sb.append(c);
                }
            }
        }
    
    
        /**
         * Get the text up but not including the specified character or the
         * end of line, whichever comes first.
         * @param  delimiter A delimiter character.
         * @return   A string.
         * @throws Fault Thrown if there is an error while searching
         *  for the delimiter
         */
        public String nextTo(char delimiter) throws Fault {
            StringBuilder sb = new StringBuilder();
            for (;;) {
                char c = this.next();
                if (c == delimiter || c == 0 || c == '\n' || c == '\r') {
                    if (c != 0) {
                        this.back();
                    }
                    return sb.toString().trim();
                }
                sb.append(c);
            }
        }
    
    
        /**
         * Get the text up but not including one of the specified delimiter
         * characters or the end of line, whichever comes first.
         * @param delimiters A set of delimiter characters.
         * @return A string, trimmed.
         * @throws Fault Thrown if there is an error while searching
         *  for the delimiter
         */
        public String nextTo(String delimiters) throws Fault {
            char c;
            StringBuilder sb = new StringBuilder();
            for (;;) {
                c = this.next();
                if (delimiters.indexOf(c) >= 0 || c == 0 ||
                        c == '\n' || c == '\r') {
                    if (c != 0) {
                        this.back();
                    }
                    return sb.toString().trim();
                }
                sb.append(c);
            }
        }
    
    
        /**
         * Get the next value. The value can be a Boolean, Double, Integer,
         * VariantList, Variant, Long, or String, or the Variant.NULL object.
         * @throws Fault If syntax error.
         *
         * @return An object.
         */
        public Object nextValue() throws Fault {
            char c = this.nextClean();
            String string;
    
            switch (c) {
            case '"':
            case '\'':
                return this.nextString(c);
            case '{':
                this.back();
                return new Type.Variant(this);
            case '[':
                this.back();
                return new Type.VariantList(this);
            }
    
            /*
             * Handle unquoted text. This could be the values true, false, or
             * null, or it can be a number. An implementation (such as this one)
             * is allowed to also accept non-standard forms.
             *
             * Accumulate characters until we reach the end of the text or a
             * formatting character.
             */
    
            StringBuilder sb = new StringBuilder();
            while (c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
                sb.append(c);
                c = this.next();
            }
            this.back();
    
            string = sb.toString().trim();
            if ("".equals(string)) {
                throw this.syntaxError("Missing value");
            }
            return Type.Variant.stringToValue(string);
        }
    
    
        /**
         * Skip characters until the next character is the requested character.
         * If the requested character is not found, no characters are skipped.
         * @param to A character to skip to.
         * @return The requested character, or zero if the requested character
         * is not found.
         * @throws Fault Thrown if there is an error while searching
         *  for the to character
         */
        public char skipTo(char to) throws Fault {
            char c;
            try {
                long startIndex = this.index;
                long startCharacter = this.character;
                long startLine = this.line;
                this.reader.mark(1000000);
                do {
                    c = this.next();
                    if (c == 0) {
                        // in some readers, reset() may throw an exception if
                        // the remaining portion of the input is greater than
                        // the mark size (1,000,000 above).
                        this.reader.reset();
                        this.index = startIndex;
                        this.character = startCharacter;
                        this.line = startLine;
                        return 0;
                    }
                } while (c != to);
                this.reader.mark(1);
            } catch (IOException exception) {
                throw new Fault(exception);
            }
            this.back();
            return c;
        }
    
        /**
         * Make a Fault to signal a syntax error.
         *
         * @param message The error message.
         * @return  A Fault object, suitable for throwing
         */
        public Fault syntaxError(String message) {
            return new Fault(message + this.toString());
        }
    
        /**
         * Make a Fault to signal a syntax error.
         *
         * @param message The error message.
         * @param causedBy The throwable that caused the error.
         * @return  A Fault object, suitable for throwing
         */
        public Fault syntaxError(String message, Throwable causedBy) {
            return new Fault(message + this.toString(), causedBy);
        }
    
        /**
         * Make a printable string of this Compiler.
         *
         * @return " at {index} [character {character} line {line}]"
         */
        @Override
        public String toString() {
            return " at " + this.index + " [character " + this.character + " line " +
                    this.line + "]";
        }
    }

    public static class Plus {
        /**
         * Convert a web browser cookie specification to a Variant and back.
         * JSON and Cookies are both notations for name/value pairs.
         * @author JSON.org
         * @version 2015-12-09
         */
        public static class Cookie {

            /**
             * Produce a copy of a string in which the characters '+', '%', '=', ';'
             * and control characters are replaced with "%hh". This is a gentle form
             * of URL encoding, attempting to cause as little distortion to the
             * string as possible. The characters '=' and ';' are meta characters in
             * cookies. By convention, they are escaped using the URL-encoding. This is
             * only a convention, not a standard. Often, cookies are expected to have
             * encoded values. We encode '=' and ';' because we must. We encode '%' and
             * '+' because they are meta characters in URL encoding.
             * @param string The source string.
             * @return       The escaped result.
             */
            public static String escape(String string) {
                char            c;
                String          s = string.trim();
                int             length = s.length();
                StringBuilder   sb = new StringBuilder(length);
                for (int i = 0; i < length; i += 1) {
                    c = s.charAt(i);
                    if (c < ' ' || c == '+' || c == '%' || c == '=' || c == ';') {
                        sb.append('%');
                        sb.append(Character.forDigit((char)((c >>> 4) & 0x0f), 16));
                        sb.append(Character.forDigit((char)(c & 0x0f), 16));
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            }


            /**
             * Convert a cookie specification string into a Variant. The string
             * will contain a name value pair separated by '='. The name and the value
             * will be unescaped, possibly converting '+' and '%' sequences. The
             * cookie values may follow, separated by ';', also represented as
             * name=value (except the secure property, which does not have a value).
             * The name will be stored under the key "name", and the value will be
             * stored under the key "value". This method does not do checking or
             * validation of the parameters. It only converts the cookie string into
             * a Variant.
             * @param string The cookie specification string.
             * @return A Variant containing "name", "value", and possibly other
             *  members.
             * @throws Fault
             */
            public static Type.Variant toJSONValue(String string) throws Fault {
                String         name;
                Type.Variant jo = new Type.Variant();
                Object         value;
                Compiler x = new Compiler(string);
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
             * @param jo A Variant
             * @return A cookie specification string
             * @throws Fault
             */
            public static String toString(Type.Variant jo) throws Fault {
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
             * @param string A string that may contain
             *      <code>+</code>&nbsp;<small>(plus)</small> and
             *      <code>%</code><i>hh</i> sequences.
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
                        int d = JSON.Compiler.dehexchar(string.charAt(i + 1));
                        int e = JSON.Compiler.dehexchar(string.charAt(i + 2));
                        if (d >= 0 && e >= 0) {
                            c = (char)(d * 16 + e);
                            i += 2;
                        }
                    }
                    sb.append(c);
                }
                return sb.toString();
            }

          /**
           * Convert a web browser cookie list string to a Variant and back.
           * @author JSON.org
           * @version 2015-12-09
           */
          public static class Batch {

              /**
               * Convert a cookie list into a Variant. A cookie list is a sequence
               * of name/value pairs. The names are separated from the values by '='.
               * The pairs are separated by ';'. The names and the values
               * will be unescaped, possibly converting '+' and '%' sequences.
               *
               * To add a cookie to a cookie list,
               * cookielistJSONValue.put(cookieJSONValue.getString("name"),
               *     cookieJSONValue.getString("value"));
               * @param string  A cookie list string
               * @return A Variant
               * @throws Fault
               */
              public static Type.Variant toJSONValue(String string) throws Fault {
                  Type.Variant jo = new Type.Variant();
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
               * @param jo A Variant
               * @return A cookie list string
               * @throws Fault
               */
              public static String toString(Type.Variant jo) throws Fault {
                  boolean             b = false;
                  StringBuilder sb = new StringBuilder();
                  for (final Map.Entry<String,?> entry : jo.entrySet()) {
                    final String key = entry.getKey();
                    final Object value = entry.getValue();
                      if (!Type.Variant.NULL.equals(value)) {
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

        /**
         * Convert an HTTP header to a Variant and back.
         * @author JSON.org
         * @version 2015-12-09
         */
        public static class HTTP {

            /** Carriage return/line feed. */
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
             * In addition, the other parameters in the header will be captured, using
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
             * @param string An HTTP header string.
             * @return A Variant containing the elements and attributes
             * of the XML string.
             * @throws Fault
             */
            public static Type.Variant toJSONValue(String string) throws Fault {
                Type.Variant jo = new Type.Variant();
                Compiler x = new Compiler(string);
                String         token;

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
             * @param jo A Variant
             * @return An HTTP header string.
             * @throws Fault if the object does not contain enough
             *  information.
             */
            public static String toString(Type.Variant jo) throws Fault {
                StringBuilder       sb = new StringBuilder();
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
                for (final Map.Entry<String,?> entry : jo.entrySet()) {
                  final String key = entry.getKey();
                    if (!"HTTP-Version".equals(key)      && !"Status-Code".equals(key) &&
                            !"Reason-Phrase".equals(key) && !"Method".equals(key) &&
                            !"Request-URI".equals(key)   && !Type.Variant.NULL.equals(entry.getValue())) {
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
             * @author JSON.org
             * @version 2015-12-09
             */
            public static class Compiler extends JSON.Compiler {

                /**
                 * Construct an Compiler from a string.
                 * @param string A source string.
                 */
                public Compiler(String string) {
                    super(string);
                }


                /**
                 * Get the next token or string. This is used in parsing HTTP headers.
                 * @throws Fault
                 * @return A String.
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
                        for (;;) {
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
                    for (;;) {
                        if (c == 0 || Character.isWhitespace(c)) {
                            return sb.toString();
                        }
                        sb.append(c);
                        c = next();
                    }
                }
            }
        }

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
         * @author JSON.org
         * @version 2016-05-01
         */
        public static class CSV {

            /**
             * Get the next value. The value can be wrapped in quotes. The value can
             * be empty.
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
                    for (;;) {
                        c = x.next();
                        if (c == q) {
                            //Handle escaped double-quote
                            char nextC = x.next();
                            if(nextC != '\"') {
                                // if our quote was the end of the file, don't step
                                if(nextC > 0) {
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
             * Produce a VariantList of strings from a row of comma delimited values.
             * @param x A Compiler of the source text.
             * @return A VariantList of strings.
             * @throws Fault
             */
            public static Type.VariantList rowToJSONElements(Compiler x) throws Fault {
                Type.VariantList ja = new Type.VariantList();
                for (;;) {
                    String value = getValue(x);
                    char c = x.next();
                    if (value == null ||
                            (ja.length() == 0 && value.length() == 0 && c != ',')) {
                        return null;
                    }
                    ja.put(value);
                    for (;;) {
                        if (c == ',') {
                            break;
                        }
                        if (c != ' ') {
                            if (c == '\n' || c == '\r' || c == 0) {
                                return ja;
                            }
                            throw x.syntaxError("Bad character '" + c + "' (" +
                                    (int)c + ").");
                        }
                        c = x.next();
                    }
                }
            }

            /**
             * Produce a Variant from a row of comma delimited text, using a
             * parallel VariantList of strings to provides the names of the elements.
             * @param names A VariantList of names. This is commonly obtained from the
             *  first row of a comma delimited text file using the rowToJSONElements
             *  method.
             * @param x A Compiler of the source text.
             * @return A Variant combining the names and values.
             * @throws Fault
             */
            public static Type.Variant rowToJSONValue(Type.VariantList names, Compiler x)
                    throws Fault
            {
                Type.VariantList ja = rowToJSONElements(x);
                return ja != null ? ja.toJSONValue(names) :  null;
            }

            /**
             * Produce a comma delimited text row from a VariantList. Values containing
             * the comma character will be quoted. Troublesome characters may be
             * removed.
             * @param ja A VariantList of strings.
             * @return A string ending in NEWLINE.
             */
            public static String rowToString(Type.VariantList ja) {
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
             * @param string The comma delimited text.
             * @return A VariantList of VariantList.
             * @throws Fault
             */
            public static Type.VariantList toJSONElements(String string) throws Fault {
                return toJSONElements(new Compiler(string));
            }

            /**
             * Produce a VariantList of VariantList from a comma delimited text string,
             * using the first row as a source of names.
             * @param x The Compiler containing the comma delimited text.
             * @return A VariantList of VariantList.
             * @throws Fault
             */
            public static Type.VariantList toJSONElements(Compiler x) throws Fault {
                return toJSONElements(rowToJSONElements(x), x);
            }

            /**
             * Produce a VariantList of VariantList from a comma delimited text string
             * using a supplied VariantList as the source of element names.
             * @param names A VariantList of strings.
             * @param string The comma delimited text.
             * @return A VariantList of VariantList.
             * @throws Fault
             */
            public static Type.VariantList toJSONElements(Type.VariantList names, String string)
                    throws Fault
            {
                return toJSONElements(names, new Compiler(string));
            }

            /**
             * Produce a VariantList of VariantList from a comma delimited text string
             * using a supplied VariantList as the source of element names.
             * @param names A VariantList of strings.
             * @param x A Compiler of the source text.
             * @return A VariantList of VariantList.
             * @throws Fault
             */
            public static Type.VariantList toJSONElements(Type.VariantList names, Compiler x)
                    throws Fault
            {
                if (names == null || names.length() == 0) {
                    return null;
                }
                Type.VariantList ja = new Type.VariantList();
                for (;;) {
                    Type.Variant jo = rowToJSONValue(names, x);
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
             * @param ja A VariantList of VariantList.
             * @return A comma delimited text.
             * @throws Fault
             */
            public static String toString(Type.VariantList ja) throws Fault {
                Type.Variant jo = ja.optJSONValue(0);
                if (jo != null) {
                    Type.VariantList names = jo.names();
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
             * @param names A VariantList of strings.
             * @param ja A VariantList of VariantList.
             * @return A comma delimited text.
             * @throws Fault
             */
            public static String toString(Type.VariantList names, Type.VariantList ja)
                    throws Fault
            {
                if (names == null || names.length() == 0) {
                    return null;
                }
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < ja.length(); i += 1) {
                    Type.Variant jo = ja.optJSONValue(i);
                    if (jo != null) {
                        sb.append(rowToString(jo.toJSONElements(names)));
                    }
                }
                return sb.toString();
            }
        }

        /**
         * This provides static methods to convert an XML text into a Variant, and to
         * covert a Variant into an XML text.
         *
         * @author JSON.org
         * @version 2016-08-10
         */
        @SuppressWarnings("boxing")
        public static class XML {
            /** The Character '&amp;'. */
            public static final Character AMP = '&';

            /** The Character '''. */
            public static final Character APOS = '\'';

            /** The Character '!'. */
            public static final Character BANG = '!';

            /** The Character '='. */
            public static final Character EQ = '=';

            /** The Character '>'. */
            public static final Character GT = '>';

            /** The Character '&lt;'. */
            public static final Character LT = '<';

            /** The Character '?'. */
            public static final Character QUEST = '?';

            /** The Character '"'. */
            public static final Character QUOT = '"';

            /** The Character '/'. */
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
             *      "http://stackoverflow.com/a/21791059/6030888">http://stackoverflow.com/a/21791059/6030888</a>
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
             *
             * <pre>
             * &amp; <small>(ampersand)</small> is replaced by &amp;amp;
             * &lt; <small>(less than)</small> is replaced by &amp;lt;
             * &gt; <small>(greater than)</small> is replaced by &amp;gt;
             * &quot; <small>(double quote)</small> is replaced by &amp;quot;
             * &apos; <small>(single quote / apostrophe)</small> is replaced by &amp;apos;
             * </pre>
             *
             * @param string
             *            The string to be escaped.
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
                 * #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
                 *
                 * any Unicode character, excluding the surrogate blocks, FFFE, and FFFF.
                 */
                // isISOControl is true when (cp >= 0 && cp <= 0x1F) || (cp >= 0x7F && cp <= 0x9F)
                // all ISO control characters are out of range except tabs and new lines
                return (Character.isISOControl(cp)
                        && cp != 0x9
                        && cp != 0xA
                        && cp != 0xD
                    ) || !(
                        // valid the range of acceptable characters that aren't control
                        (cp >= 0x20 && cp <= 0xD7FF)
                        || (cp >= 0xE000 && cp <= 0xFFFD)
                        || (cp >= 0x10000 && cp <= 0x10FFFF)
                    )
                ;
            }

            /**
             * Removes XML escapes from the string.
             *
             * @param string
             *            string to remove escapes from
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
             * @param string
             *            A string.
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
             * @param x
             *            The Compiler containing the source string.
             * @param context
             *            The Variant that will include the new material.
             * @param name
             *            The tag name.
             * @return true if the close tag is processed.
             * @throws Fault
             */
            private static boolean parse(Compiler x, Type.Variant context, String name, boolean keepStrings)
                    throws Fault
            {
                char c;
                int i;
                Type.Variant jsonobject = null;
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
                    jsonobject = new Type.Variant();
                    for (;;) {
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
                                jsonobject.accumulate(string,
                                        keepStrings ? ((String)token) : stringToValue((String) token));
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
                            for (;;) {
                                token = x.nextContent();
                                if (token == null) {
                                    if (tagName != null) {
                                        throw x.syntaxError("Unclosed tag " + tagName);
                                    }
                                    return false;
                                } else if (token instanceof String) {
                                    string = (String) token;
                                    if (string.length() > 0) {
                                        jsonobject.accumulate("content",
                                                keepStrings ? string : stringToValue(string));
                                    }

                                } else if (token == LT) {
                                    // Nested element
                                    if (parse(x, jsonobject, tagName,keepStrings)) {
                                        if (jsonobject.length() == 0) {
                                            context.accumulate(tagName, "");
                                        } else if (jsonobject.length() == 1
                                                && jsonobject.opt("content") != null) {
                                            context.accumulate(tagName,
                                                    jsonobject.opt("content"));
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
             * This method is the same as {@link Type.Variant#stringToValue(String)}
             * except that this also tries to unescape String values.
             *
             * @param string String to convert
             * @return JSON value of this string or the string
             */
            public static Object stringToValue(String string) {
                return Type.Variant.stringToValue(string);
            }

            /**
             * Convert a well-formed (but not necessarily valid) XML string into a
             * Variant. Some information may be lost in this transformation because
             * JSON is a data format and XML is a document format. XML uses elements,
             * attributes, and content text, while JSON uses unordered collections of
             * name/value pairs and arrays of values. JSON does not does not like to
             * distinguish between elements and attributes. Sequences of similar
             * elements are represented as JSONElementss. Content text may be placed in a
             * "content" member. Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code>
             * are ignored.
             *
             * @param string
             *            The source string.
             * @return A Variant containing the structured data from the XML string.
             * @throws Fault Thrown if there is an errors while parsing the string
             */
            public static Type.Variant toJSONValue(String string) throws Fault {
                return toJSONValue(string, false);
            }


            /**
             * Convert a well-formed (but not necessarily valid) XML string into a
             * Variant. Some information may be lost in this transformation because
             * JSON is a data format and XML is a document format. XML uses elements,
             * attributes, and content text, while JSON uses unordered collections of
             * name/value pairs and arrays of values. JSON does not does not like to
             * distinguish between elements and attributes. Sequences of similar
             * elements are represented as JSONElementss. Content text may be placed in a
             * "content" member. Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code>
             * are ignored.
             *
             * All values are converted as strings, for 1, 01, 29.0 will not be coerced to
             * numbers but will instead be the exact value as seen in the XML document.
             *
             * @param string
             *            The source string.
             * @param keepStrings If true, then values will not be coerced into boolean
             *  or numeric values and will instead be left as strings
             * @return A Variant containing the structured data from the XML string.
             * @throws Fault Thrown if there is an errors while parsing the string
             */
            public static Type.Variant toJSONValue(String string, boolean keepStrings) throws Fault {
                Type.Variant jo = new Type.Variant();
                Compiler x = new Compiler(string);
                while (x.more() && x.skipPast("<")) {
                    parse(x, jo, null, keepStrings);
                }
                return jo;
            }
            /**
             * Convert a Variant into a well-formed, element-normal XML string.
             *
             * @param object
             *            A Variant.
             * @return A string.
             * @throws Fault Thrown if there is an error parsing the string
             */
            public static String toString(Object object) throws Fault {
                return toString(object, null);
            }

            /**
             * Convert a Variant into a well-formed, element-normal XML string.
             *
             * @param object
             *            A Variant.
             * @param tagName
             *            The optional name of the enclosing tag.
             * @return A string.
             * @throws Fault Thrown if there is an error parsing the string
             */
            public static String toString(final Object object, final String tagName)
                    throws Fault
            {
                StringBuilder sb = new StringBuilder();
                Type.VariantList ja;
                Type.Variant jo;
                String string;

                if (object instanceof Type.Variant) {

                    // Emit <tagName>
                    if (tagName != null) {
                        sb.append('<');
                        sb.append(tagName);
                        sb.append('>');
                    }

                    // Loop thru the keys.
                    jo = (Type.Variant) object;
                    for (final Map.Entry<String, ?> entry : jo.entrySet()) {
                      final String key = entry.getKey();
                        Object value = entry.getValue();
                        if (value == null) {
                            value = "";
                        } else if (value.getClass().isArray()) {
                            value = new Type.VariantList(value);
                        }

                        // Emit content in body
                        if ("content".equals(key)) {
                            if (value instanceof Type.VariantList) {
                                ja = (Type.VariantList) value;
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

                        } else if (value instanceof Type.VariantList) {
                            ja = (Type.VariantList) value;
                            for (Object val : ja) {
                                if (val instanceof Type.VariantList) {
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

                if (object != null && (object instanceof Type.VariantList ||  object.getClass().isArray())) {
                    if(object.getClass().isArray()) {
                        ja = new Type.VariantList(object);
                    } else {
                        ja = (Type.VariantList) object;
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
                        : (string.length() == 0) ? "<" + tagName + "/>" : "<" + tagName
                                + ">" + string + "</" + tagName + ">";

            }

          /**
           * The Compiler extends the Compiler to provide additional methods
           * for the parsing of XML texts.
           * @author JSON.org
           * @version 2015-12-09
           */
          public static class Compiler extends JSON.Compiler {


             /** The table of entity values. It initially contains Character values for
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
               * @param s A source string.
               */
              public Compiler(String s) {
                  super(s);
              }

              /**
               * Get the text in the CDATA block.
               * @return The string up to the <code>]]&gt;</code>.
               * @throws Fault If the <code>]]&gt;</code> is not found.
               */
              public String nextCDATA() throws Fault {
                  char         c;
                  int          i;
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
               * @return  A string, or a '<' Character, or null if there is no more
               * source text.
               * @throws Fault
               */
              public Object nextContent() throws Fault {
                  char         c;
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
                  for (;;) {
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
               * Return the next entity. These entities are translated to Characters:
               *     <code>&amp;  &apos;  &gt;  &lt;  &quot;</code>.
               * @param ampersand An ampersand character.
               * @return  A Character or an entity String if the entity is not recognized.
               * @throws Fault If missing ';' in XML entity.
               */
              public Object nextEntity(char ampersand) throws Fault {
                  StringBuilder sb = new StringBuilder();
                  for (;;) {
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
               * Unescapes an XML entity encoding;
               * @param e entity (only the actual entity value, not the preceding & or ending ;
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
                      return new String(new int[] {cp},0,1);
                  }
                  Character knownEntity = entity.get(e);
                  if(knownEntity==null) {
                      // we don't know the entity so keep it encoded
                      return '&' + e + ';';
                  }
                  return knownEntity.toString();
              }


              /**
               * Returns the next XML meta token. This is used for skipping over <!...>
               * and <?...?> structures.
               * @return Syntax characters (<code>< > / = ! ?</code>) are returned as
               *  Character, and strings and names are returned as Boolean. We don't care
               *  what the values actually are.
               * @throws Fault If a string is not properly closed or if the XML
               *  is badly structured.
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
                      for (;;) {
                          c = next();
                          if (c == 0) {
                              throw syntaxError("Unterminated string");
                          }
                          if (c == q) {
                              return Boolean.TRUE;
                          }
                      }
                  default:
                      for (;;) {
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
                      for (;;) {
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
                      for (;;) {
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
               * If it is not found, we are left at the end of the source with a result of false.
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
                   * First fill the circle buffer with as many characters as are in the
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

                  for (;;) {
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

                      /* Get the next character. If there isn't one, then defeat is ours. */

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
                 * @param x       The Compiler containing the source string.
                 * @param arrayForm true if array form, false if object form.
                 * @param ja      The VariantList that is containing the current tag or null
                 *     if we are at the outermost level.
                 * @param keepStrings	Don't type-convert text nodes and attribute values
                 * @return A VariantList if the value is the outermost tag, otherwise null.
                 * @throws Fault
                 */
                private static Object parse(
                    Compiler x,
                    boolean    arrayForm,
                    Type.VariantList ja,
                    boolean keepStrings
                ) throws Fault
                {
                    String     attribute;
                    char       c;
                    String     closeTag = null;
                    int        i;
                    Type.VariantList newja = null;
                    Type.Variant newjo = null;
                    Object     token;
                    String     tagName = null;

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
                                tagName = (String)token;
                                newja = new Type.VariantList();
                                newjo = new Type.Variant();
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
                                for (;;) {
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

                                    attribute = (String)token;
                                    if (!arrayForm && ("tagName".equals(attribute) || "childNode".equals(attribute))) {
                                        throw x.syntaxError("Reserved attribute.");
                                    }
                                    token = x.nextToken();
                                    if (token == EQ) {
                                        token = x.nextToken();
                                        if (!(token instanceof String)) {
                                            throw x.syntaxError("Missing value");
                                        }
                                        newjo.accumulate(attribute, keepStrings ? ((String)token) : stringToValue((String)token));
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
                                    closeTag = (String)parse(x, arrayForm, newja, keepStrings);
                                    if (closeTag != null) {
                                        if (!closeTag.equals(tagName)) {
                                            throw x.syntaxError("Mismatched '" + tagName +
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
                                    ? keepStrings ? unescape((String)token) : stringToValue((String)token)
                                    : token);
                            }
                        }
                    }
                }


                /**
                 * Convert a well-formed (but not necessarily valid) XML string into a
                 * VariantList using the JsonML transform. Each XML tag is represented as
                 * a VariantList in which the first element is the tag name. If the tag has
                 * attributes, then the second element will be Variant containing the
                 * name/value pairs. If the tag contains children, then strings and
                 * JSONElementss will represent the child tags.
                 * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
                 * @param string The source string.
                 * @return A VariantList containing the structured data from the XML string.
                 * @throws Fault Thrown on error converting to a VariantList
                 */
                public static Type.VariantList toJSONElements(String string) throws Fault {
                    return (Type.VariantList)parse(new Compiler(string), true, null, false);
                }


                /**
                 * Convert a well-formed (but not necessarily valid) XML string into a
                 * VariantList using the JsonML transform. Each XML tag is represented as
                 * a VariantList in which the first element is the tag name. If the tag has
                 * attributes, then the second element will be Variant containing the
                 * name/value pairs. If the tag contains children, then strings and
                 * JSONElementss will represent the child tags.
                 * As opposed to toJSONElements this method does not attempt to convert
                 * any text node or attribute value to any type
                 * but just leaves it as a string.
                 * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
                 * @param string The source string.
                 * @param keepStrings If true, then values will not be coerced into boolean
                 *  or numeric values and will instead be left as strings
                 * @return A VariantList containing the structured data from the XML string.
                 * @throws Fault Thrown on error converting to a VariantList
                 */
                public static Type.VariantList toJSONElements(String string, boolean keepStrings) throws Fault {
                    return (Type.VariantList)parse(new Compiler(string), true, null, keepStrings);
                }


                /**
                 * Convert a well-formed (but not necessarily valid) XML string into a
                 * VariantList using the JsonML transform. Each XML tag is represented as
                 * a VariantList in which the first element is the tag name. If the tag has
                 * attributes, then the second element will be Variant containing the
                 * name/value pairs. If the tag contains children, then strings and
                 * JSONElementss will represent the child content and tags.
                 * As opposed to toJSONElements this method does not attempt to convert
                 * any text node or attribute value to any type
                 * but just leaves it as a string.
                 * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
                 * @param x An Compiler.
                 * @param keepStrings If true, then values will not be coerced into boolean
                 *  or numeric values and will instead be left as strings
                 * @return A VariantList containing the structured data from the XML string.
                 * @throws Fault Thrown on error converting to a VariantList
                 */
                public static Type.VariantList toJSONElements(Compiler x, boolean keepStrings) throws Fault {
                    return (Type.VariantList)parse(x, true, null, keepStrings);
                }


                /**
                 * Convert a well-formed (but not necessarily valid) XML string into a
                 * VariantList using the JsonML transform. Each XML tag is represented as
                 * a VariantList in which the first element is the tag name. If the tag has
                 * attributes, then the second element will be Variant containing the
                 * name/value pairs. If the tag contains children, then strings and
                 * JSONElementss will represent the child content and tags.
                 * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
                 * @param x An Compiler.
                 * @return A VariantList containing the structured data from the XML string.
                 * @throws Fault Thrown on error converting to a VariantList
                 */
                public static Type.VariantList toJSONElements(Compiler x) throws Fault {
                    return (Type.VariantList)parse(x, true, null, false);
                }


                /**
                 * Convert a well-formed (but not necessarily valid) XML string into a
                 * Variant using the JsonML transform. Each XML tag is represented as
                 * a Variant with a "tagName" property. If the tag has attributes, then
                 * the attributes will be in the Variant as properties. If the tag
                 * contains children, the object will have a "childNodes" property which
                 * will be an array of strings and JsonML VariantList.

                 * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
                 * @param string The XML source text.
                 * @return A Variant containing the structured data from the XML string.
                 * @throws Fault Thrown on error converting to a Variant
                 */
                public static Type.Variant toJSONValue(String string) throws Fault {
                    return (Type.Variant)parse(new Compiler(string), false, null, false);
                }


                /**
                 * Convert a well-formed (but not necessarily valid) XML string into a
                 * Variant using the JsonML transform. Each XML tag is represented as
                 * a Variant with a "tagName" property. If the tag has attributes, then
                 * the attributes will be in the Variant as properties. If the tag
                 * contains children, the object will have a "childNodes" property which
                 * will be an array of strings and JsonML VariantList.

                 * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
                 * @param string The XML source text.
                 * @param keepStrings If true, then values will not be coerced into boolean
                 *  or numeric values and will instead be left as strings
                 * @return A Variant containing the structured data from the XML string.
                 * @throws Fault Thrown on error converting to a Variant
                 */
                public static Type.Variant toJSONValue(String string, boolean keepStrings) throws Fault {
                    return (Type.Variant)parse(new Compiler(string), false, null, keepStrings);
                }


                /**
                 * Convert a well-formed (but not necessarily valid) XML string into a
                 * Variant using the JsonML transform. Each XML tag is represented as
                 * a Variant with a "tagName" property. If the tag has attributes, then
                 * the attributes will be in the Variant as properties. If the tag
                 * contains children, the object will have a "childNodes" property which
                 * will be an array of strings and JsonML VariantList.

                 * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
                 * @param x An Compiler of the XML source text.
                 * @return A Variant containing the structured data from the XML string.
                 * @throws Fault Thrown on error converting to a Variant
                 */
                public static Type.Variant toJSONValue(Compiler x) throws Fault {
                       return (Type.Variant)parse(x, false, null, false);
                }


                /**
                 * Convert a well-formed (but not necessarily valid) XML string into a
                 * Variant using the JsonML transform. Each XML tag is represented as
                 * a Variant with a "tagName" property. If the tag has attributes, then
                 * the attributes will be in the Variant as properties. If the tag
                 * contains children, the object will have a "childNodes" property which
                 * will be an array of strings and JsonML VariantList.

                 * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
                 * @param x An Compiler of the XML source text.
                 * @param keepStrings If true, then values will not be coerced into boolean
                 *  or numeric values and will instead be left as strings
                 * @return A Variant containing the structured data from the XML string.
                 * @throws Fault Thrown on error converting to a Variant
                 */
                public static Type.Variant toJSONValue(Compiler x, boolean keepStrings) throws Fault {
                       return (Type.Variant)parse(x, false, null, keepStrings);
                }


                /**
                 * Reverse the Kit transformation, making an XML text from a VariantList.
                 * @param ja A VariantList.
                 * @return An XML string.
                 * @throws Fault Thrown on error converting to a string
                 */
                public static String toString(Type.VariantList ja) throws Fault {
                    int                 i;
                    Type.Variant jo;
                    int                 length;
                    Object              object;
                    StringBuilder        sb = new StringBuilder();
                    String              tagName;

            // Emit <tagName

                    tagName = ja.getString(0);
                    noSpace(tagName);
                    tagName = escape(tagName);
                    sb.append('<');
                    sb.append(tagName);

                    object = ja.opt(1);
                    if (object instanceof Type.Variant) {
                        i = 2;
                        jo = (Type.Variant)object;

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
                                } else if (object instanceof Type.Variant) {
                                    sb.append(toString((Type.Variant)object));
                                } else if (object instanceof Type.VariantList) {
                                    sb.append(toString((Type.VariantList)object));
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
                 * @param jo A Variant.
                 * @return An XML string.
                 * @throws Fault Thrown on error converting to a string
                 */
                public static String toString(Type.Variant jo) throws Fault {
                    StringBuilder sb = new StringBuilder();
                    int                 i;
                    Type.VariantList ja;
                    int                 length;
                    Object              object;
                    String              tagName;
                    Object              value;

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
                                } else if (object instanceof Type.Variant) {
                                    sb.append(toString((Type.Variant)object));
                                } else if (object instanceof Type.VariantList) {
                                    sb.append(toString((Type.VariantList)object));
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
    }
}
