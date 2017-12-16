package XPR;

import java.util.HashMap;

import static XPR.Math.getRandomInteger;

public class Kiosk<KTYPE, VTYPE> {

  public enum Permission {
    OVERWRITE_KEY,
    ADD_KEY, ADD_NULL_KEY,
    UNHANDLED_REQUESTS,
    QUERY
  }

  public static interface Supervisor<KTYPE, VTYPE> {
    boolean allow(Permission permission, KTYPE id);
    void onAdd(Kiosk kiosk, KTYPE id, VTYPE value);
    void onRemove(Kiosk kiosk, KTYPE id, VTYPE value, boolean transfer);
  }

  private HashMap<KTYPE, VTYPE> kiosk;
  private int size = 16;
  public final String type;
  private final Supervisor<KTYPE, VTYPE> kioskSupervisor;

  public Kiosk(String name) {this(name, null);}

  public Kiosk(Supervisor<KTYPE, VTYPE> tracker) {
    this("item", tracker);
  }

  private static final Supervisor NULL_MANAGER = new Supervisor() {
    @Override
    public boolean allow(Permission permission, Object id) { return true; }
    @Override
    public void onAdd(Kiosk kiosk, Object id, Object value) {}
    @Override
    public void onRemove
      (Kiosk kiosk, Object id, Object value, boolean transfer) {}
  };

  public Kiosk(String type, Supervisor<KTYPE, VTYPE> kioskSupervisor) {
    this.type = type;
    kiosk = new HashMap<>(size);
    this.kioskSupervisor = kioskSupervisor == null ? NULL_MANAGER : kioskSupervisor;
  }

  private <ANY> ANY halt(String type, boolean clear) {
    if (kioskSupervisor.allow(Permission.UNHANDLED_REQUESTS, (KTYPE)(Integer)0)) return null;
    if (clear) {kiosk.clear();}
    throw new Fault(this.getClass().getSimpleName(),
      new IllegalAccessException(type
        + (clear ? Speak.quoteAnd("all known entities have been cleared"):"")
        + Speak.quoteAnd("please correct your memory access routines")
      ));
  }

  final public boolean has(KTYPE key) {
    if (kioskSupervisor.allow(Permission.QUERY, key))
      return kiosk.containsKey(key);
    return false;
  }

  final public Integer newKioskID() {
    Integer id = getRandomInteger(1024, Integer.MAX_VALUE);
    for (;kiosk.containsKey(id);) id = getRandomInteger(1024, Integer.MAX_VALUE);
    return id;
  }

  final public <ANY> ANY get(KTYPE id) {
    VTYPE unit = kiosk.get(id);
    if (unit == null) return halt("request for unknown "+ type, true);
    return (ANY) unit;
  }

  final public <ANY> ANY set(KTYPE id, VTYPE value) {
    VTYPE unit = kiosk.get(id);
    if (unit == null) {
      if (kioskSupervisor.allow(Permission.ADD_KEY, id)) {
        if (value == null && ! kioskSupervisor.allow(Permission.ADD_NULL_KEY, (KTYPE)id)) {
          return halt("attempting to store null " + type, false);
        }
        kiosk.put(id, value);
        kioskSupervisor.onAdd(this, id, value);
        return (ANY) id;
      }
    } else if (kioskSupervisor.allow(Permission.OVERWRITE_KEY, id)) {
      kiosk.put(id, value);
      return (ANY) id;
    }
    return halt("attempting to set unknown "+ type + " key", true);
  }

  final public <ANY> ANY add(VTYPE value) {
    Integer id = newKioskID();
    if (value == null && ! kioskSupervisor.allow(Permission.ADD_NULL_KEY, (KTYPE)id))
      return halt("attempting to store null "+ type, false);
    kiosk.put((KTYPE) id, value);
    if (loadFactor() > 0.70) resizeStorage((int) size * 2);
    kioskSupervisor.onAdd(this, (KTYPE) id, value);
    return (ANY) id;
  }

  final public void free(KTYPE id) {
    VTYPE free = kiosk.remove(id);
    if (free == null) return;
    long length = length();
    boolean
      lessThan25PercentLoad = loadFactor() < 0.25,
      moreThan16SlotsFree = size - length > 16;
    if (lessThan25PercentLoad && moreThan16SlotsFree) { resizeStorage((int) length + 16); }
    kioskSupervisor.onRemove(this, id, free, false);
  }

  private final void resizeStorage(int size) {
    HashMap out = new HashMap<KTYPE, VTYPE>(size);
    this.size = size;
    out.putAll(kiosk);
    kiosk.clear();
    kiosk = out;
  }

  public void clear() {
    kiosk.clear();
    kioskSupervisor.onRemove(null, null, null, false);
  }

  final public <ANY> ANY transfer(KTYPE id) throws IllegalAccessException {
    VTYPE unit = kiosk.remove(id);
    if (unit == null) {
      return halt("attempting to transfer unknown "+ type, true);
    }
    kioskSupervisor.onRemove(this, id, unit, true);
    return (ANY) unit;
  }

  final public long size() { return size; }
  final public long length() { return kiosk.size(); }
  final public double loadFactor() {
    double units = size;
    return  (size / length());
  }

}
