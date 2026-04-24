package os.interpreter;

import os.memory.Memory;
import os.mutex.MutexManager;
import os.process.Process;
import os.syscall.SystemCalls;

public class Interpreter {

    public static void executeNext(Process process) {

        if (!process.pcb.isInMemory()) {
            return;
        }

        if (process.pcb.programCounter >= process.codeSize) {
            process.pcb.state = "FINISHED";
            Memory.getInstance().syncPCB(process);
            return;
        }

        String instruction = Memory.getInstance().fetchInstruction(
                process.pcb.programCounter,
                process.pcb.memoryLowerBound,
                process.pcb.memoryUpperBound,
                process.codeSize
        );

        if (instruction == null) {
            process.pcb.state = "FINISHED";
            Memory.getInstance().syncPCB(process);
            return;
        }

        String[] parts = instruction.trim().split("\\s+");
        String command = parts[0];

        System.out.println("[RUNNING] P" + process.pcb.processID);
        System.out.println("[INSTRUCTION] " + instruction);

        switch (command) {
            case "print":
                if (parts.length > 1) {
                    SystemCalls.print(resolve(process, parts[1]));
                }
                break;

            case "assign":
                executeAssign(process, parts);
                break;

            case "writeFile":
                if (parts.length > 2) {
                    SystemCalls.writeFile(resolve(process, parts[1]), resolve(process, parts[2]));
                }
                break;

            case "readFile":
                if (parts.length > 1) {
                    SystemCalls.print(SystemCalls.readFile(resolve(process, parts[1])));
                }
                break;

            case "printFromTo":
                executePrintFromTo(process, parts);
                break;

            case "semWait":
                if (parts.length > 1) {
                    MutexManager.semWait(parts[1], process);
                    if ("BLOCKED".equals(process.pcb.state)) {
                        Memory.getInstance().syncPCB(process);
                        return;
                    }
                }
                break;

            case "semSignal":
                if (parts.length > 1) {
                    MutexManager.semSignal(parts[1], process);
                }
                break;

            default:
                System.err.println("[ERROR] Unknown instruction: " + instruction);
        }

        if (!"BLOCKED".equals(process.pcb.state)) {
            process.pcb.programCounter++;

            if (process.pcb.programCounter >= process.codeSize) {
                process.pcb.state = "FINISHED";
            }

            if (process.pcb.burstTime > 0) {
                process.pcb.burstTime--;
            }

            Memory.getInstance().syncPCB(process);
        }
    }

    private static void executeAssign(Process process, String[] parts) {
        if (parts.length < 3) {
            return;
        }

        String value;

        if ("input".equals(parts[2])) {
            value = SystemCalls.takeInput();
        } else if ("readFile".equals(parts[2]) && parts.length > 3) {
            value = SystemCalls.readFile(resolve(process, parts[3]));
        } else {
            value = resolve(process, parts[2]);
        }

        SystemCalls.writeToMemory(process, parts[1], value);
    }

    private static void executePrintFromTo(Process process, String[] parts) {
        if (parts.length < 3) {
            return;
        }

        try {
            int from = Integer.parseInt(resolve(process, parts[1]));
            int to = Integer.parseInt(resolve(process, parts[2]));

            int start = Math.min(from, to);
            int end = Math.max(from, to);

            for (int i = start + 1; i < end; i++) {
                SystemCalls.print(String.valueOf(i));
            }
        } catch (NumberFormatException exception) {
            System.err.println("[ERROR] Invalid printFromTo range.");
        }
    }

    private static String resolve(Process process, String token) {
        String value = SystemCalls.readFromMemory(process, token);
        return value == null ? token : value;
    }
}
