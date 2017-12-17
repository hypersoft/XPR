package XPR.IO;

import XPR.Plus;
import com.sun.istack.internal.NotNull;

import static XPR.Plus.valueOf;

/**
   * A two-way-data-transformation-gate, with validation function.
   * @author pc.wiz.tt@gmail.com
   */
  public abstract class Codec {
    static final public Transformation FORWARD = Transformation.SOURCE;
    static final public Transformation BACKWARD = Transformation.OUTPUT;

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

    public abstract static class Buffer extends Codec {
      @Override
      public boolean canTransform(Transformation type, @NotNull Object data) {
        Class dataType = data.getClass();
        if (! Plus.classMatch(dataType, byte.class) ||
            ! Plus.basicListClass(dataType)) return false;
        return true;
      }
    }
  }
