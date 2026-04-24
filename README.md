# OS Simulator

An academic operating systems simulator written in Java. The project models process loading, memory allocation, swapping, mutex-based blocking, and multiple CPU scheduling algorithms.

## Features

- CPU scheduling:
  - `HRRN` and `RR`
  - `MLFQ` with 4 priority queues
- Contiguous memory allocation
- Swapping processes to and from disk when memory is full
- Simple process control blocks stored in simulated memory
- Mutex support for blocking and unblocking processes
- Basic system calls for:
  - printing output
  - reading and writing files
  - reading user input
  - reading and writing process variables in simulated memory

## Project Layout

- `src/os/Main.java` - simulation entry point
- `src/os/scheduler/` - scheduling algorithms and queue logic
- `src/os/memory/` - simulated memory and swap support
- `src/os/mutex/` - mutex and blocked-process handling
- `src/os/interpreter/` - program loading and instruction execution
- `src/os/syscall/` - file, input, output, and memory-related system calls
- `Program1.txt`, `Program2.txt`, `Program3.txt` - sample programs loaded by the simulator
- `GUC_OS_Simulator_Handbook_Windows_XP.pdf` - project handbook

## Requirements

- Java 8 or later
- A terminal or IDE such as Eclipse

## How to Run

### Option 1: Run from the command line

From the project root:

```bash
javac -d bin -sourcepath src src/os/Main.java
java -cp bin os.Main
```

To choose a scheduler:

```bash
java -cp bin os.Main HRRN
java -cp bin os.Main RR q=2
java -cp bin os.Main MLFQ
```

### Option 2: Run from Eclipse

1. Import the project as an existing Java project.
2. Make sure the working directory is the project root so the sample `Program*.txt` files can be found.
3. Run `os.Main`.

## Scheduler Options

- `HRRN` - Highest Response Ratio Next
- `RR` - Round Robin
  - Optional quantum argument: `q=<number>`
- `MLFQ` - Multi-Level Feedback Queue

If no argument is provided, the simulator defaults to `HRRN`.

## Notes

- The simulator expects the sample program text files to be present in the working directory.
- During execution, the simulator may create swap-related disk images in the project folder.
- The included `bin/` directory contains compiled classes from the original project, but it is not required if you rebuild locally.

## License

No license was provided with the original project.
