package XPR;

import com.sun.istack.internal.NotNull;

public class Plus {
  
  private Plus(){};

  public static <ANY> ANY valueOf(Object v){return (ANY)v;}

  public final static class Help { private Help(){}

    public interface Locator {
      @NotNull String locateHelpFor(String path);
    }

  }

}
