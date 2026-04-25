package os;

import os.interpreter.Interpreter;
import os.interpreter.ProgramLoader;
import os.memory.Memory;
import os.mutex.MutexManager;
import os.process.Process;
import os.scheduler.Scheduler;
import os.syscall.SystemCalls;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

public class Main {

    private static final int MAX_CLOCK_CYCLES = 100;
    private static final int DEFAULT_RR_QUANTUM = 2;
    private static final Scheduler.Algorithm DEFAULT_ALGORITHM = Scheduler.Algorithm.HRRN;

    private static final String[] PROGRAM_FILES = {"Program1.txt", "Program2.txt", "Program3.txt"};
    private static final int[] PROCESS_IDS = {1, 2, 3};
    private static final int[] ARRIVAL_TIMES = {0, 1, 4};

    public static void main(String[] args) {
        resetSimulationState();
        runSimulation(args);
    }

    public static void runWithScriptedInputs(String[] args, Collection<String> scriptedInputs) {
        resetSimulationState();
        SystemCalls.setScriptedInputs(scriptedInputs);
        try {
            runSimulation(args);
        } finally {
            SystemCalls.clearScriptedInputs();
        }
    }

    private static void runSimulation(String[] args) {

        Scheduler.Algorithm algorithm = Scheduler.resolveAlgorithm(args, DEFAULT_ALGORITHM);
        int rrQuantum = Scheduler.resolveQuantum(args, DEFAULT_RR_QUANTUM);

        int clock = 0;
        int timeSliceUsed = 0;

        List<Process> allProcesses = new ArrayList<>();
        Queue<Process> rrReadyQueue = new ArrayDeque<>();
        List<Queue<Process>> mlfqQueues = Scheduler.createMLFQQueues();
        Process activeProcess = null;

        System.out.println("===== OS SIMULATION START =====");
        if (algorithm == Scheduler.Algorithm.RR) {
            System.out.println("Scheduler: " + algorithm + " (quantum = " + rrQuantum + " instructions)");
        } else if (algorithm == Scheduler.Algorithm.MLFQ) {
            System.out.println("Scheduler: MLFQ (4 queues, quantum = 2^i, last queue = RR)");
        } else {
            System.out.println("Scheduler: " + algorithm);
        }
      //handling the clock
        while (clock < MAX_CLOCK_CYCLES) {

            System.out.println("\n>>> CLOCK: " + clock + " <<<");

            handleArrivals(clock, allProcesses, rrReadyQueue, mlfqQueues, algorithm, activeProcess);
            appendUnblockedProcesses(rrReadyQueue, mlfqQueues, algorithm);

            if (algorithm == Scheduler.Algorithm.MLFQ
                    && activeProcess != null
                    && Scheduler.hasHigherPriorityReadyProcess(mlfqQueues, activeProcess.pcb.mlfqLevel)) {
                setProcessState(activeProcess, "READY");
                Scheduler.enqueueMLFQ(activeProcess, mlfqQueues, activeProcess.pcb.mlfqLevel);
                printSchedulingEvent("Preempted By Higher Queue", allProcesses, rrReadyQueue, mlfqQueues, null, algorithm);
                activeProcess = null;
                timeSliceUsed = 0;
            }

            if (activeProcess == null) {
                activeProcess = dispatchNextProcess(allProcesses, rrReadyQueue, mlfqQueues, algorithm);

                if (activeProcess != null) {
                    timeSliceUsed = 0;

                    if (!ensureProcessInMemory(activeProcess, allProcesses, rrReadyQueue, mlfqQueues, null)) {
                        setProcessState(activeProcess, "READY");
                        if (algorithm == Scheduler.Algorithm.RR) {
                            enqueueIfAbsent(rrReadyQueue, activeProcess);
                        } else if (algorithm == Scheduler.Algorithm.MLFQ) {
                            Scheduler.enqueueMLFQ(activeProcess, mlfqQueues, activeProcess.pcb.mlfqLevel);
                        }
                        activeProcess = null;
                    } else {
                        printSchedulingEvent("Chosen", allProcesses, rrReadyQueue, mlfqQueues, activeProcess, algorithm);
                    }
                }
            }

            if (activeProcess != null) {
                Interpreter.executeNext(activeProcess);

                if ("BLOCKED".equals(activeProcess.pcb.state)) {
                    printSchedulingEvent("Blocked", allProcesses, rrReadyQueue, mlfqQueues, null, algorithm);
                    activeProcess = null;
                    timeSliceUsed = 0;
                } else if (isFinished(activeProcess)) {
                    finishProcess(activeProcess, allProcesses, rrReadyQueue);
                    printSchedulingEvent("Finished", allProcesses, rrReadyQueue, mlfqQueues, null, algorithm);
                    activeProcess = null;
                    timeSliceUsed = 0;
                } else {
                    timeSliceUsed++;

                    if (algorithm == Scheduler.Algorithm.RR && timeSliceUsed >= rrQuantum) {
                        setProcessState(activeProcess, "READY");
                        enqueueIfAbsent(rrReadyQueue, activeProcess);
                        printSchedulingEvent("Time Slice Expired", allProcesses, rrReadyQueue, mlfqQueues, null, algorithm);
                        activeProcess = null;
                        timeSliceUsed = 0;
                    } else if (algorithm == Scheduler.Algorithm.MLFQ) {
                        int levelQuantum = Scheduler.getMLFQQuantum(activeProcess.pcb.mlfqLevel);
                        if (timeSliceUsed >= levelQuantum) {
                            setProcessState(activeProcess, "READY");
                            int nextLevel = Math.min(activeProcess.pcb.mlfqLevel + 1, Scheduler.MLFQ_LEVELS - 1);
                            Scheduler.enqueueMLFQ(activeProcess, mlfqQueues, nextLevel);
                            printSchedulingEvent("MLFQ Quantum Expired", allProcesses, rrReadyQueue, mlfqQueues, null, algorithm);
                            activeProcess = null;
                            timeSliceUsed = 0;
                        }
                    }
                }
            }

            appendUnblockedProcesses(rrReadyQueue, mlfqQueues, algorithm);
            printClockSnapshot(allProcesses, rrReadyQueue, mlfqQueues, activeProcess, algorithm);
            Memory.getInstance().printMemory();

            Scheduler.updateWaitingTimes(allProcesses, activeProcess);

            clock++;

            if (allProcesses.isEmpty() && noFutureArrivals(clock)) {
                break;
            }
        }

        System.out.println("\n===== SIMULATION COMPLETE =====");
    }

    private static void resetSimulationState() {
        Memory.getInstance().reset();
        MutexManager.reset();
        SystemCalls.clearScriptedInputs();
    }

    private static void handleArrivals(int clock,
                                       List<Process> allProcesses,
                                       Queue<Process> rrReadyQueue,
                                       List<Queue<Process>> mlfqQueues,
                                       Scheduler.Algorithm algorithm,
                                       Process activeProcess) {
        for (int i = 0; i < ARRIVAL_TIMES.length; i++) {
            if (ARRIVAL_TIMES[i] == clock) {
                Process process = loadProcess(
                        PROGRAM_FILES[i],
                        PROCESS_IDS[i],
                        ARRIVAL_TIMES[i],
                        allProcesses,
                        rrReadyQueue,
                        mlfqQueues,
                        activeProcess
                );

                if (process != null) {
                    allProcesses.add(process);
                    if (algorithm == Scheduler.Algorithm.RR) {
                        enqueueIfAbsent(rrReadyQueue, process);
                    } else if (algorithm == Scheduler.Algorithm.MLFQ) {
                        Scheduler.enqueueMLFQ(process, mlfqQueues, 0);
                    }
                    System.out.println("[ARRIVAL] P" + process.pcb.processID + " arrived at time " + clock);
                }
            }
        }
    }
  //loading process
    private static Process loadProcess(String fileName,
                                       int processId,
                                       int arrivalTime,
                                       List<Process> allProcesses,
                                       Queue<Process> rrReadyQueue,
                                       List<Queue<Process>> mlfqQueues,
                                       Process activeProcess) {

        Process process = ProgramLoader.load(fileName, processId, arrivalTime);
        int requiredSize = ProgramLoader.estimateProcessSize(fileName);

        if (requiredSize == -1) {
            return null;
        }

        while (process == null) {
            Process victim = Scheduler.selectProcessToSwap(allProcesses, activeProcess, requiredSize);

            if (victim == null) {
                System.out.println("[SYSTEM] Unable to free space for P" + processId);
                return null;
            }

            swapOutProcess(victim, rrReadyQueue, mlfqQueues);
            process = ProgramLoader.load(fileName, processId, arrivalTime);
        }

        setProcessState(process, "READY");
        process.pcb.mlfqLevel = 0;
        return process;
    }
  //el ba3do
    private static Process dispatchNextProcess(List<Process> allProcesses,
                                               Queue<Process> rrReadyQueue,
                                               List<Queue<Process>> mlfqQueues,
                                               Scheduler.Algorithm algorithm) {

        Process next;
        if (algorithm == Scheduler.Algorithm.RR) {
            next = Scheduler.getNextRR(rrReadyQueue);
        } else if (algorithm == Scheduler.Algorithm.MLFQ) {
            next = Scheduler.getNextMLFQ(mlfqQueues);
        } else {
            next = Scheduler.getNextHRRN(allProcesses);
        }

        if (next != null) {
            setProcessState(next, "RUNNING");
        }

        return next;
    }

    private static boolean ensureProcessInMemory(Process target,
                                                 List<Process> allProcesses,
                                                 Queue<Process> rrReadyQueue,
                                                 List<Queue<Process>> mlfqQueues,
                                                 Process excluded) {

        if (target.pcb.isInMemory()) {
            return true;
        }

        while (true) {
            int newLower = Memory.getInstance().swapFromDisk(target.pcb.processID, target.processSize);

            if (newLower != -1) {
                target.pcb.memoryLowerBound = newLower;
                target.pcb.memoryUpperBound = newLower + target.processSize - 1;
                Memory.getInstance().syncPCB(target);
                System.out.println("[SWAP] P" + target.pcb.processID + " swapped IN");
                Memory.getInstance().printDiskImage(target.pcb.processID);
                return true;
            }

            Process victim = Scheduler.selectProcessToSwap(allProcesses, excluded == null ? target : excluded, target.processSize);

            if (victim == null) {
                System.out.println("[SWAP] No process available to swap out for P" + target.pcb.processID);
                return false;
            }

            swapOutProcess(victim, rrReadyQueue, mlfqQueues);
        }
    }
  //switching
    private static void swapOutProcess(Process victim,
                                       Queue<Process> rrReadyQueue,
                                       List<Queue<Process>> mlfqQueues) {
        Memory.getInstance().swapToDisk(
                victim.pcb.processID,
                victim.pcb.memoryLowerBound,
                victim.pcb.memoryUpperBound
        );

        victim.pcb.memoryLowerBound = -1;
        victim.pcb.memoryUpperBound = -1;
        setProcessState(victim, victim.pcb.state);
        removeFromMLFQQueues(victim, mlfqQueues);
        if ("READY".equals(victim.pcb.state)) {
            Scheduler.enqueueMLFQ(victim, mlfqQueues, victim.pcb.mlfqLevel);
        }

        System.out.println("[SWAP] P" + victim.pcb.processID + " swapped OUT");
        Memory.getInstance().printDiskImage(victim.pcb.processID);
    }

    private static boolean isFinished(Process process) {
        return "FINISHED".equals(process.pcb.state) || process.pcb.programCounter >= process.codeSize;
    }

    private static void finishProcess(Process process,
                                      List<Process> allProcesses,
                                      Queue<Process> rrReadyQueue) {

        setProcessState(process, "FINISHED");

        if (process.pcb.isInMemory()) {
            Memory.getInstance().free(
                    process.pcb.processID,
                    process.pcb.memoryLowerBound,
                    process.pcb.memoryUpperBound
            );
        }

        allProcesses.remove(process);
        rrReadyQueue.remove(process);
        MutexManager.removeProcessFromQueues(process);

        System.out.println("[SYSTEM] P" + process.pcb.processID + " finished.");
    }

    private static void appendUnblockedProcesses(Queue<Process> rrReadyQueue,
                                                 List<Queue<Process>> mlfqQueues,
                                                 Scheduler.Algorithm algorithm) {
        for (Process process : MutexManager.drainRecentlyUnblocked()) {
            if (algorithm == Scheduler.Algorithm.RR) {
                enqueueIfAbsent(rrReadyQueue, process);
            } else if (algorithm == Scheduler.Algorithm.MLFQ) {
                Scheduler.enqueueMLFQ(process, mlfqQueues, process.pcb.mlfqLevel);
            }
        }
    }

    private static void enqueueIfAbsent(Queue<Process> queue, Process process) {
        if (process != null && "READY".equals(process.pcb.state) && !queue.contains(process)) {
            queue.offer(process);
        }
    }

    private static void setProcessState(Process process, String state) {
        process.pcb.state = state;
        Memory.getInstance().syncPCB(process);
    }

    private static boolean noFutureArrivals(int nextClock) {
        for (int arrivalTime : ARRIVAL_TIMES) {
            if (arrivalTime >= nextClock) {
                return false;
            }
        }
        return true;
    }

    private static void removeFromMLFQQueues(Process process, List<Queue<Process>> mlfqQueues) {
        for (Queue<Process> queue : mlfqQueues) {
            queue.remove(process);
        }
    }
//printing
    private static void printSchedulingEvent(String event,
                                             List<Process> allProcesses,
                                             Queue<Process> rrReadyQueue,
                                             List<Queue<Process>> mlfqQueues,
                                             Process activeProcess,
                                             Scheduler.Algorithm algorithm) {
        System.out.println("\n[EVENT] " + event);
        printQueues(allProcesses, rrReadyQueue, mlfqQueues, activeProcess, algorithm);
    }

    private static void printClockSnapshot(List<Process> allProcesses,
                                           Queue<Process> rrReadyQueue,
                                           List<Queue<Process>> mlfqQueues,
                                           Process activeProcess,
                                           Scheduler.Algorithm algorithm) {
        System.out.println("\n--- SYSTEM STATE ---");
        System.out.println("Scheduler Mode: " + algorithm);
        printQueues(allProcesses, rrReadyQueue, mlfqQueues, activeProcess, algorithm);
        System.out.println("----------------------");
    }

    private static void printQueues(List<Process> allProcesses,
                                    Queue<Process> rrReadyQueue,
                                    List<Queue<Process>> mlfqQueues,
                                    Process activeProcess,
                                    Scheduler.Algorithm algorithm) {

        System.out.println("READY Queue: " + formatReadyQueue(allProcesses, rrReadyQueue, mlfqQueues, algorithm));
        System.out.println("BLOCKED Queue: " + formatProcessCollection(MutexManager.getGeneralBlockedQueue()));
        System.out.println("RUNNING: " + (activeProcess == null ? "None" : "P" + activeProcess.pcb.processID));
        if (algorithm == Scheduler.Algorithm.MLFQ) {
            for (int i = 0; i < mlfqQueues.size(); i++) {
                System.out.println("RQ" + i + " (q=" + Scheduler.getMLFQQuantum(i) + "): " + formatProcessCollection(mlfqQueues.get(i)));
            }
        }
        System.out.println("FILE Blocked: " + formatProcessCollection(MutexManager.getBlockedQueue("file")));
        System.out.println("USER INPUT Blocked: " + formatProcessCollection(MutexManager.getBlockedQueue("userInput")));
        System.out.println("USER OUTPUT Blocked: " + formatProcessCollection(MutexManager.getBlockedQueue("userOutput")));
    }

    private static String formatReadyQueue(List<Process> allProcesses,
                                           Queue<Process> rrReadyQueue,
                                           List<Queue<Process>> mlfqQueues,
                                           Scheduler.Algorithm algorithm) {
        if (algorithm == Scheduler.Algorithm.RR) {
            return formatProcessCollection(rrReadyQueue);
        }
        if (algorithm == Scheduler.Algorithm.MLFQ) {
            List<Process> ready = new ArrayList<Process>();
            for (Queue<Process> queue : mlfqQueues) {
                for (Process process : queue) {
                    if ("READY".equals(process.pcb.state) && !ready.contains(process)) {
                        ready.add(process);
                    }
                }
            }
            return formatProcessCollection(ready);
        }

        List<Process> ready = new ArrayList<>();
        for (Process process : allProcesses) {
            if ("READY".equals(process.pcb.state)) {
                ready.add(process);
            }
        }
        ready.sort(Comparator.comparingInt(p -> p.pcb.processID));
        return formatProcessCollection(ready);
    }

    private static String formatProcessCollection(Iterable<Process> processes) {
        StringBuilder builder = new StringBuilder();

        for (Process process : processes) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append("P").append(process.pcb.processID);
        }

        return builder.length() == 0 ? "None" : builder.toString();
    }
}
