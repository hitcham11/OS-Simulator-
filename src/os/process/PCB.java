package os.process;

public class PCB {

    public int processID;
    public String state;
    public int programCounter;
    public int memoryLowerBound;
    public int memoryUpperBound;

    public int burstTime;
    public int waitingTime;
    public int mlfqLevel;

    public PCB(int id, int lower, int upper, int burstTime) {
        this.processID        = id;
        this.state            = "READY";
        this.programCounter   = 0;
        this.memoryLowerBound = lower;
        this.memoryUpperBound = upper;
        this.burstTime        = burstTime;
        this.waitingTime      = 0;
        this.mlfqLevel        = 0;
    }

    // make sure bounds are valid
    public boolean isInMemory() {
        return memoryLowerBound != -1 && memoryUpperBound != -1;
    }
}
