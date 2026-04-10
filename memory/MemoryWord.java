package memory;

public class MemoryWord {
    public String name;
    public String value;

    public MemoryWord() {
        this.name = null;
        this.value = null;
    }

    public MemoryWord(String name, String value) {
        this.name = name;
        this.value = value;
    }

    // removed the getters and setters since this is spaghetti code and we access fields directly

    public void clear() {
        this.name = null;
        this.value = null;
    }

    // 7elw 3ashan el testing
    @Override
    public String toString() {
        return "[" + name + " : " + value + "]";
    }
}
