package XPR;

import java.util.HashMap;
import java.util.Map;

import static XPR.Plus.valueOf;

public class System { private System(){}

  static private HashMap<String, String> copyVirons() {
    Map<String, String> source = java.lang.System.getenv();
    HashMap<String, String> localMap;
    localMap = new HashMap<>(source.size() + 16);
    localMap.putAll(source);
    return localMap;
  }

  public static class Viron{ private Viron(){};

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

  }
}
