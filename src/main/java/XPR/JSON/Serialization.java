package XPR.JSON;

/**
* The <code>Serialization</code> interface allows a <code>toJSON()</code>
* method so that a class can change the behavior of
* <code>Variant.toString()</code>, <code>VariantList.toString()</code>,
* and <code>Composer.value(</code>Object<code>)</code>. The
* <code>toJSON</code> method will be used instead of the default behavior
* of using the Object's <code>toString()</code> method and quoting the result.
*/
public interface Serialization {
  /**
   * The <code>toJSON</code> method allows a class to produce its own JSON
   * serialization.
   *
   * @return A strictly syntactically correct JSON text.
   */
  public String toJSON();
}
