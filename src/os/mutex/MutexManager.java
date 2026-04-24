package os.mutex;

import os.memory.Memory;
import os.process.Process;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MutexManager {

    private static final Mutex userInput = new Mutex("userInput");
    private static final Mutex userOutput = new Mutex("userOutput");
    private static final Mutex file = new Mutex("file");

    private static final Queue<Process> generalBlockedQueue = new LinkedList<>();
    private static final Queue<Process> recentlyUnblocked = new LinkedList<>();

    public static void semWait(String resource, Process process) {
        Mutex target = getMutex(resource);

        if (target == null) {
            System.err.println("[ERROR] Invalid resource: " + resource);
            return;
        }

        if (target.isLocked && target.owner == process) {
            return;
        }

        if (!target.isLocked) {
            target.isLocked = true;
            target.owner = process;
            System.out.println("[MUTEX] P" + process.pcb.processID + " acquired " + resource);
            return;
        }

        if (!target.blockedQueue.contains(process)) {
            target.blockedQueue.offer(process);
        }

        if (!generalBlockedQueue.contains(process)) {
            generalBlockedQueue.offer(process);
        }

        process.pcb.state = "BLOCKED";
        Memory.getInstance().syncPCB(process);
        System.out.println("[MUTEX] P" + process.pcb.processID + " blocked on " + resource);
    }

    public static void semSignal(String resource, Process process) {
        Mutex target = getMutex(resource);

        if (target == null || target.owner != process) {
            return;
        }

        System.out.println("[MUTEX] P" + process.pcb.processID + " released " + resource);

        if (!target.blockedQueue.isEmpty()) {
            Process next = target.blockedQueue.poll();
            generalBlockedQueue.remove(next);

            target.isLocked = true;
            target.owner = next;

            next.pcb.state = "READY";
            Memory.getInstance().syncPCB(next);
            recentlyUnblocked.offer(next);

            System.out.println("[MUTEX] P" + next.pcb.processID + " unblocked and acquired " + resource);
        } else {
            target.isLocked = false;
            target.owner = null;
        }
    }

    public static List<Process> drainRecentlyUnblocked() {
        List<Process> processes = new ArrayList<>();

        while (!recentlyUnblocked.isEmpty()) {
            processes.add(recentlyUnblocked.poll());
        }

        return processes;
    }

    public static Queue<Process> getGeneralBlockedQueue() {
        return new LinkedList<>(generalBlockedQueue);
    }

    public static Queue<Process> getBlockedQueue(String resource) {
        Mutex target = getMutex(resource);
        return target == null ? new LinkedList<>() : new LinkedList<>(target.blockedQueue);
    }

    public static void removeProcessFromQueues(Process process) {
        generalBlockedQueue.remove(process);
        recentlyUnblocked.remove(process);

        for (Mutex mutex : new Mutex[]{userInput, userOutput, file}) {
            mutex.blockedQueue.remove(process);
            if (mutex.owner == process) {
                mutex.owner = null;
                mutex.isLocked = false;
            }
        }
    }

    public static void reset() {
        generalBlockedQueue.clear();
        recentlyUnblocked.clear();

        for (Mutex mutex : new Mutex[]{userInput, userOutput, file}) {
            mutex.isLocked = false;
            mutex.owner = null;
            mutex.blockedQueue.clear();
        }
    }

    private static Mutex getMutex(String name) {
        if ("userInput".equals(name)) {
            return userInput;
        }
        if ("userOutput".equals(name)) {
            return userOutput;
        }
        if ("file".equals(name)) {
            return file;
        }
        return null;
    }
}
