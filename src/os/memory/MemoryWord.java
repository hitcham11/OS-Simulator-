package os.memory;

public class MemoryWord {
    private String key;
    private String value;

    public MemoryWord() {
        this.key   = null;
        this.value = null;
    }

    public String getKey()   { return key;   }
    public String getValue() { return value; }
    public void setKey(String key)     { this.key   = key;   }
    public void setValue(String value) { this.value = value; }
    public boolean isFree() { return key == null; }

    public void clear() {
        this.key   = null;
        this.value = null;
    }

    @Override
    public String toString() {
        return isFree() ? "[ EMPTY ]" : "[" + key + ": " + value + "]";
    }
}