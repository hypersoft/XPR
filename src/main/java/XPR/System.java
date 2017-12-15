package XPR;

import java.util.HashMap;
import java.util.Map;

import static XPR.Plus.valueOf;

public class System { private System(){}

  public static class Viron{ private Viron(){};

   private HashMap<String, String> virons = loadVirons();

   private HashMap loadVirons() {
     Map env = java.lang.System.getenv();
     HashMap<String, String> localMap;
     localMap = new HashMap<>(env.size() + 32);
     localMap.putAll(env);
     return localMap;
   }

   public Map<String, String> clone() {
     return valueOf(virons.clone());
   }

   public String get(String key) {
     return virons.get(key);
   }

   public void set(String key, String value) {
     virons.put(key, value);
   }

   public String delete(String key) {
     return virons.remove(key);
   }

   public String[] keys() {
     return valueOf(virons.keySet().toArray());
   }

   public void reset() {
     virons = loadVirons();
   }

   public void copy(Map<String, String> table) {
     virons.putAll(table);
   }

  }
}
