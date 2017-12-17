package XPR;

import java.util.concurrent.ThreadLocalRandom;

public class Math { private Math(){}
  static public int getRandomInteger(Integer min, Integer max) {
    if (max == Integer.MAX_VALUE) --max;
    return ThreadLocalRandom.current().nextInt(min, max + 1);
  }

  static public long getRandomLong(Integer min, Integer max) {
    if (max == Long.MAX_VALUE) --max;
    return ThreadLocalRandom.current().nextLong(min, max + 1);
  }
}
