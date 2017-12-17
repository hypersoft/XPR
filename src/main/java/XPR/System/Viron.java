package XPR.System;

import XPR.Fault;
import XPR.IO.Stream;
import XPR.Plus;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.FileSystemException;
import java.util.HashMap;
import java.util.Map;

import static XPR.Plus.valueOf;

public class Viron { private Viron(){};

    static private HashMap<String, String> copyVirons() {
      Map<String, String> source = java.lang.System.getenv();
      HashMap<String, String> localMap;
      localMap = new HashMap<>(source.size() + 16);
      localMap.putAll(source);
      return localMap;
    }

    static private HashMap<String, String> virons = copyVirons();

    static public Map<String, String> readTable() {
     return valueOf(virons.clone());
   }

   static public void writeTable(Map<String, String> table) {
     virons.putAll(table);
   }

   static public String get(String key) {
     return virons.get(key);
   }

   static public void set(String key, String value) {
     virons.put(key, value);
   }

   static public String clearKey(String key) {
     return virons.remove(key);
   }

   static public void clearKeys() {
     virons.clear();
   }

   static public String[] getKeys() {
     return Plus.getBasicListOf(virons.keySet());
   }

   static public void loadSystemTable() {
     virons = copyVirons();
   }

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

    static private final String javaWorkingDirectory = java.lang.System.getProperty("user.dir");
    static private String cwd = javaWorkingDirectory;

    static public boolean currentWorkingDirectoryConflict() {
      return ! cwd.equals(javaWorkingDirectory);
    }

    static public void resetCurrentWorkingDirectory() {
      cwd = javaWorkingDirectory;
    }

    public static String getCurrentWorkingDirectory() {
      return cwd;
    }

    static public void setCurrentWorkingDirectory(String path) {
      File f = new File(path);
      if (! f.isAbsolute() && currentWorkingDirectoryConflict())
        f = new File(cwd, path);
      if (! f.isDirectory())
        throw new Fault(new FileSystemException("cannot change directory; file not found"));
      cwd = f.getAbsolutePath();
    }

  static private int ios[] = new int[] {
    Stream.add(System.in),
    Stream.add(System.out),
    Stream.add(System.err)
  };

  public static boolean setInputStream(Integer pointer) {
    InputStream x = (InputStream) Stream.get(pointer);
    java.lang.System.setIn(x);
    ios[0] = pointer;
    return true;
  }

  static String selectStringCodec(String source) {
    if (source == null) return getCharSetName();
    else return source;
  }

  public static Integer getInputStream() { return ios[0]; }

  public static boolean setOutputStream(Integer pointer, String codec) throws
    UnsupportedEncodingException
  {
    OutputStream stream = (OutputStream) Stream.get(pointer);
    java.lang.System.setOut(new PrintStream(stream, true, selectStringCodec(codec)));
    ios[1] = pointer;
    return true;
  }

  public static Integer getOutputStream() { return ios[1]; }

  public static boolean setStandardErrorStream(Integer pointer, String codec) throws
    UnsupportedEncodingException {
    OutputStream x = (OutputStream) Stream.get(pointer);
    java.lang.System.setErr(new PrintStream(x, true, selectStringCodec(codec)));
    ios[2] = pointer;
    return true;
  }

  public static Integer getErrorStream() { return ios[2]; }

  }
