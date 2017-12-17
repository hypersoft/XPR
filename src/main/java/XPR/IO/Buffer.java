package XPR.IO;

import XPR.Fault;
import XPR.Kiosk;
import XPR.Plus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static XPR.Plus.valueOf;

public class Buffer {

  private Buffer() {}

  static private long bytesAllocated = 0;

  static private Kiosk bufferKiosk = new Kiosk(new Kiosk.Supervisor() {

    @Override
    public void onAdded(Object key, Object value) {
      byte[] buffer = valueOf(value);
      bytesAllocated += buffer.length;
    }

    @Override
    public void onRemoved(Object key, Object value, boolean transfer) {
      byte[] buffer = valueOf(value);
      bytesAllocated -= buffer.length;
    }

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
      if (Plus.classMember(value, byte[].class)) return super.add(value);
      throw new Fault(new IllegalArgumentException());
    }
  });

  static public Integer create(int width, int length) {
    switch (width) {
      case 1:
      case 2:
      case 4:
      case 8:
        break;
      default:
        throw new IllegalArgumentException("width must be 1, 2, 4, or 8");
    }
    byte[] buffer = new byte[width * length];
    return bufferKiosk.add(buffer);
  }

  static public void free(Integer id) {bufferKiosk.delete(id);}

  static public long getBytesAllocated() {return bytesAllocated;}

  static public long getUnits() {
    return bufferKiosk.length();
  }

  static public Integer sliceBuffer(Integer id, int offset, int length)
    throws IllegalAccessException
  {
    byte[] unit = bufferKiosk.get(id);
    return bufferKiosk.add(Arrays.copyOfRange(unit, offset, length));
  }

  static public byte[] get(Integer id) {
    return bufferKiosk.get(id);
  }

  static public Integer add(byte[] buffer) {
    return bufferKiosk.add(buffer);
  }

  static byte[] transfer(Integer id) {
    return bufferKiosk.transfer(id);
  }

  public static long lengthOf(Integer bufferId)
    throws IllegalAccessException
  {
    byte[] unit = bufferKiosk.get(bufferId);
    return unit.length;
  }

  public static Integer createBufferOutputStream(int bytes)
    throws IllegalAccessException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes);
    return Stream.add(baos);
  }

  public static Integer copyBufferOutputStreamBytes(Integer bufferOutputStream)
    throws IllegalAccessException, IOException
  {
    Object bOS = Stream.get(bufferOutputStream);
    if (bOS instanceof ByteArrayOutputStream) {
      ByteArrayOutputStream stream = valueOf(bOS);
      return add(stream.toByteArray());
    }
    return 0;
  }

}
