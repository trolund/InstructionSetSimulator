package Core;

import Instruction.abstact.Instruction;
import Instruction.R;
import Instruction.I;
import Instruction.S;
import Instruction.U;
import Instruction.SB;
import Instruction.UJ;

public class InstructionDecoder {

    public <T extends Instruction> T process(int instr, boolean debug) throws Exception {
        return (T) mapInstruction(instr, debug);
    }

    public <T extends Instruction> T process(int instr) throws Exception {
        return (T) mapInstruction(instr, false);
    }

    private Instruction mapInstruction(int instr, boolean debug) throws Exception {
        int opcode = instr & 0x7f;
        switch (opcode) {
            //Format: R-type
            case 0x33 -> {
                if (debug) System.out.println("Type: R-type");
                return new R(instr);
            }
            //Format: I-type
            case 0x3, 0x13, 0x67, 0x73 -> {
                if (debug) System.out.println("Type: I-type");
                return new I(instr);
            }
            //Format: S-type
            case 0x23 -> {
                if (debug) System.out.println("Type: S-type");
                return new S(instr);
            }
            //Format: SB-type
            case 0x63 -> {
                if (debug) System.out.println("Type: SB-type");
                return new SB(instr);
            }
            //Format: U-type
            case 0x37, 0x17 -> {
                if (debug) System.out.println("Type: U-type");
                return new U(instr);
            }
            case 0x6F -> {  //UJ type
                if (debug) System.out.println("Type: UJ-type");
                return new UJ(instr);
            }
            default -> throw new Exception("Type is not implemented (opcode: " + opcode + ")");
        }

    }
}
