import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

public class ProgramLoader {

    enum ProgramType {
        ASSEMBLY,
        BINARY,
        TEXT
    }

    public int[] loadTest(String programName) {
        return loadTest(programName, ProgramType.TEXT, true);
    }

    public int[] loadTest(String programName, ProgramType programType) {
        return loadTest(programName, programType, true);
    }

    public int[] loadTest(String programName, ProgramType programType, boolean test) {
        InputStream inputStream = null;
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(Objects.requireNonNull(classLoader.getResource("TestPrograms/" + programType + "/" + programName + ".txt")).getFile());
            inputStream = new FileInputStream(file);
            return transformInputStream(inputStream);
        }catch (Exception e) {
            System.out.println(e);
            System.err.println("Failed to load program: " + programName);
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return new int[0];
    }

    public int[] loadProgram(String url) {
        InputStream inputStream = null;
        try {
            File file = new File(url);
            inputStream = new FileInputStream(file);
            return transformInputStream(inputStream);
        }catch (Exception e) {
            System.out.println(e);
            System.err.println("Failed to load program: " + url);
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return new int[0];
    }

    public File[] getAllFilesWithEx(String path, String ex) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(Objects.requireNonNull(classLoader.getResource(path)).getFile());

            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File f, String name) {
                    return name.endsWith("." + ex);
                }
            };

                return file.listFiles(filter);
             }catch (Exception e) {
            System.out.println(e);
            }
        return null;
    }

    private String getFileName(String filename){
        String[] parts = filename.split("\\.");
        return parts[0];
    }

    public File findFileWithName(String filename, File[] files){

        String name = getFileName(filename);

        for(File f: files){
            if(getFileName(f.getName()).equals(name)) {
                return f;
            }
        }

        return null;
    }

    public int[] readBinFile(File file) throws IOException {
        ArrayList<Integer> program = new ArrayList<>();
        DataInputStream binFile = new DataInputStream(file.toURL().openStream());
        while(binFile.available() > 0) {
            try
            {
                int instr = binFile.readInt();
                instr = Integer.reverseBytes(instr);
                program.add(instr);
            }
            catch(EOFException e) {
                e.printStackTrace();
            }
        }
        binFile.close();

        return program.stream().mapToInt(i -> (int) i).toArray();
    }

    private int[] transformInputStream(InputStream inputStream) throws IOException {
        ArrayList program = new ArrayList<Integer>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                int processedInput = processLine(line);
                if(processedInput > 0) program.add(processedInput);
            }
        }
        return program.stream().mapToInt(i -> (int) i).toArray();
    }

    private int processLine(String line) {
        if(line.isBlank() || line.isEmpty() || line.charAt(0) == '/' || line.charAt(0) == '#') return -1; // discard line
        String[] parts = line.split("//|#");
        return Integer.decode(parts[0].trim());
    }
}
