public class IsaSim {

    static int pc;
    static int reg[] = new int[4];

    // Here the first program hard coded as an array
/*
    static int progr[] = {
            // As minimal RISC-V assembler example
            0x00200093, // addi x1 x0 2
            0x00300113, // addi x2 x0 3
            0x002081b3, // add x3 x1 x2
    };
*/

    public static void main(String[] args) {
        System.out.println("Hello RISC-V World!");


        ProgramLoader loader = new ProgramLoader();
        int[] progr = loader.loadProgram("first", ProgramLoader.ProgramType.BINARY);

        pc = 0;

        for (; ; ) {

            int instr = progr[pc >> 2];
            int opcode = instr & 0x7f;
            int rd = (instr >> 7) & 0x01f;
            int rs1 = (instr >> 15) & 0x01f;
            int imm = (instr >> 20);

            switch (opcode) {

                case 0x13:
                    reg[rd] = reg[rs1] + imm;
                    break;
                default:
                    System.out.println("Opcode " + opcode + " not yet implemented");
                    break;
            }

            pc += 4; // One instruction is four bytes
            if ((pc >> 2) >= progr.length) {
                break;
            }
            for (int i = 0; i < reg.length; ++i) {
                System.out.print(reg[i] + " ");
            }
            System.out.println();
        }

        System.out.println("Program exit");

    }

}