package XPR.JSON;

import XPR.Fault;
import XPR.JSON.Type.Variant;

import java.io.IOException;

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
public class Composer {
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
  private final Variant stack[];

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
      this.stack = new Variant[maxdepth];
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
              this.writer.append(Variant.quote(string));
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
          this.push(new Variant());
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
  private void push(Variant jo) throws Fault {
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
      return this.append(Variant.valueToString(object));
  }
}
