package os.process;

public class Process {

    public PCB pcb;

    // Arrival time (used by scheduler)
    public int arrivalTime;

    // Number of instructions
    public int codeSize;

    // Total memory size (code + vars + PCB)
    public int processSize;

    public Process(int id, int arrivalTime, int lower, int upper, int burstTime, int codeSize) {

        this.pcb = new PCB(id, lower, upper, burstTime);

        this.arrivalTime = arrivalTime;
        this.codeSize    = codeSize;

        // Total allocated memory size
        this.processSize = upper - lower + 1;
    }
}