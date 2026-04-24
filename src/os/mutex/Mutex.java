package os.mutex;
import os.process.Process;
import java.util.LinkedList;
import java.util.Queue;

public class Mutex {
    public String resourceName;
    public boolean isLocked;
    public Process owner;
    public Queue<Process> blockedQueue;

    public Mutex(String name) {
        this.resourceName = name;
        this.isLocked = false;
        this.owner = null;
        this.blockedQueue = new LinkedList<>();
    }
}