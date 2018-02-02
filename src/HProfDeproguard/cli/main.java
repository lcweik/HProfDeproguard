package HProfDeproguard.cli;

import HProfDeproguard.hprof.HProfParse;
import HProfDeproguard.ProguardMapping.PMapParse;

public class main {
    public static void main(String []args) {
        if (args.length != 2) {
            System.out.println("java -jar deproguard.jar <hprof_file> <mapping_file>");
            System.out.println("output: <hprof_file>.out");
            return;
        }
        try {
            HProfParse hp = new HProfParse(args[0]);
            hp.parse();
            PMapParse mp = new PMapParse(args[1]);
            mp.parse();
            hp.Deproguard(mp);
            hp.write();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }
}
