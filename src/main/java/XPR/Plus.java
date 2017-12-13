package XPR;

import com.sun.istack.internal.NotNull;

public class Plus {
  
  private Plus(){};

  public interface ResourcePathLoader {
    @NotNull
    String getResourceFor(String path);
  }

}
