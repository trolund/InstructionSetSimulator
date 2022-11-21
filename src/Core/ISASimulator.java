package Core;

import IO.DataDumper;
import Instruction.R;
import Instruction.I;
import Instruction.U;
import Instruction.SB;
import Instruction.S;
import Instruction.UJ;
import Instruction.abstact.Instruction;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;

public class ISASimulator {

    private boolean printReg = false;
    private boolean debug = false;
    private int pc;
    private int[] reg;
    private int[] memory;
    private int currInstr;
    private int[] progr;
    private Instruction currInstrObj;
    private InstructionDecoder decoder;
    private DataDumper dataDumper;
    private boolean dumpData;
    private final TUIColors c;
    private int instrCount;
    private boolean forceEnd = false;
    private int exitCode = 0;
    private String programName;
    private int ecallAction = 17;
    private int ecallArg = 10;
    private boolean exitPrint = false;
    private int pcInit = 0;

    public ISASimulator(String programName, boolean printReg, boolean debug, boolean dumpData, int ecallAction, int ecallArg, boolean exitPrint) {
        this.programName = programName;
        this.printReg = printReg;
        this.debug = debug;
        this.dumpData = dumpData;
        this.c = new TUIColors();
        this.dataDumper = new DataDumper();
        this.ecallAction = ecallAction;
        this.ecallArg = ecallArg;
        this.exitPrint = exitPrint;
        // Reset machine and allocate space for reg and mem
        resetSim();
        startUpPrint();
    }

    public ISASimulator(String programName, boolean printReg, boolean debug, boolean dumpData) {
        this.programName = programName;
        this.printReg = printReg;
        this.debug = debug;
        this.dumpData = dumpData;
        this.c = new TUIColors();
        this.dataDumper = new DataDumper();
        // Reset machine and allocate space for reg and mem
        resetSim();
        startUpPrint();
    }

    public void startUpPrint(){
        // Start up print in terminal
        System.out.println(c.colorText("🛠 RISC-V Simulator started 🚀", TUIColors.BLUE_BACKGROUND));
        if (!programName.isEmpty()) System.out.println(c.colorText("🏃 Running program : " + programName, TUIColors.PURPLE_BACKGROUND));
    }

    public ISASimulator() {
        this.c = new TUIColors();
        resetSim();
    }

    private void resetSim() {
        this.pc = pcInit;
        this.memory = new int[0x100000]; // 1 mb // 0x100000
        this.reg = new int[32];
        this.reg[2] = 0; // SP = 0 at init
        this.decoder = new InstructionDecoder();
        this.instrCount = 0;
    }

    public int[] getReg() {
        return reg;
    }

    private void loadData(){
        for (int i = pcInit; i < this.progr.length * 4; i = i + 4) {
            // split into 4 times 8 bit
            int instr = progr[i >> 2];
            memory[i] = (instr & 0xFF);
            memory[i + 1] = ((instr >> 8) & 0xFF);
            memory[i + 2] = ((instr >> 16) & 0xFF);
            memory[i + 3] = ((instr >> 24) & 0xFF);
        }
    }

    private int loadInstruction(int i){
        return  memory[i] |  memory[i + 1] << 8 | memory[i + 2] << 16 | memory[i + 3] << 24;
    }

    private void fetchInstruction(){
        // fetch new instruction
        currInstr = loadInstruction(pc);
        // print instruction as hex if in debug mode
        if (debug) {
            String hex = Integer.toHexString(pc);
            System.out.println(c.colorText("PC Hex: (" + hex + "), PC: (" + (pc >> 2) + ") : " + Integer.toHexString(currInstr), TUIColors.YELLOW_UNDERLINED));
        }
    }

    private void empty(){
        if (debug && progr.length <= 0) {
            System.out.println(c.colorText("Empty program (∅ == 🪹)", TUIColors.YELLOW_BACKGROUND));
        }
    }

    public void runProgram(int[] progr) {
        this.progr = progr;
        empty(); // print if the program is empty - just for debugging
        loadData(); // load the program into memory


        while (true) {
            if(forceEnd){ break; }

            try {
                fetchInstruction(); // 1. read the instructions from the memory
                decodeInstr(currInstr); // 2. decode the instruction
                exeInstr(currInstrObj); // 3. executes the instruction
                instrCount++;
                reg[0] = 0; // keep 0x = zero
            } catch (Exception e) {
                e.printStackTrace();
                exit(99);
            }

            pc += 4; // One instruction is four bytes (32 bit) -> move program counter to next instruction 🛠

            if (this.printReg) {
                printRegState();
            }

            // program have hit the end.
            if ((pc >> 2) >= progr.length) {
                break;
            }
        }

        exitPrint();
        writeDump();
    }

    private void writeDump(){
        if(this.dumpData){
            try {
                this.dataDumper.writeFile(this.programName, this.reg);
                System.out.println(c.colorText("Data dump made", TUIColors.BLACK_BACKGROUND_BRIGHT));
            } catch (IOException e) {
                System.out.println(c.colorText("Data dump failed", TUIColors.RED_BACKGROUND));
                e.printStackTrace();
            }
        }
    }

    private void exit(int exitCode) {
        forceEnd = true;
        this.exitCode = exitCode;
    }
    private void exitPrint() {
        System.out.println(c.colorText("Program exit with code: " + exitCode, TUIColors.CYAN));
        if (exitPrint && !debug) printRegState();
        if(debug) System.out.println(c.colorText("Executed " + instrCount + " instructions", TUIColors.CYAN_BACKGROUND));
    }

    private void printRegState() {
            for (int i : reg) {
                System.out.print(c.colorText(i + " ", TUIColors.PURPLE_BACKGROUND));
            }
            System.out.println();
    }

    private void decodeInstr(int instr) throws Exception {
        Instruction i = decoder.process(instr, debug);
        if (debug) System.out.println("Opcode: " + i.opcode);
        this.currInstr = instr;
        this.currInstrObj = i;
    }

    private void exeInstr(Instruction i) throws Exception {
            // map the opcode to the right action's
            if(i instanceof R) processR((R) i);
            else if (i instanceof I) processI((I) i);
            else if (i instanceof U) processU((U) i);
            else if (i instanceof SB) processSB((SB) i);
            else if (i instanceof S) processS((S) i);
            else if (i instanceof UJ) processUJ((UJ) i);
            else System.out.println(c.colorText("Opcode " + i.opcode + " not yet implemented 🛠😤", TUIColors.RED));
    }

    private void processUJ(UJ i) {
        // jal instruction
        if (debug) System.out.println("jal");
        reg[i.rd] = pc + 4;
        pc = pc + i.imm - 4;
    }

    private void processS(S i) {
        //  sb instruction
        if((i.funct3 == 0x0)){
            if (debug) System.out.println("sb");
            // Store 8-bit, values from the low bits of register rs2 to memory.
            // imm = offset
            memory[reg[i.rs1] + i.imm] = reg[i.rs2] & 0xFF;
            return;
        }
        //  sh instruction
        if((i.funct3 == 0x1)){
            if (debug) System.out.println("sh");
            // Store 16-bit, values from the low bits of register rs2 to memory
            // imm = offset
            // split into 2 times 8 bit
            memory[reg[i.rs1] + i.imm] = (reg[i.rs2] & 0xFF);
            memory[reg[i.rs1] + i.imm + 1] = ((reg[i.rs2] >> 8) & 0xFF);
            return;
        }
        //  sw instruction
        if((i.funct3 == 0x2)){
            if (debug) System.out.println("sw");
            // Store 32-bit, values from the low bits of register rs2 to memory.
            // imm = offset
            // split into 4 times 8 bit
            memory[reg[i.rs1] + i.imm] = (reg[i.rs2] & 0xFF);
            memory[reg[i.rs1] + i.imm + 1] = ((reg[i.rs2] >> 8) & 0xFF);
            memory[reg[i.rs1] + i.imm + 2] = ((reg[i.rs2] >> 16) & 0xFF);
            memory[reg[i.rs1] + i.imm + 3] = ((reg[i.rs2] >> 24) & 0xFF);
        }

    }

    private void processU(U i) throws ExecutionControl.NotImplementedException {
        // lui
        if (i.opcode == 0x37) {
            if (debug) System.out.println("lui");
            reg[i.rd] = i.imm;
            return;
        }
        // auipc
        if (i.opcode == 0x17) {
            if (debug) System.out.println("auipc");
            reg[i.rd] = pc + i.imm;
            return;
        }

        throw new ExecutionControl.NotImplementedException(c.colorText("U-type instruction not implemented 🛠😤", TUIColors.RED));
    }

    private void processI(I i) throws Exception {
        // ecall https://github.com/mortbopet/Ripes/blob/master/docs/ecalls.md
        if (i.opcode == 0x73) {
            if (debug) System.out.println("ecall");
            int action = reg[ecallAction];
            int arg = reg[ecallArg];
            if (action == 1) {
                System.out.println(arg);
            } else if (action == 4) {
                int x = arg;
                int ch = memory[x];
                while(ch != 0x00){
                    System.out.println((char) ch);
                    ch = memory[++x];
                }
            } else if (action == 9) { // allocates a1 bytes on the heap, returns pointer to start in a7

            } else if (action == 10) {
                if (debug) System.out.println(c.colorText("ecall (exit): " + action, TUIColors.BLUE_BACKGROUND));
                exit(0);
            } else if (action == 11) {
                if (debug) System.out.println((char) arg);
            } else if (action == 17) {
                if (debug) System.out.println(c.colorText("ecall: " + action, TUIColors.BLUE_BACKGROUND));
                exit(arg);
            } else {
                if (debug) System.out.println(c.colorText("Invalid ecall: " + action, TUIColors.YELLOW_BACKGROUND));
            }
            return;
        }
        if (i.opcode == 0x67) {
            // Jalr instruction
            // t =pc+4; pc=(x[rs1]+sext(offset))&∼1; x[rd]=t
            if (debug) System.out.println("jalr");
            if (i.funct3 == 0x0) {
                int temp = pc + 4;
                // move pc
                pc = (reg[i.rs1] + i.imm - 4) & 0xFFFFFFFE;
                reg[i.rd] = temp;
            }
            return;
        }
        if (i.opcode == 0x3) {
            //  lb
            if (i.funct3 == 0x0) {
                if (debug) System.out.println("lb");
                if ((memory[reg[i.rs1] + i.imm]) >> 7 == 1) // should be sign extend (negative value)
                    reg[i.rd] = sext((memory[reg[i.rs1] + i.imm]), 8);
                else {
                    int res = (memory[reg[i.rs1] + i.imm]);
                    reg[i.rd] = res;
                }
                return;
            }
            //  lh
            if (i.funct3 == 0x1) {
                if (debug) System.out.println("lh");
                int result = memory[reg[i.rs1] + i.imm] | ((memory[reg[i.rs1] + i.imm + 1]) << 8);
                if ((result & 0x8000) > 0) result = sext(result, 16);
                reg[i.rd] = result;

                return;
            }
            //  lw
            if (i.funct3 == 0x2) {
                if (debug) System.out.println("lw");
                reg[i.rd] = (memory[reg[i.rs1] + i.imm]) | ((memory[reg[i.rs1] + i.imm + 1]) << 8) | ((memory[reg[i.rs1] + i.imm + 2]) << 16) | ((memory[reg[i.rs1] + i.imm + 3]) << 24);
                return;
            }
            //  ld
            if (i.funct3 == 0x3) {
                if (debug) System.out.println("ld");
                reg[i.rd] = memory[reg[i.rs1] + i.imm];
                return;
            }
            // lbu
            if (i.funct3 == 0x4) {
                if (debug) System.out.println("lbu");
                reg[i.rd] = (int) unsignedValue(memory[reg[i.rs1] + i.imm]);
                return;
            }
            // lhu
            if (i.funct3 == 0x5) {
                if (debug) System.out.println("lhu");
                int result = (memory[reg[i.rs1] + i.imm]) | ((memory[reg[i.rs1] + i.imm + 1]) << 8);
                reg[i.rd] = (int) unsignedValue(result);
                return;
            }
            // lwu
            if (i.funct3 == 0x6) {
                if (debug) System.out.println("lwu");
                int result = (memory[reg[i.rs1] + i.imm]) | ((memory[reg[i.rs1] + i.imm + 1]) << 8) | ((memory[reg[i.rs1] + i.imm + 2]) << 16) | ((memory[reg[i.rs1] + i.imm + 3]) << 24);
                reg[i.rd] = (int) unsignedValue(result);
                return;
            }
        }
        if (i.opcode == 0x13) {
            // addi
            if (i.funct3 == 0x0) {
                if (debug) System.out.println("addi: reg[" + i.rd + "] = " + reg[i.rs1] + " + " + i.imm);
                reg[i.rd] = (reg[i.rs1] + i.imm);
                return;
            }
            // slli
            if (i.funct3 == 0x1 && i.funct6 == 0x00) {
                if (debug) System.out.println("slli");
                reg[i.rd] = reg[i.rs1] << (i.imm & 0x1F);
                return;
            }
            // slti
            if (i.funct3 == 0x2) {
                if (debug) System.out.println("slti");
                reg[i.rd] = reg[i.rs1] < i.imm ? 1 : 0;
                return;
            }
            // sltiu
            if (i.funct3 == 0x3) {
                if (debug) System.out.println("sltiu");
                reg[i.rd] = (unsignedValue(reg[i.rs1]) < unsignedValue(i.imm)) ? 1 : 0;
                return;
            }
            // xori
            if (i.funct3 == 0x4) {
                if (debug) System.out.println("xori");
                reg[i.rd] = reg[i.rs1] ^ i.imm;
                return;
            }
            // srli
            if (i.funct3 == 0x5 && i.funct6 == 0x00) {
                if (debug) System.out.println("srli");
                reg[i.rd] = (int) (unsignedValue(reg[i.rs1]) >> unsignedValue(i.imm));
                return;
            }
            // srai
            if (i.funct3 == 0x5 && i.funct6 == 0x10) {
                if (debug) System.out.println("srai");
                reg[i.rd] = reg[i.rs1] >> i.imm;
                return;
            }
            // ori
            if (i.funct3 == 0x6) {
                if (debug) System.out.println("ori");
                reg[i.rd] = reg[i.rs1] | i.imm;
                return;
            }
            // andi
            if (i.funct3 == 0x7) {
                if (debug) System.out.println("andi");
                reg[i.rd] = reg[i.rs1] & i.imm;
                return;
            }

            throw new ExecutionControl.NotImplementedException(c.colorText("I-type instruction not implemented 🛠😤", TUIColors.RED));
        }
    }

    private void processSB(SB i) throws ExecutionControl.NotImplementedException {
        // beq instruction
        if ((i.funct3 == 0x0)) {
            if (debug) System.out.println("beq");
            if (reg[i.rs1] == reg[i.rs2]) {
                pc = pc + i.imm - 4;
            }
            return;
        }
        // bne instruction
        if ((i.funct3 == 0x1)) {
            if (debug) System.out.println("bne");
            if (reg[i.rs1] != reg[i.rs2]) {
                pc = pc + i.imm - 4;
            }
            return;
        }
        // blt instruction
        if ((i.funct3 == 0x4)) {
            if (debug) System.out.println("blt");
            if (reg[i.rs1] < reg[i.rs2]) {
                pc = pc + i.imm - 4;
            }
            return;
        }
        // bge instruction
        if ((i.funct3 == 0x5)) {
            if (debug) System.out.println("bge");
            if (reg[i.rs1] >= reg[i.rs2]) {
                pc += i.imm - 4;
                return;
            }
        }
        // bltu instruction
        if ((i.funct3 == 0x6)) {
            if (debug) System.out.println("bltu");
            if (unsignedValue(reg[i.rs1]) < unsignedValue(reg[i.rs2])) {
                pc += i.imm - 4;
            }
            return;
        }
        // bgeu instruction
        if ((i.funct3 == 0x7)) {
            if (debug) System.out.println("bgeu");
            if (unsignedValue(reg[i.rs1]) >= unsignedValue(reg[i.rs2])) {
                pc = pc + i.imm - 4;
            }
        }
    }

    private void processR(R i) throws ExecutionControl.NotImplementedException {
        // add
        if (i.funct3 == 0x00 && i.funct7 == 0x00) {
            if (debug) System.out.println("add");
            reg[i.rd] = reg[i.rs1] + reg[i.rs2];
            return;
        }
        // sub
        if (i.funct3 == 0x0 && i.funct7 == 0x20) {
            if (debug) System.out.println("sub");
            reg[i.rd] = reg[i.rs1] - reg[i.rs2];
            return;
        }
        // sll
        if (i.funct3 == 0x1 && i.funct7 == 0x00) {
            if (debug) System.out.println("sll");
            reg[i.rd] = reg[i.rs1] << reg[i.rs2];
            return;
        }
        // xor
        if (i.funct3 == 0x4 && i.funct7 == 0x0) {
            if (debug) System.out.println("xor");
            reg[i.rd] = reg[i.rs1] ^ reg[i.rs2];
            return;
        }
        // srl
        if (i.funct3 == 0x5 && i.funct7 == 0x00) {
            if (debug) System.out.println("srl: " + reg[i.rs1] + " >> " + reg[i.rs2]);
            reg[i.rd] = (int) (unsignedValue(reg[i.rs1]) >> unsignedValue(reg[i.rs2]));
            return;
        }
        // sra
        if (i.funct3 == 0x5 && i.funct7 == 0x20) {
            if (debug) System.out.println("sra");
            reg[i.rd] = reg[i.rs1] >> reg[i.rs2];
            return;
        }
        //  or
        if (i.funct3 == 0x6 && i.funct7 == 0x00) {
            if (debug) System.out.println("or");
            reg[i.rd] = reg[i.rs1] | reg[i.rs2];
            return;
        }
        // and
        if (i.funct3 == 0x7 && i.funct7 == 0x00) {
            if (debug) System.out.println("and");
            reg[i.rd] = reg[i.rs1] & reg[i.rs2];
            return;
        }
        // slt
        if (i.funct3 == 0x2 && i.funct7 == 0x00) {
            if (debug) System.out.println("slt");
            reg[i.rd] = (reg[i.rs1] < reg[i.rs2]) ? 1 : 0;
            return;
        }
        // sltu
        if (i.funct3 == 0x3 && i.funct7 == 0x00) {
            if (debug) System.out.println("sltu");
            reg[i.rd] = (reg[i.rs1] < reg[i.rs2]) ? 1 : 0;
            return;
        }

        throw new ExecutionControl.NotImplementedException(c.colorText("R-type instruction not implemented 🛠😤", TUIColors.RED));
    }

    private long unsignedValue(int v) {
        return v & 0xffffffffL;
    }

    private int sext(int val, int bits) {
        int shift = 32 - bits;
        return val << shift >> shift;
    }

}