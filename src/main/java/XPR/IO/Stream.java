package XPR.IO;

import XPR.Fault;
import XPR.Kiosk;

import java.io.*;

import static XPR.Plus.classMember;
import static XPR.Plus.valueOf;

public class Stream {

  private static Kiosk streamKiosk = new Kiosk(new Kiosk.Supervisor(){
    @Override
    public boolean permit(Kiosk.Operation operation, Object key) {
      switch (operation) {
        case GET_KEY:
        case TRANSFER_KEY:
        case DELETE_KEY:
        case ADD_KEY: return true;
      }
      return false;
    }
  }, new Kiosk.Storage.Type.RandomPointerMap(){
    @Override
    public Integer add(Object value) {
      if (classMember(value, OutputStream.class, InputStream.class))
        return super.add(value);
      throw new Fault(new UnsupportedOperationException());
    }

    @Override
    public void delete(Integer key) {
      Object value = store.get(key);
      if (value instanceof OutputStream) {
        OutputStream x = valueOf(value);
        try { x.flush(); } catch (IOException e) {}
        try { x.close(); } catch (IOException e) { throw new Fault(e); } finally { super.delete(key); }
      } else {
        InputStream x = valueOf(value);
        try { x.close(); } catch (IOException e) { throw new Fault(e); } finally { super.delete(key); }
      }
    }
  });

  static public <ANY> ANY get(int pointer) {
    return streamKiosk.get(pointer);
  }

  static public int add(Closeable link) {
    return streamKiosk.add(link);
  }

  static public void free(int pointer) {
    streamKiosk.delete(pointer);
  }

  static public Object transfer(int pointer) {
    return streamKiosk.transfer(pointer);
  }

  public static String getStreamType(Integer id) {
    return streamKiosk.get(id).getClass().getSimpleName();
  }

  public static int getInputStreamBytesReady(Integer id) throws IOException {
    return ((InputStream) streamKiosk.get(id)).available();
  }

  public static int readInputStream(Integer id, Integer in) throws IOException,
    IllegalAccessException
  {
    InputStream x = (InputStream) streamKiosk.get(id);
    return x.read(Buffer.get(in));
  }

  public static void writeOutputStream(Integer id, Integer out,
    boolean flush) throws IOException, IllegalAccessException
  {
    OutputStream x = (OutputStream) streamKiosk.get(id);
    byte[] source = Buffer.get(out);
    x.write(source);
    if (flush) x.flush();
  }

  public static void closeStream(Integer id) { streamKiosk.delete(id); }

  public static Integer openFileInputStream(String file) throws
    FileNotFoundException
  {
    File path = new File(file);
    InputStream x = new FileInputStream(path);
    return streamKiosk.add(x);
  }

  public static Integer openFileOutputStream(String file) throws
    FileNotFoundException
  {
    File path = new File(file);
    OutputStream x = new FileOutputStream(path);
    return streamKiosk.add(x);
  }

  private static byte[] readStream(InputStream is, int initialBufferCapacity) throws IOException {
    if (initialBufferCapacity <= 0) {
      throw new IllegalArgumentException("Bad initialBufferCapacity: " + initialBufferCapacity);
    } else {
      byte[] buffer = new byte[initialBufferCapacity];
      int cursor = 0;

      while(true) {
        int n = is.read(buffer, cursor, buffer.length - cursor);
        if (n < 0) {
          if (cursor != buffer.length) {
            byte[] tmp = new byte[cursor];
            System.arraycopy(buffer, 0, tmp, 0, cursor);
            buffer = tmp;
          }

          return buffer;
        }

        cursor += n;
        if (cursor == buffer.length) {
          byte[] tmp = new byte[buffer.length * 2];
          System.arraycopy(buffer, 0, tmp, 0, cursor);
          buffer = tmp;
        }
      }
    }
  }

  public static Integer inputStreamToBuffer(Integer id, int bufferSize)
    throws IllegalAccessException, IOException
  {
    byte[] units = readStream(
      (InputStream) streamKiosk.transfer(id),
      bufferSize == 0 ? 1024 : bufferSize
    );
    return Buffer.add(units);
  }

}
