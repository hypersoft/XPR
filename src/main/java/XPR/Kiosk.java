package XPR;

import com.sun.istack.internal.NotNull;

import java.util.HashMap;

import static XPR.Plus.valueOf;

public class Kiosk {

  public enum Operation {
    ADD_KEY, DELETE_KEY, GET_KEY, SET_KEY, CHECK_KEY, TRANSFER_KEY, CHECK_LENGTH, LIST_KEYS
  }

  public static class Supervisor {

    final static Supervisor nullSuperVisor = new Supervisor();

    public boolean permit(Operation operation, Object key) {
      return true;
    }
    protected void onAdded(Object key, Object value) {}
    protected void onRemoved(Object key, Object value, boolean transfer) {}

  }

  public static abstract class Storage<KTYPE, VTYPE> {

    public abstract VTYPE get(KTYPE key);
    public abstract void set(KTYPE key, VTYPE value);
    public abstract KTYPE add(VTYPE value);
    public abstract void delete(KTYPE key);
    public abstract VTYPE transfer(KTYPE key);
    public abstract KTYPE[] listKeys();
    public abstract boolean exists(KTYPE key);
    public abstract int length();

    public static class Type {

      public static class RandomPointerMap<VTYPE> extends Storage<Integer, VTYPE>{
        protected HashMap<Integer, Object> store = new HashMap<>();
        private Integer generateKey() {
          Integer key; while (store.containsKey(
            key = Math.getRandomInteger(1024, Integer.MAX_VALUE)
          ));
          return key;
        }
        @Override
        public VTYPE get(Integer key) { return valueOf(store.get(key)); }
        @Override
        public void set(Integer key, VTYPE value) { store.put(key, value); }
        @Override
        public Integer add(VTYPE value) {
          Integer key = generateKey(); store.put(key, value);
          return key;
        }
        @Override
        public void delete(Integer key) { store.remove(key); }
        @Override
        public VTYPE transfer(Integer key) { return valueOf(store.remove(key)); }
        @Override
        public Integer[] listKeys() { return new Integer[0]; }
        @Override
        public boolean exists(Integer key) { return store.containsKey(key); }
        @Override
        public int length() { return store.size(); }

      }
    }

  }

  private final Storage<Integer, Object> kStorage;
  private final Supervisor kSupervisor;

  public Kiosk() {this(Supervisor.nullSuperVisor);}

  public Kiosk(@NotNull Supervisor supervisor) {
    this(supervisor, new Storage.Type.RandomPointerMap<>());
  }

  public Kiosk(@NotNull Supervisor supervisor, @NotNull Storage storage) {
    kSupervisor = supervisor;
    kStorage = storage;
  }

  public <ANY> ANY get(Object key) {
    if (kSupervisor.permit(Operation.GET_KEY, valueOf(key)))
      return valueOf(kStorage.get(valueOf(key)));
    throw new Fault(new IllegalAccessError());
  }

  public void set(Object key, Object value) {
    if (kSupervisor.permit(Operation.SET_KEY, valueOf(key))) {
      kStorage.set(valueOf(key), value);
      kSupervisor.onAdded(key, value);
    }
    else throw new Fault(new IllegalAccessError());
  }

  public Integer add(Object value) {
    if (kSupervisor.permit(Operation.ADD_KEY, null))
      return kStorage.add(value);
    throw new Fault(new IllegalAccessError());
  }

  public void delete(Object key) {
    if (kSupervisor.permit(Operation.DELETE_KEY, valueOf(key))) {
      Object value = kStorage.transfer(valueOf(key));
      kSupervisor.onRemoved(key, value, false);
    }
    else throw new Fault(new IllegalAccessError());
  }

  public <ANY> ANY transfer(Object key) {
    if (kSupervisor.permit(Operation.TRANSFER_KEY, valueOf(key))) {
      Object value = kStorage.transfer(valueOf(key));
      kSupervisor.onRemoved(key, value, true);
      return valueOf(value);
    }
    throw new Fault(new IllegalAccessError());
  }

  public <ANY> ANY[] listKeys() {
    if (kSupervisor.permit(Operation.LIST_KEYS, null))
      return valueOf(kStorage.listKeys());
    throw new Fault(new IllegalAccessError());
  }

  public boolean existingKey(Object key) {
    if (kSupervisor.permit(Operation.CHECK_KEY, valueOf(key)))
      return kStorage.exists(valueOf(key));
    throw new Fault(new IllegalAccessError());
  }

  public int length() {
    if (kSupervisor.permit(Operation.CHECK_LENGTH, null))
      return kStorage.length();
    throw new Fault(new IllegalAccessError());
  }

}
