package XPR.IO;

import XPR.Fault;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compression { private Compression(){}

  public static class Zip { private Zip(){}

    public Integer getZipOutputStream(Integer outputstream) {
      try {
        OutputStream os = XPR.IO.Stream.get(outputstream);
        if (os instanceof GZIPOutputStream) return outputstream;
        GZIPOutputStream gz = new GZIPOutputStream(os);
        return Stream.add(gz);
      } catch (IOException e) {
        throw new Fault(e);
      }
    }
    public Integer getZipInputStream(Integer inputstream) {
      try {
        InputStream is = XPR.IO.Stream.get(inputstream);
        if (is instanceof GZIPInputStream) return inputstream;
        GZIPInputStream gz = new GZIPInputStream(is);
        return Stream.add(gz);
      } catch (IOException e) {
        throw new Fault(e);
      }
    }
  }

}
