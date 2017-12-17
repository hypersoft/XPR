package XPR.Crypto;

import XPR.Fault;
import XPR.IO.Codec;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import static XPR.Plus.valueOf;

final public class Authentication {

    private Authentication(){}

    public static final Codec.Buffer MD5Hash = new Codec.Buffer() {

      private final static String cipher = "MD5";
      private final static String name = cipher + "Hash";

      private final static String backwardTransformationFaultMessage
        = name + " cannot create backward transformations";

      @Override
      public boolean canTransform(Transformation type, Object data) {
        if (type.equals(Codec.BACKWARD)) return false;
        return super.canTransform(type, data);
      }
      @Override
      public <ANY> ANY transform(Transformation direction, Object data) {
        if (direction.equals(Codec.BACKWARD)) throw new Fault(
          new UnsupportedEncodingException(backwardTransformationFaultMessage)
        );
        MessageDigest md;
        byte[] bytes = valueOf(data);
        try {
          md = MessageDigest.getInstance(cipher);
          return valueOf(md.digest(bytes));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
      @Override
      public String getName() {
        return name;
      }
    };

  }
