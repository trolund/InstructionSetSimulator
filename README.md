# InstructionSetSimulator

RISC-V simulator implementing the RV32I Instructions.

The project is done as part of the course 02155 Computer Architecture and Engineering at DTU.

RV32I Instructions info:
https://msyksphinz-self.github.io/riscv-isadoc/html/rvi.html#lbu
https://itnext.io/risc-v-instruction-set-cheatsheet-70961b4bbe8

## RV32I usage

The CLI can be used by using the compiled jar file *rv32i.jar* like this: 

    java -cp "picocli-4.7.0.jar:rv32i.jar" RV32I

The CLI have a number of options that can be seen by using the command:

    RV32I --help 

this will result in this info:

    Usage: RV32I [-dhpruV] [-e=<ecall>] <path>
    A basic RISC-V simulator. Supporting the RV32I instructions.
    <path>            Path to the bin file containing the program.
    -d, --debug           Will do debug printing. default: false.
    -e, --ecall=<ecall>   The register that 'ecall' uses to decide the kind of
    env call. default: a7.
    -h, --help            Show this help message and exit.
    -p, --print           Will print the 32 registers to the console after each
    instruction. default: false.
    -r, --result          Print the result of the 32 registers after execution.
    default: true.
    -u, --dump            Write data-dump file after execution. default: false.
    -V, --version         Print version information and exit.