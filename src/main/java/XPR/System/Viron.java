package XPR.System;

import XPR.Fault;
import XPR.Plus;

import java.io.File;
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

  }
