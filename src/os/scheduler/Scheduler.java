package os.scheduler;

import os.memory.Memory;
import os.process.Process;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class Scheduler {

    public static final int MLFQ_LEVELS = 4;

    public enum Algorithm {
        HRRN,
        RR,
        MLFQ
    }

    public static void updateWaitingTimes(List<Process> allProcesses, Process activeProcess) {
        for (Process process : allProcesses) {
            if ("READY".equals(process.pcb.state) && process != activeProcess) {
                process.pcb.waitingTime++;
            }
        }
    }

    public static Process getNextHRRN(List<Process> allProcesses) {
        Process bestProcess = null;
        double highestRatio = -1;

        System.out.println("[SCHEDULER] HRRN ratios:");

        for (Process process : allProcesses) {
            if (!"READY".equals(process.pcb.state)) {
                continue;
            }

            int waitingTime = process.pcb.waitingTime;
            int burstTime = Math.max(process.pcb.burstTime, 1);
            double ratio = (double) (waitingTime + burstTime) / burstTime;

            System.out.printf("   P%d -> (W:%d + S:%d) / S:%d = %.3f%n",
                    process.pcb.processID, waitingTime, burstTime, burstTime, ratio);

            if (ratio > highestRatio
                    || (ratio == highestRatio && bestProcess != null
                    && process.pcb.processID < bestProcess.pcb.processID)) {
                bestProcess = process;
                highestRatio = ratio;
            }
        }

        return bestProcess;
    }

    public static Process getNextRR(Queue<Process> readyQueue) {
        while (!readyQueue.isEmpty()) {
            Process process = readyQueue.poll();
            if (process != null && "READY".equals(process.pcb.state)) {
                return process;
            }
        }
        return null;
    }

    public static List<Queue<Process>> createMLFQQueues() {
        List<Queue<Process>> queues = new ArrayList<Queue<Process>>();
        for (int i = 0; i < MLFQ_LEVELS; i++) {
            queues.add(new ArrayDeque<Process>());
        }
        return queues;
    }

    public static Process getNextMLFQ(List<Queue<Process>> queues) {
        for (int level = 0; level < queues.size(); level++) {
            Queue<Process> queue = queues.get(level);
            while (!queue.isEmpty()) {
                Process process = queue.poll();
                if (process != null && "READY".equals(process.pcb.state)) {
                    process.pcb.mlfqLevel = level;
                    return process;
                }
            }
        }
        return null;
    }

    public static void enqueueMLFQ(Process process, List<Queue<Process>> queues, int level) {
        if (process == null || !"READY".equals(process.pcb.state)) {
            return;
        }

        int safeLevel = Math.max(0, Math.min(level, queues.size() - 1));
        process.pcb.mlfqLevel = safeLevel;

        Queue<Process> queue = queues.get(safeLevel);
        if (!queue.contains(process)) {
            queue.offer(process);
        }
    }

    public static boolean hasHigherPriorityReadyProcess(List<Queue<Process>> queues, int currentLevel) {
        for (int level = 0; level < Math.max(0, currentLevel); level++) {
            Queue<Process> queue = queues.get(level);
            for (Process process : queue) {
                if ("READY".equals(process.pcb.state)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int getMLFQQuantum(int level) {
        return 1 << Math.max(0, level);
    }

    public static Process selectProcessToSwap(List<Process> allProcesses, Process excludedProcess, int neededSize) {
        Process readyFallback = null;
        Process blockedFallback = null;

        for (Process process : allProcesses) {
            if (process == excludedProcess) {
                continue;
            }

            if (!process.pcb.isInMemory()) {
                continue;
            }

            boolean idealFit = Memory.getInstance().canAllocateIfFreed(
                    neededSize,
                    process.pcb.memoryLowerBound,
                    process.pcb.memoryUpperBound
            );

            if ("READY".equals(process.pcb.state)) {
                if (idealFit) {
                    return process;
                }
                if (readyFallback == null) {
                    readyFallback = process;
                }
            }

            if ("BLOCKED".equals(process.pcb.state)) {
                if (idealFit) {
                    return process;
                }
                if (blockedFallback == null) {
                    blockedFallback = process;
                }
            }
        }

        return readyFallback != null ? readyFallback : blockedFallback;
    }

    public static Algorithm resolveAlgorithm(String[] args, Algorithm defaultAlgorithm) {
        if (args == null || args.length == 0) {
            return defaultAlgorithm;
        }

        for (String arg : args) {
            if ("RR".equalsIgnoreCase(arg)) {
                return Algorithm.RR;
            }
            if ("MLFQ".equalsIgnoreCase(arg)) {
                return Algorithm.MLFQ;
            }
            if ("HRRN".equalsIgnoreCase(arg)) {
                return Algorithm.HRRN;
            }
        }

        return defaultAlgorithm;
    }

    public static int resolveQuantum(String[] args, int defaultQuantum) {
        if (args == null) {
            return defaultQuantum;
        }

        for (String arg : args) {
            if (arg != null && arg.startsWith("q=")) {
                try {
                    int parsed = Integer.parseInt(arg.substring(2));
                    return parsed > 0 ? parsed : defaultQuantum;
                } catch (NumberFormatException ignored) {
                    return defaultQuantum;
                }
            }
        }

        return defaultQuantum;
    }
}
