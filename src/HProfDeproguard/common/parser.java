package HProfDeproguard.common;

import java.io.DataInputStream;
import java.io.FileInputStream;

public abstract class parser {
    protected DataInputStream disFile;


    public parser(String fileName) {
        try {
            disFile = new DataInputStream(new FileInputStream(fileName));
        } catch (Exception e) {
            System.out.println("File not found: " + fileName);
        }
    }

    public abstract void parse();

}
