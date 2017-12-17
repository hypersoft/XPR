package XPR.JSON.Type;

import XPR.Fault;
import XPR.JSON.Compiler;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * A VariantList is an ordered sequence of values. Its external text form
 * is a
 * string wrapped in square brackets with commas separating the values. The
 * internal form is an object having <code>getKey</code> and
 * <code>opt</code>
 * methods for accessing the values by index, and <code>put</code>
 * methods for
 * adding or replacing values. The values can be any of these types:
 * <code>Boolean</code>, <code>VariantList</code>, <code>Variant</code>,
 * <code>Number</code>, <code>String</code>, or the
 * <code>Variant.NULL object</code>.
 * <p>
 * The constructor can convert a JSON text into a Java object. The
 * <code>toString</code> method converts to JSON text.
 * <p>
 * A <code>getKey</code> method returns a value if one can be found, and
 * throws an
 * exception if one cannot be found. An <code>opt</code> method returns a
 * default value instead of throwing an exception, and so is useful for
 * obtaining optional values.
 * <p>
 * The generic <code>getKey()</code> and <code>opt()</code> methods
 * return an
 * object which you can cast or query for type. There are also typed
 * <code>getKey</code> and <code>opt</code> methods that do type checking
 * and type
 * coercion for you.
 * <p>
 * The texts produced by the <code>toString</code> methods strictly
 * conform to
 * JSON syntax rules. The constructors are more forgiving in the texts
 * they will
 * accept:
 * <ul>
 * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just
 * before the closing bracket.</li>
 * <li>The <code>null</code> value will be inserted when there is <code>,
 * </code>
 * &nbsp;<small>(comma)</small> elision.</li>
 * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single
 * quote)</small>.</li>
 * <li>Strings do not need to be quoted at all if they do not begin with
 * a quote
 * or single quote, and if they do not contain leading or trailing
 * spaces, and
 * if they do not contain any of these characters:
 * <code>{ } [ ] / \ : , #</code> and if they do not look like numbers and
 * if they are not the reserved words <code>true</code>,
 * <code>false</code>, or
 * <code>null</code>.</li>
 * </ul>
 *
 * @author JSON.org
 * @version 2016-08/15
 */
public class VariantList implements Iterable<Object> {

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
   * @param x A Compiler
   * @throws Fault If there is a syntax error.
   */
  public VariantList(Compiler x) throws Fault {
    this();
    if (x.nextClean() != '[') {
      throw x.syntaxError("A VariantList text must start with '['");
    }
    if (x.nextClean() != ']') {
      x.back();
      for (; ; ) {
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
   * @param source A string that begins with <code>[</code>&nbsp;<small>(left
   *               bracket)</small> and ends with <code>]</code>
   *               &nbsp;<small>(right bracket)</small>.
   * @throws Fault If there is a syntax error.
   */
  public VariantList(String source) throws Fault {
    this(new Compiler(source));
  }

  /**
   * Construct a VariantList from a Collection.
   *
   * @param collection A Collection.
   */
  public VariantList(Collection<?> collection) {
    if (collection == null) {
      this.myArrayList = new ArrayList<Object>();
    } else {
      this.myArrayList = new ArrayList<Object>(collection.size());
      for (Object o : collection) {
        this.myArrayList.add(Variant.wrap(o));
      }
    }
  }

  /**
   * Construct a VariantList from an array
   *
   * @throws Fault If not an array.
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
        "VariantList initial value should be a string or " +
          "collection or array.");
    }
  }

  @Override
  public Iterator<Object> iterator() {
    return this.myArrayList.iterator();
  }

  /**
   * Get the object value associated with an index.
   *
   * @param index The index must be between 0 and length() - 1.
   * @return An object value.
   * @throws Fault If there is no value for the index.
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
   * @param index The index must be between 0 and length() - 1.
   * @return The truth.
   * @throws Fault If there is no value for the index or if the value is not
   *               convertible to boolean.
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
   * @param index The index must be between 0 and length() - 1.
   * @return The value.
   * @throws Fault If the key is not found or if the value cannot be converted
   *               to a number.
   */
  public double getDouble(int index) throws Fault {
    Object object = this.get(index);
    try {
      return object instanceof Number ? ((Number) object)
        .doubleValue()
        : Double.parseDouble((String) object);
    } catch (Exception e) {
      throw new Fault("VariantList[" + index + "] is not a number.", e);
    }
  }

  /**
   * Get the float value associated with a key.
   *
   * @param index The index must be between 0 and length() - 1.
   * @return The numeric value.
   * @throws Fault if the key is not found or if the value is not a Number
   *               object and cannot be converted to a number.
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
   * @param index The index must be between 0 and length() - 1.
   * @return The numeric value.
   * @throws Fault if the key is not found or if the value is not a Number
   *               object and cannot be converted to a number.
   */
  public Number getNumber(int index) throws Fault {
    Object object = this.get(index);
    try {
      if (object instanceof Number) {
        return (Number) object;
      }
      return Variant.stringToNumber(object.toString());
    } catch (Exception e) {
      throw new Fault("VariantList[" + index + "] is not a number.", e);
    }
  }

  /**
   * Get the enum value associated with an index.
   *
   * @param clazz The type of enum to retrieve.
   * @param index The index must be between 0 and length() - 1.
   * @return The enum value at the index location
   * @throws Fault if the key is not found or if the value cannot be converted
   *               to an enum.
   */
  public <E extends Enum<E>> E getEnum(Class<E> clazz,
    int index) throws Fault
  {
    E val = optEnum(clazz, index);
    if (val == null) {
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
   * @param index The index must be between 0 and length() - 1.
   * @return The value.
   * @throws Fault If the key is not found or if the value cannot be
   *               converted
   *               to a BigDecimal.
   */
  public BigDecimal getBigDecimal(int index) throws Fault {
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
   * @param index The index must be between 0 and length() - 1.
   * @return The value.
   * @throws Fault If the key is not found or if the value cannot be
   *               converted
   *               to a BigInteger.
   */
  public BigInteger getBigInteger(int index) throws Fault {
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
   * @param index The index must be between 0 and length() - 1.
   * @return The value.
   * @throws Fault If the key is not found or if the value is not a
   *               number.
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
   * @param index The index must be between 0 and length() - 1.
   * @return A VariantList value.
   * @throws Fault If there is no value for the index. or if the value is
   *               not a
   *               VariantList
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
   * @param index subscript
   * @return A Variant value.
   * @throws Fault If there is no value for the index or if the value is not a
   *               Variant
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
   * @param index The index must be between 0 and length() - 1.
   * @return The value.
   * @throws Fault If the key is not found or if the value cannot be converted
   *               to a number.
   */
  public long getLong(int index) throws Fault {
    Object object = this.get(index);
    try {
      return object instanceof Number ? ((Number) object).longValue()
        : Long.parseLong((String) object);
    } catch (Exception e) {
      throw new Fault("VariantList[" + index + "] is not a number" +
        ".", e);
    }
  }

  /**
   * Get the string associated with an index.
   *
   * @param index The index must be between 0 and length() - 1.
   * @return A string value.
   * @throws Fault If there is no string value for the index.
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
   * @param index The index must be between 0 and length() - 1.
   * @return true if the value at the index is null, or if there is no value.
   */
  public boolean isNull(int index) {
    return Variant.NULL.equals(this.opt(index));
  }

  /**
   * Make a string from the contents of this VariantList. The
   * <code>separator</code> string is inserted between each element.
   * Warning:
   * This method assumes that the data structure is acyclical.
   *
   * @param separator A string that will be inserted between the elements.
   * @return a string.
   * @throws Fault If the array contains an invalid number.
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
   * @param index The index must be between 0 and length() - 1. If not,
   *              null is returned.
   * @return An object value, or null if there is no object at that index.
   */
  public Object opt(int index) {
    return (index < 0 || index >= this.length()) ? null : this
      .myArrayList
      .get(index);
  }

  /**
   * Get the optional boolean value associated with an index. It returns false
   * if there is no value at that index, or if the value is not Boolean
   * .TRUE
   * or the String "true".
   *
   * @param index The index must be between 0 and length() - 1.
   * @return The truth.
   */
  public boolean optBoolean(int index) {
    return this.optBoolean(index, false);
  }

  /**
   * Get the optional boolean value associated with an index. It returns the
   * defaultValue if there is no value at that index or if it is not a
   * Boolean
   * or the String "true" or "false" (case insensitive).
   *
   * @param index        The index must be between 0 and length() - 1.
   * @param defaultValue A boolean default.
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
   * @param index The index must be between 0 and length() - 1.
   * @return The value.
   */
  public double optDouble(int index) {
    return this.optDouble(index, Double.NaN);
  }

  /**
   * Get the optional double value associated with an index. The
   * defaultValue
   * is returned if there is no value for the index, or if the value is
   * not a
   * number and cannot be converted to a number.
   *
   * @param index        subscript
   * @param defaultValue The default value.
   * @return The value.
   */
  public double optDouble(int index, double defaultValue) {
    Object val = this.opt(index);
    if (Variant.NULL.equals(val)) {
      return defaultValue;
    }
    if (val instanceof Number) {
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
   * @param index The index must be between 0 and length() - 1.
   * @return The value.
   */
  public float optFloat(int index) {
    return this.optFloat(index, Float.NaN);
  }

  /**
   * Get the optional float value associated with an index. The
   * defaultValue
   * is returned if there is no value for the index, or if the value is not a
   * number and cannot be converted to a number.
   *
   * @param index        subscript
   * @param defaultValue The default value.
   * @return The value.
   */
  public float optFloat(int index, float defaultValue) {
    Object val = this.opt(index);
    if (Variant.NULL.equals(val)) {
      return defaultValue;
    }
    if (val instanceof Number) {
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
   * @param index The index must be between 0 and length() - 1.
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
   * @param index        The index must be between 0 and length() - 1.
   * @param defaultValue The default value.
   * @return The value.
   */
  public int optInt(int index, int defaultValue) {
    Object val = this.opt(index);
    if (Variant.NULL.equals(val)) {
      return defaultValue;
    }
    if (val instanceof Number) {
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
   * @param clazz The type of enum to retrieve.
   * @param index The index must be between 0 and length() - 1.
   * @return The enum value at the index location or null if not found
   */
  public <E extends Enum<E>> E optEnum(Class<E> clazz, int index) {
    return this.optEnum(clazz, index, null);
  }

  /**
   * Get the enum value associated with a key.
   *
   * @param clazz        The type of enum to retrieve.
   * @param index        The index must be between 0 and length() - 1.
   * @param defaultValue The default in case the value is not found
   * @return The enum value at the index location or defaultValue if
   * the value is not found or cannot be assigned to clazz
   */
  public <E extends Enum<E>> E optEnum(Class<E> clazz, int index,
    E defaultValue)
  {
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
   * @param index        The index must be between 0 and length() - 1.
   * @param defaultValue The default value.
   * @return The value.
   */
  public BigInteger optBigInteger(int index, BigInteger defaultValue) {
    Object val = this.opt(index);
    if (Variant.NULL.equals(val)) {
      return defaultValue;
    }
    if (val instanceof BigInteger) {
      return (BigInteger) val;
    }
    if (val instanceof BigDecimal) {
      return ((BigDecimal) val).toBigInteger();
    }
    if (val instanceof Double || val instanceof Float) {
      return new BigDecimal(((Number) val).doubleValue()).toBigInteger();
    }
    if (val instanceof Long || val instanceof Integer
      || val instanceof Short || val instanceof Byte) {
      return BigInteger.valueOf(((Number) val).longValue());
    }
    try {
      final String valStr = val.toString();
      if (Variant.isDecimalNotation(valStr)) {
        return new BigDecimal(valStr).toBigInteger();
      }
      return new BigInteger(valStr);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * Get the optional BigDecimal value associated with an index. The
   * defaultValue is returned if there is no value for the index, or if
   * the
   * value is not a number and cannot be converted to a number.
   *
   * @param index        The index must be between 0 and length() - 1.
   * @param defaultValue The default value.
   * @return The value.
   */
  public BigDecimal optBigDecimal(int index, BigDecimal defaultValue) {
    Object val = this.opt(index);
    if (Variant.NULL.equals(val)) {
      return defaultValue;
    }
    if (val instanceof BigDecimal) {
      return (BigDecimal) val;
    }
    if (val instanceof BigInteger) {
      return new BigDecimal((BigInteger) val);
    }
    if (val instanceof Double || val instanceof Float) {
      return new BigDecimal(((Number) val).doubleValue());
    }
    if (val instanceof Long || val instanceof Integer
      || val instanceof Short || val instanceof Byte) {
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
   * @param index subscript
   * @return A VariantList value, or null if the index has no value, or
   * if the
   * value is not a VariantList.
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
   * @param index The index must be between 0 and length() - 1.
   * @return A Variant value.
   */
  public Variant optJSONValue(int index) {
    Object o = this.opt(index);
    return o instanceof Variant ? (Variant) o : null;
  }

  /**
   * Get the optional long value associated with an index. Zero is
   * returned if
   * there is no value for the index, or if the value is not a number and
   * cannot be converted to a number.
   *
   * @param index The index must be between 0 and length() - 1.
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
   * @param index        The index must be between 0 and length() - 1.
   * @param defaultValue The default value.
   * @return The value.
   */
  public long optLong(int index, long defaultValue) {
    Object val = this.opt(index);
    if (Variant.NULL.equals(val)) {
      return defaultValue;
    }
    if (val instanceof Number) {
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
   * Get an optional {@link Number} value associated with a key, or
   * <code>null</code>
   * if there is no such key or if the value is not a number. If the value
   * is a string,
   * an attempt will be made to evaluate it as a number
   * ({@link BigDecimal}). This method
   * would be used in cases where type coercion of the number value is
   * unwanted.
   *
   * @param index The index must be between 0 and length() - 1.
   * @return An object which is the value.
   */
  public Number optNumber(int index) {
    return this.optNumber(index, null);
  }

  /**
   * Get an optional {@link Number} value associated with a key, or the
   * default if there
   * is no such key or if the value is not a number. If the value is a
   * string,
   * an attempt will be made to evaluate it as a number
   * ({@link BigDecimal}). This method
   * would be used in cases where type coercion of the number value is
   * unwanted.
   *
   * @param index        The index must be between 0 and length() - 1.
   * @param defaultValue The default.
   * @return An object which is the value.
   */
  public Number optNumber(int index, Number defaultValue) {
    Object val = this.opt(index);
    if (Variant.NULL.equals(val)) {
      return defaultValue;
    }
    if (val instanceof Number) {
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
   * @param index The index must be between 0 and length() - 1.
   * @return A String value.
   */
  public String optString(int index) {
    return this.optString(index, "");
  }

  /**
   * Get the optional string associated with an index. The defaultValue is
   * returned if the key is not found.
   *
   * @param index        The index must be between 0 and length() - 1.
   * @param defaultValue The default value.
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
   * @param value A boolean value.
   * @return this.
   */
  public VariantList put(boolean value) {
    this.put(value ? Boolean.TRUE : Boolean.FALSE);
    return this;
  }

  /**
   * Put a value in the VariantList, where the value will be a
   * VariantList which
   * is produced from a Collection.
   *
   * @param value A Collection value.
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
      for (; ; ) {
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
   * @param value A double value.
   * @return this.
   * @throws Fault if the value is not finite.
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
   * @param value An int value.
   * @return this.
   */
  public VariantList put(int value) {
    this.put(new Integer(value));
    return this;
  }

  /**
   * Append an long value. This increases the array's length by one.
   *
   * @param value A long value.
   * @return this.
   */
  public VariantList put(long value) {
    this.put(new Long(value));
    return this;
  }

  /**
   * Put a value in the VariantList, where the value will be a Variant
   * which
   * is produced from a Map.
   *
   * @param value A Map value.
   * @return this.
   */
  public VariantList put(Map<?, ?> value) {
    this.put(new Variant(value));
    return this;
  }

  /**
   * Append an object value. This increases the array's length by one.
   *
   * @param value An object value. The value should be a Boolean, Double,
   *              Integer, VariantList, Variant, Long, or String, or the
   *              Variant.NULL object.
   * @return this.
   */
  public VariantList put(Object value) {
    this.myArrayList.add(value);
    return this;
  }

  /**
   * Put or replace a boolean value in the VariantList. If the index is
   * greater
   * than the length of the VariantList, then null elements will be added as
   * necessary to pad it out.
   *
   * @param index The subscript.
   * @param value A boolean value.
   * @return this.
   * @throws Fault If the index is negative.
   */
  public VariantList put(int index, boolean value) throws Fault {
    this.put(index, value ? Boolean.TRUE : Boolean.FALSE);
    return this;
  }

  /**
   * Put a value in the VariantList, where the value will be a VariantList
   * which
   * is produced from a Collection.
   *
   * @param index The subscript.
   * @param value A Collection value.
   * @return this.
   * @throws Fault If the index is negative or if the value is not finite.
   */
  public VariantList put(int index, Collection<?> value) throws Fault {
    this.put(index, new VariantList(value));
    return this;
  }

  /**
   * Put or replace a double value. If the index is greater than the length of
   * the VariantList, then null elements will be added as necessary to
   * pad it
   * out.
   *
   * @param index The subscript.
   * @param value A double value.
   * @return this.
   * @throws Fault If the index is negative or if the value is not finite.
   */
  public VariantList put(int index, double value) throws Fault {
    this.put(index, new Double(value));
    return this;
  }

  /**
   * Put or replace an int value. If the index is greater than the
   * length of
   * the VariantList, then null elements will be added as necessary to pad it
   * out.
   *
   * @param index The subscript.
   * @param value An int value.
   * @return this.
   * @throws Fault If the index is negative.
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
   * @param index The subscript.
   * @param value A long value.
   * @return this.
   * @throws Fault If the index is negative.
   */
  public VariantList put(int index, long value) throws Fault {
    this.put(index, new Long(value));
    return this;
  }

  /**
   * Put a value in the VariantList, where the value will be a Variant
   * that
   * is produced from a Map.
   *
   * @param index The subscript.
   * @param value The Map value.
   * @return this.
   * @throws Fault If the index is negative or if the the value is an
   *               invalid
   *               number.
   */
  public VariantList put(int index, Map<?, ?> value) throws Fault {
    this.put(index, new Variant(value));
    return this;
  }

  /**
   * Put or replace an object value in the VariantList. If the index is
   * greater
   * than the length of the VariantList, then null elements will be
   * added as
   * necessary to pad it out.
   *
   * @param index The subscript.
   * @param value The value to put into the array. The value should be a
   *              Boolean, Double, Integer, VariantList, Variant, Long, or
   *              String, or the Variant.NULL object.
   * @return this.
   * @throws Fault If the index is negative or if the the value is an
   *               invalid
   *               number.
   */
  public VariantList put(int index, Object value) throws Fault {
    Variant.testValidity(value);
    if (index < 0) {
      throw new Fault("VariantList[" + index + "] not found.");
    }
    if (index < this.length()) {
      this.myArrayList.set(index, value);
    } else if (index == this.length()) {
      // simple add
      this.put(value);
    } else {
      // if we are inserting past the length, we want to grow the array all
      // at once
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
   * Queries and returns a value from this object using {@code
   * jsonPointer}, or
   * returns null if the query fails due to a missing key.
   *
   * @param jsonPointer the string representation of the JSON pointer
   * @return the queried value or {@code null}
   * @throws IllegalArgumentException if {@code jsonPointer} has invalid
   *                                  syntax
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
    } catch (Fault e) {
      return null;
    }
  }

  /**
   * Remove an index and close the hole.
   *
   * @param index The index of the element to be removed.
   * @return The value that was associated with the index, or null if there
   * was no value.
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
    if (len != ((VariantList) other).length()) {
      return false;
    }
    for (int i = 0; i < len; i += 1) {
      Object valueThis = this.myArrayList.get(i);
      Object valueOther = ((VariantList) other).myArrayList.get(i);
      if (valueThis == valueOther) {
        return true;
      }
      if (valueThis == null) {
        return false;
      }
      if (valueThis instanceof Variant) {
        if (!((Variant) valueThis).similar(valueOther)) {
          return false;
        }
      } else if (valueThis instanceof VariantList) {
        if (!((VariantList) valueThis).similar(valueOther)) {
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
   * @param names A VariantList containing a list of key strings. These
   *              will be
   *              paired with the values.
   * @return A Variant, or null if there are no names or if this
   * VariantList
   * has no values.
   * @throws Fault If any of the names are null.
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
   * correct JSON text then null will be returned instead. This could
   * occur if
   * the array contains an invalid number.
   * <p><b>
   * Warning: This method assumes that the data structure is acyclical.
   * </b>
   *
   * @return a printable, displayable, transmittable representation of the
   * array.
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
   * <p>
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
   * @param indentFactor The number of spaces to add to each level of
   *                     indentation.
   * @return a printable, displayable, transmittable representation of the
   * object, beginning with <code>[</code>&nbsp;<small>(left
   * bracket)</small> and ending with <code>]</code>
   * &nbsp;<small>(right bracket)</small>.
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
   * </b>
   *
   * @return The writer.
   * @throws Fault
   */
  public java.io.Writer write(java.io.Writer writer) throws Fault {
    return this.write(writer, 0, 0);
  }

  /**
   * Write the contents of the VariantList as JSON text to a writer.
   * <p>
   * <p>If <code>indentFactor > 0</code> and the {@link VariantList}
   * has only
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
   * @param writer       Writes the serialized JSON
   * @param indentFactor The number of spaces to add to each level of
   *                     indentation.
   * @param indent       The indentation of the top level.
   * @return The writer.
   * @throws Fault
   */
  public java.io.Writer write(java.io.Writer writer, int indentFactor,
    int indent)
    throws Fault
  {
    try {
      boolean commanate = false;
      int length = this.length();
      writer.write('[');

      if (length == 1) {
        try {
          Variant.writeValue(writer, this.myArrayList.get(0),
            indentFactor, indent
          );
        } catch (Exception e) {
          throw new Fault("Unable to write VariantList value at" +
            " index: 0", e);
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
              indentFactor, newindent
            );
          } catch (Exception e) {
            throw new Fault("Unable to write VariantList " +
              "value at index: " + i, e);
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
   * Returns a java.util.List containing all of the elements in this
   * array.
   * If an element in the array is a VariantList or Variant it will also
   * be converted.
   * <p>
   * Warning: This method assumes that the data structure is acyclical.
   *
   * @return a java.util.List containing the elements of this array
   */
  public List<Object> toList() {
    List<Object> results = new ArrayList<Object>(this.myArrayList
      .size());
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