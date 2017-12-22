package XPR.IO;

import XPR.DeadBug;
import XPR.Fault;
import XPR.Kiosk;
import XPR.Plus;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static XPR.Plus.valueOf;

public class Stream {

  final static Class
    READING_STREAM = InputStream.class,
    WRITING_STREAM = OutputStream.class,
    RECORD_STREAM = RandomAccessFile.class,
    DATA_STREAM_OUT = DataOutputStream.class,
    DATA_STREAM_IN = DataInputStream.class;

  final static Class[] streamType = new Class[]{
    Closeable.class,        // 0 
    READING_STREAM,         // 1
    WRITING_STREAM,         // 2
    RECORD_STREAM,          // 3
    DATA_STREAM_OUT,        // 4
    DATA_STREAM_IN,         // 5
  };


  private static Kiosk streamKiosk = new Kiosk(new Kiosk.Supervisor() {
    @Override
    public boolean permit(Kiosk.Operation operation, Object key) {
      switch (operation) {
        case GET_KEY:
        case TRANSFER_KEY:
        case DELETE_KEY:
        case ADD_KEY:
          return true;
        default:
          break;
      }
      return false;
    }

    @Override
    protected void onRemoved(Object key, Object value, boolean transfer) {
      Closeable stream = valueOf(value);
      if (value instanceof Flushable) try {
        ((Flushable) stream).flush();
      } catch (Exception e) {e.printStackTrace();}
      try {
        if (!transfer) stream.close();
      } catch (Exception e) {e.printStackTrace();}
    }

  }, new Kiosk.Storage.Type.RandomPointerMap() {
    @Override
    public Integer add(Object value) {
      if (Plus.classMember(value, streamType))
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
    if (Plus.classMember(stream, READING_STREAM)) {
      try { return ((InputStream) stream).available(); } catch (Exception e) {
        throw new Fault(e);
      }
    }
    throw new Fault.WrongStreamType(stream.getClass().getName());
  }

  public static void bookmarkReadingStream(Integer pointer, int readlimit) {
    Object stream = streamKiosk.get(pointer);
    if (Plus.classMember(stream, READING_STREAM)) try {
      InputStream source = valueOf(stream);
      source.mark(readlimit);
      return;
    } catch (Exception e) { throw new Fault(e); }
    throw new Fault.WrongStreamType(stream.getClass().getName());
  }

  public static void resetReadingStream(Integer pointer) {
    Object stream = valueOf(streamKiosk.get(pointer));
    if (Plus.classMember(stream, READING_STREAM)) try {
      InputStream source = valueOf(stream);
      source.reset();
      return;
    } catch (IOException e) { throw new Fault(e); }
    throw new Fault.WrongStreamType(stream.getClass().getName());
  }

  public static boolean canBookmarkReadingStream(Integer pointer) {
    Object stream = streamKiosk.get(pointer);
    if (Plus.classMember(stream, READING_STREAM)) {
      InputStream source = valueOf(stream);
      return source.markSupported();
    }
    return false;
  }

  public static int read(Integer pointer, Integer in) throws IOException,
    IllegalAccessException
  {
    Object stream = streamKiosk.get(pointer);
    if (Plus.classMember(stream, READING_STREAM)) {
      InputStream source = valueOf(stream);
      source.reset();
      return source.read(Buffer.get(in));
    }
    if (Plus.classMember(stream, RECORD_STREAM)) {
      RandomAccessFile database = valueOf(stream);
      return database.read(Buffer.get(in));
    }
    if (Plus.classMember(stream, DATA_STREAM_IN)) {
      DataInputStream database = valueOf(stream);
      return database.read(Buffer.get(in));
    }
    if (Plus.classMember(stream, Pipes.Synthetic.Pipe.class)) {
      Pipes.Synthetic.Pipe pipe = valueOf(stream);
      return pipe.readingPipe.read(Buffer.get(in));
    }
    if (Plus.classMember(stream, Pipes.Real.Pipe.class)) {
      Pipes.Real.Pipe pipe = valueOf(stream);
      return pipe.source.read(ByteBuffer.wrap(Buffer.get(in)));
    }
    throw new Fault.WrongStreamType(stream.getClass().getName());
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
    if (Plus.classMember(stream, Pipes.Real.Pipe.class)) {
      Pipes.Real.Pipe pipe = valueOf(stream);
      return;
    }
    if (Plus.classMember(stream, Pipes.Synthetic.Pipe.class)) {
      Pipes.Synthetic.Pipe pipe = valueOf(stream);
      try {
        pipe.writingPipe.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return;
    }
    throw new Fault.WrongStreamType(stream.getClass().getName());
  }

  public static void write(Integer pointer, Integer out,
    boolean flush) throws IOException, IllegalAccessException
  {
    Object stream = (OutputStream) streamKiosk.get(pointer);
    if (Plus.classMember(stream, READING_STREAM)) {
      byte[] source = Buffer.get(out);
      OutputStream dest = valueOf(stream);
      dest.write(source);
      if (flush) dest.flush();
      return;
    }
    if (Plus.classMember(stream, RECORD_STREAM)) {
      RandomAccessFile dest = valueOf(stream);
      dest.write(Buffer.get(out));
      return;
    }
    if (Plus.classMember(stream, DATA_STREAM_OUT)) {
      DataOutputStream dest = valueOf(stream);
      dest.write(Buffer.get(out));
      if (flush) dest.flush();
      return;
    }
    if (Plus.classMember(stream, Pipes.Real.Pipe.class)) {
      Pipes.Real.Pipe pipe = valueOf(stream);
      pipe.out.write(ByteBuffer.wrap(Buffer.get(out)));
      return;
    }
    if (Plus.classMember(stream, Pipes.Synthetic.Pipe.class)) {
      Pipes.Synthetic.Pipe pipe = valueOf(stream);
      pipe.writingPipe.write(Buffer.get(out));
      return;
    }
    throw new Fault.WrongStreamType(stream.getClass().getName());
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

  @DeadBug("OBL_UNSATISFIED_OBLIGATION")
  public static Integer getFileReadingStream(String file) throws
    FileNotFoundException
  {
    File path = new File(file);
    InputStream x = new FileInputStream(path);
    return streamKiosk.add(x);
  }

  @DeadBug("OBL_UNSATISFIED_OBLIGATION")
  public static Integer getFileWritingStream(String file) throws
    FileNotFoundException
  {
    File path = new File(file);
    OutputStream x = new FileOutputStream(path);
    return streamKiosk.add(x);
  }

  public static Integer getDataOutputStream(Integer pointer) {
    Object stream = valueOf(streamKiosk.get(pointer));
    if (Plus.classMember(stream, WRITING_STREAM)) {
      OutputStream dest = streamKiosk.transfer(pointer);
      return streamKiosk.add(new DataOutputStream(dest));
    }
    if (Plus.classMember(stream, RECORD_STREAM))
      return pointer;
    throw new Fault.WrongStreamType(stream.getClass().getName());
  }

  private static byte[] captureWholeReadingStream(InputStream is,
    int initialBufferCapacity) throws IOException
  {
    if (initialBufferCapacity <= 0) {
      throw new IllegalArgumentException(
        "Bad initialBufferCapacity: " + initialBufferCapacity);
    } else {
      byte[] buffer = new byte[initialBufferCapacity];
      int cursor = 0;

      while (true) {
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
    if (Plus.classMember(stream, RECORD_STREAM)) {
      RandomAccessFile f = valueOf(stream);
      try {
        int length = valueOf(f.length() - f.getFilePointer());
        byte[] data = new byte[length];
        f.readFully(data);
        return Buffer.add(data);
      } catch (Exception e) { throw new Fault(e); }
    }
    if (Plus.classMember(stream, READING_STREAM)) {
      try {
        InputStream source = valueOf(stream);
        byte[] units = captureWholeReadingStream(
          source, bufferSize == 0 ? 1024 : bufferSize
        );
        streamKiosk.transfer(pointer);
        return Buffer.add(units);
      } catch (Exception e) {throw new Fault(e);}
    }
    throw new Fault.WrongStreamType(stream.getClass().getName());
  }

  public static class Pipes {

    public static int createRealPipe() {
      Real.Pipe pipe = new Real.Pipe();
      return Stream.add(pipe);
    }

    public static int createSyntheticPipe() {
      Synthetic.Pipe pipe = new Synthetic.Pipe();
      return Stream.add(pipe);
    }

    public static class Real {
      public static class Pipe implements Closeable {
        private final java.nio.channels.Pipe p;
        private final java.nio.channels.Pipe.SinkChannel out;
        private final java.nio.channels.Pipe.SourceChannel source;

        public Pipe() {
          try {
            p = java.nio.channels.Pipe.open();
            out = p.sink();
            source = p.source();
          } catch (IOException e) { throw new Fault(e);}
        }

        public java.nio.channels.Pipe.SinkChannel getWritingPipe() {
          return out;
        }

        public java.nio.channels.Pipe.SourceChannel getSource() {
          return source;
        }

        @Override
        public void close() {
          try {
            out.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
          try {
            source.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }

    public static class Synthetic {
      public static class Pipe implements Closeable {
        public final WritingPipe writingPipe;
        public final ReadingPipe readingPipe;

        public Pipe() {
          writingPipe = new WritingPipe();
          try {
            readingPipe = new ReadingPipe(writingPipe);
          } catch (IOException e) {
            throw new Fault(e);
          }
        }

        @Override
        public void close() throws IOException {
          writingPipe.close();
          readingPipe.close();
        }

        public static class WritingPipe extends PipedOutputStream {
          public WritingPipe() {}
        }

        public static class ReadingPipe extends PipedInputStream {
          public ReadingPipe(WritingPipe d) throws IOException {super(d);}
        }
      }
    }

    public static class Compression {
      private Compression() {}

      public static class Zip {
        private Zip() {}

        public Integer startZipOutputStream(Integer outputstream) {
          Object stream = get(outputstream);
          if (Plus.classMember(stream, WRITING_STREAM)) try {
            GZIPOutputStream gz = new GZIPOutputStream(valueOf(stream));
            return add(gz);
          } catch (Exception e) { throw new Fault(e); }
          throw new Fault.WrongStreamType(stream.getClass().getName());
        }

        public void endZipOutputStream(Integer pointer) {
          Object stream = get(pointer);
          if (Plus.classMember(stream, GZIPOutputStream.class)) {
            GZIPOutputStream gz = valueOf(stream);
            try {
              gz.finish();
              return;
            } catch (Exception e) { throw new Fault(e); }
          }
          throw new Fault.WrongStreamType(stream.getClass().getName());
        }

        public Integer getZipInputStream(Integer inputstream) {
          Object stream = get(inputstream);
          if (Plus.classMember(stream, READING_STREAM)) try {
            GZIPInputStream gz = new GZIPInputStream(valueOf(stream));
            return add(gz);
          } catch (Exception e) { throw new Fault(e); }
          throw new Fault.WrongStreamType(stream.getClass().getName());
        }
      }

    }
  }
}
