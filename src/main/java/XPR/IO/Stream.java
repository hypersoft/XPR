package XPR.IO;

import XPR.Fault;
import XPR.Kiosk;
import XPR.Plus;

import java.io.*;

import static XPR.Plus.classMember;
import static XPR.Plus.valueOf;

public class Stream {

  final static Class[] streamType = new Class[] {
    Closeable.class,        // 0 
    InputStream.class,      // 1
    OutputStream.class,     // 2
    RandomAccessFile.class, // 3
    DataOutputStream.class, // 4
    DataInputStream.class,  // 5
  };
  
  final static int INPUT_STREAM = 1, OUTPUT_STREAM = 2, RECORD_STREAM = 3,
    DATA_STREAM_OUT = 4, DATA_STREAM_IN = 5;
  
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

    @Override
    protected void onRemoved(Object key, Object value, boolean transfer) {
      Closeable stream = valueOf(value);
      if (value instanceof Flushable) try { ((Flushable)stream).flush();
      } catch (Exception e) {e.printStackTrace();}
      try {
        if (! transfer) stream.close();
      } catch (Exception e) {e.printStackTrace();}
    }
    
  }, new Kiosk.Storage.Type.RandomPointerMap(){
    @Override
    public Integer add(Object value) {
      if (classMember(value, streamType))
        return super.add(value);
      throw new Fault(new IllegalAccessException());
    }
  });

  static public <ANY> ANY get(int pointer) {
    return streamKiosk.get(pointer);
  }

  static public int add(Object link) {
    return streamKiosk.add(link);
  }

  static public void free(int pointer) {
    streamKiosk.delete(pointer);
  }

  static public Object transfer(int pointer) {
    return streamKiosk.transfer(pointer);
  }

  public static String getType(Integer pointer) {
    return streamKiosk.get(pointer).getClass().getSimpleName();
  }

  public static int getReadingStreamQueLength(Integer pointer) {
    Object stream = streamKiosk.get(pointer);
    if (Plus.classMember(stream, streamType[INPUT_STREAM])) {
      try { return ((InputStream) stream).available(); } catch (Exception e) {
        throw new Fault(e);
      }
    }
    throw new Fault(new UnsupportedOperationException());
  }

  public static void bookmarkReadingStream(Integer pointer, int readlimit) {
    InputStream stream = valueOf(streamKiosk.get(pointer));
    stream.mark(readlimit);
  }

  public static boolean canBookmarkReadingStream(Integer pointer) {
    Object stream = streamKiosk.get(pointer);
    if (Plus.classMember(stream, streamType[INPUT_STREAM])) {
      InputStream source = valueOf(stream);
      return source.markSupported();
    }
    return false;
  }
  
  public static int read(Integer pointer, Integer in) throws IOException,
    IllegalAccessException
  {
    Object stream = streamKiosk.get(pointer);
    if (Plus.classMember(stream, streamType[INPUT_STREAM])) {
      InputStream source = valueOf(stream);
      source.reset();
      return source.read(Buffer.get(in));
    }
    if (Plus.classMember(stream, streamType[RECORD_STREAM])) {
      RandomAccessFile database = valueOf(stream);
      return database.read(Buffer.get(in));
    }
    if (Plus.classMember(stream, streamType[DATA_STREAM_IN])) {
      DataInputStream database = valueOf(stream);
      return database.read(Buffer.get(in));
    }
    throw new Fault(new UnsupportedOperationException());
  }

  public static void flushWritingStream(Integer pointer)
  {
    Object stream = (OutputStream) streamKiosk.get(pointer);
    if (Plus.classMember(stream, Flushable.class)) {
      Flushable dest = valueOf(stream);
      try {
        dest.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return;
    }
    throw new Fault(new UnsupportedOperationException());
  }

  public static void write(Integer pointer, Integer out, boolean flush) throws IOException, IllegalAccessException
  {
    Object stream = (OutputStream) streamKiosk.get(pointer);
    if (Plus.classMember(stream, streamType[INPUT_STREAM])) {
      byte[] source = Buffer.get(out);
      OutputStream dest = valueOf(stream);
      dest.write(source);
      if (flush) dest.flush();
      return;
    }
    if (Plus.classMember(stream, streamType[RECORD_STREAM])) {
      RandomAccessFile dest = valueOf(stream);
      dest.write(Buffer.get(out));
      return;
    }
    if (Plus.classMember(stream, streamType[DATA_STREAM_OUT])) {
      DataOutputStream dest = valueOf(stream);
      dest.write(Buffer.get(out));
      return;
    }
    throw new Fault(new UnsupportedOperationException());
  }

  public static void closeStream(Integer id) { streamKiosk.delete(id); }
  
  public static Integer getFileRecordStream(String path) {
    try {
      RandomAccessFile f = new RandomAccessFile(path, "rw");
      return streamKiosk.add(f);
    } catch (FileNotFoundException e) {
      throw new Fault(e);
    }
  }
  
  public static Integer getFileReadingStream(String file) throws
    FileNotFoundException
  {
    File path = new File(file);
    InputStream x = new FileInputStream(path);
    return streamKiosk.add(x);
  }

  public static Integer getFileWritingStream(String file) throws
    FileNotFoundException
  {
    File path = new File(file);
    OutputStream x = new FileOutputStream(path);
    return streamKiosk.add(x);
  }

  public static Integer getDataOutputStream(Integer pointer) {
    Object stream = valueOf(streamKiosk.get(pointer));
    if (Plus.classMember(stream, streamType[OUTPUT_STREAM])) {
      OutputStream dest = streamKiosk.transfer(pointer);
      return streamKiosk.add(new DataOutputStream(dest));
    }
    if (Plus.classMember(stream, streamType[RECORD_STREAM]))
      return pointer;
    throw new Fault(new UnsupportedOperationException());
  }

  private static byte[] captureWholeReadingStream(InputStream is, int initialBufferCapacity) throws IOException {
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

  public static Integer readWholeStreamToBuffer(Integer pointer, int bufferSize)
  {
    Object stream = streamKiosk.get(pointer);
    if (Plus.classMember(stream, streamType[RECORD_STREAM])) {
      RandomAccessFile f = valueOf(stream);
      try {
        int length = valueOf(f.length() - f.getFilePointer());
        byte[] data = new byte[length];
        f.readFully(data);
        return Buffer.add(data);
      } catch (Exception e) { throw new Fault(e); }
    }
    if (Plus.classMember(stream, streamType[INPUT_STREAM])) {
      try {
        byte[] units = captureWholeReadingStream(
          streamKiosk.transfer(pointer),
          bufferSize == 0 ? 1024 : bufferSize
        );
        return Buffer.add(units);
      } catch (Exception e) {throw new Fault(e);}
    }
    throw new Fault(new UnsupportedOperationException());
  }

  public static class Pipes {
    public static class Synthetic {
      public static class Pipe {
        public final Destination destination;
        public final Source source;
        final int pointer[] = new int[2];
        public Pipe() {
          destination = new Destination();
          try {
            source = new Source(destination);
          } catch (IOException e) {
            throw new Fault(e);
          }
          pointer[0] = Stream.add(destination);
          pointer[1] = Stream.add(source);
        }
        public int getPointer(int number) {
          return pointer[number];
        }
      }
      public static class Destination extends PipedOutputStream {
        public Destination(){}
      }
      public static class Source extends PipedInputStream {
        public Source(Destination d) throws IOException {super(d);}
      }
    }
  }
}
