package XPR;

/**
 * A runtime fault class featuring error code support.
 *
 *
 */
public class Fault extends RuntimeException {

  private static final Kiosk kiosk = getSession();

  public final static int noFaultCode = 0;

  static private Kiosk getSession() {
    Kiosk k = new Kiosk(new Kiosk.Supervisor(){
      @Override
      public boolean permit(Kiosk.Operation operation, Object id) {
        switch (operation) {
          case SET_KEY: if (kiosk.existingKey(id)) {
            throw new Fault(
              "cannot synchronize fault code message"
                + Speak.quoteAnd("the key")
                + Speak.quoteCitation(id) + " exists as"
                + Speak.quoteExactTarget(kiosk.get(id))
            );
          }
          case CHECK_KEY:
          case GET_KEY: return true;
          default: return false;
        }
      }
    });
    k.set(noFaultCode, "no system error message is known for this error code");
    return k;
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
