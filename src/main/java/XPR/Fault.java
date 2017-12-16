package XPR;

/**
 * A runtime fault class featuring error code support.
 */
public class Fault extends RuntimeException {

  private static final Kiosk<Integer, String> codeRegister
    = new Kiosk<Integer, String>(Fault.class.getSimpleName());

  public final static int noFaultCode
    = codeRegister.set(0, "No system information is known for this fault code");

  private int code = noFaultCode;

  public Fault(){};

  public Fault(Throwable e) {
    super(e);
  }

  public Fault(String message) {
    super(message);
  }

  public Fault(String message, Throwable e) {
    super(message, e);
  }

  public Fault(String message, int code) {
    super(message);
    this.code = code;
  }

  public Fault(int code) {
    this.code = code;
  }

  public static final void registerCodeMessage(int code, String message) {
    if (codeRegister.has(code)) throw new Fault(
      "cannot synchronize fault code message"
      + Speak.quoteAnd("the key")
      + Speak.quoteCitation(code) + " exists as"
      + Speak.quoteExactTarget(codeRegister.get(code))
    );
    codeRegister.set(code, message);
  }

  public final int getFaultCode() {
    return code;
  }

  public final String getFaultMessage() {
    if (codeRegister.has(code)) return codeRegister.get(code);
    return codeRegister.get(noFaultCode);
  }

}
