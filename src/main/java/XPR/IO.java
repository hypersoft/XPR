package XPR;

import com.sun.istack.internal.NotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import static XPR.Plus.valueOf;

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
  public abstract static class Codec {
    /**
     * <p></p>The transformation validation method.</p>
     *
     * <p>NOTE:&nbsp;<i>The default value for this method always returns true.</i></p>
     *
     * @param type The type sought for a transformation.
     * @param data The data being supplied for a transformation.
     * @return value will be true if this transformer can successfully handle the requested operation.
     */
    public boolean canTransform(Transformation type, Object data) {
      return true;
    }

    /**
     * <p>The transformation method.</p>
     *
     * <p>You <b>MUST</b> call the canTransform method on this <cite>Codec</cite> before
     * calling this method to validate the transformation operation.</p>
     *
     * @param direction The direction sought for this transformation.
     * @param data The data being supplied for this transformation.
     * @param <ANY> Automatic Type Cast.
     * @return the value of the transformation performed.
     */
    public <ANY> ANY transform(Transformation direction, Object data) {
      return valueOf(data);
    }

    public abstract String getName();

    /**
     * Codec Direction Specifiers
     */
    public enum Transformation {
      SOURCE, OUTPUT
    }

    public static class Type {

      static final public Transformation FORWARD = Transformation.SOURCE;
      static final public Transformation BACKWARD = Transformation.OUTPUT;

      private Type() {}

      public abstract static class Buffer extends Codec {
        @Override
        public boolean canTransform(Transformation type, @NotNull Object data) {
          Class dataType = data.getClass();
          if (! Plus.sameClass(dataType, byte.class) ||
              ! Plus.dimensionalValueClass(dataType)) return false;
          return true;
        }
      }
    }
    public abstract static class Stream extends Codec {
      private final static Class[] classes = new Class[]{
        InputStream.class, OutputStream.class
      };
      @Override
      public boolean canTransform(Transformation type, @NotNull Object data) {
        Class dataType = data.getClass();
        if (! Plus.sameClass(dataType, classes)) return false;
        return true;
      }
    }
  }

}
