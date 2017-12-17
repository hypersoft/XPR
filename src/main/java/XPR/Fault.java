package XPR;

/**
 * A runtime fault class featuring error code support.
 *
 *
 */
public class Fault extends RuntimeException {

  private static Kiosk kiosk = getSession();

  public final static int noFaultCode = 0;

  static private Kiosk getSession() {
    kiosk = new Kiosk(new Kiosk.Supervisor(){
      @Override
      public boolean permit(Kiosk.Operation operation, Object key) {
        switch (operation) {
          case SET_KEY: if (kiosk.existingKey(key)) {
            throw new Fault(
              "cannot synchronize fault code message"
                + Speak.quoteAnd("the key")
                + Speak.quoteCitation(key) + " exists as"
                + Speak.quoteExactTarget(kiosk.get(key))
            );
          } else return true;
          case CHECK_KEY:
          case GET_KEY: return true;
          default: return false;
        }
      }
    });
    kiosk.set(noFaultCode, "no system error message is known for this error code");
    return kiosk;
  }

  private int code = noFaultCode;

  public Fault(){};
  public Fault(Throwable e) {
    super(e);
  }
  public Fault(String message) {
    super(message);
  }
  public Fault(String message, Throwable e) { super(message, e); }
  public Fault(String message, int code) { super(message); this.code = code; }
  public Fault(int code) { super(getCodeMessage(code)); this.code = code; }
  public final int getFaultCode() {
    return code;
  }
  public final String getFaultMessage() { return getCodeMessage(code); }

  public static final void registerCodeMessage(int code, String message) {
    kiosk.set(code, message);
  }

  public static final String getCodeMessage(int code) {
    return kiosk.get(code);
  }

  public static final boolean hasCodeMessage(int code) {
    return kiosk.existingKey(code);
  }

}
