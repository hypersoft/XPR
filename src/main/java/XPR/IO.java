package XPR;

import com.sun.istack.internal.NotNull;

import java.nio.charset.Charset;

public class IO {

  public static final String getCharSetName() {
    return getCharSet().name();
  }

  public static final Charset getCharSet() {
    return Charset.forName(java.lang.System.getProperty("file.encoding"));
  }

  public static final void setCharSet(Charset cs) {
    java.lang.System.setProperty("file.encoding", cs.name());
  }

  public static final Charset getSystemCharSet() {
    return Charset.defaultCharset();
  }

  /**
   * A two-way-data-transformation-gate, with validation function.
   * @author pc.wiz.tt@gmail.com
   */
  public interface Codec {
    /**
     * <p></p>The transformation validation method.</p>
     *
     * <p>NOTE:&nbsp;<i>The default value for this method always returns true.</i></p>
     *
     * @param type The type sought for a transformation.
     * @param data The data being supplied for a transformation.
     * @return value will be true if this transformer can successfully handle the requested operation.
     */
    default boolean canTransform(Transformation type, Object data) {
      return true;
    }

    /**
     * A convenience function which casts the data to the type being sought for assignment.
     * @param data The data which has been transformed.
     * @param <ANY> Automatic Type Cast
     * @return the data parameter
     */
    default <ANY> ANY value(Object data) {
      return (ANY) data;
    }

    /**
     * <p>The transformation method.</p>
     *
     * <p>You <b>MUST</b> call the canTransform method on this <cite>Codec</cite> before
     * calling this method to validate the transformation operation.</p>
     *
     * @param direction The direction sought for this transformation.
     * @param data The data being supplied for this transformation.
     * @param <ANY> Automatic Type Cast
     * @return the value of the transformation performed.
     */
    default <ANY> ANY transform(Transformation direction, Object data) {
      return (ANY) data;
    }
    String getName();

    /**
     * Codec Direction Specifiers
     */
    enum Transformation {
      INPUT, OUTPUT
    }
  }

  public abstract static class ByteArrayCodec implements Codec
  {
    static final public Transformation FORWARD = Transformation.INPUT;
    static final public Transformation BACKWARD = Transformation.OUTPUT;
    @Override
    final public <ANY> ANY value(Object data) {
      return null;
    }
    @Override
    public boolean canTransform(Transformation type, @NotNull Object data) {
      Class c = data.getClass();
      if (! c.equals(byte.class) ||! c.isArray()) return false;
      return true;
    }
  }
}
