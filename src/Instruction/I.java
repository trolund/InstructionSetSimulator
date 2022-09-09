package Instruction;

import Instruction.abstact.RD;

public class I extends RD {
    public int rs1;
    public int imm;
    public int funct6;
    public int funct3;

    public I(int instr) {
        super(instr);
        rd = (instr >> 7) & 0x1F;
        funct3 = (instr >> 12) & 0x7 ;
        rs1 = (instr >> 15) & 0x1F;
        imm = (instr >> 20) & 0xFFF;
        funct6 = (instr >> 26) & 0x3F;
    }
}

