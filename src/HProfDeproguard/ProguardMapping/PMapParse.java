package HProfDeproguard.ProguardMapping;

import HProfDeproguard.common.parser;

import java.util.ArrayList;
import java.util.HashMap;

import static HProfDeproguard.ProguardMapping.PMapParse.proguardItem.TYPE_CLASS;
import static HProfDeproguard.ProguardMapping.PMapParse.proguardItem.TYPE_FIELD;
import static HProfDeproguard.ProguardMapping.PMapParse.proguardItem.TYPE_METHOD;

public class PMapParse extends parser {
    public PMapParse(String filename) {
        super(filename);
    }

    public HashMap<String, proguardItem> clazzMap = new HashMap<String, proguardItem>();

    public class proguardItem {
        public String name1;
        public String name2;

        public String proto;

        public int type;

        public static final int TYPE_CLASS = 0;
        public static final int TYPE_METHOD = 1;
        public static final int TYPE_FIELD = 2;

        public ArrayList<proguardItem> children;

    }

    public void parse() {
        try {
            proguardItem lastClazzItem = null;
            while (true) {
                String line = disFile.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("    ")) {
                    proguardItem methodOrFieldItem = new proguardItem();
                    int p1 = line.indexOf(" ", 4);
                    int p2 = line.indexOf(" -> ");
                    int p3 = line.indexOf("(");
                    if (p3 == -1) {
                        methodOrFieldItem.type = TYPE_FIELD;
                        p3 = p2;
                    }
                    else {
                        methodOrFieldItem.type = TYPE_METHOD;
                        methodOrFieldItem.proto = line.substring(p3, line.indexOf(")") + 1);
                    }
                    methodOrFieldItem.name1 = line.substring(p1+1,p3);
                    methodOrFieldItem.name2 = line.substring(p2 + 4);
                    lastClazzItem.children.add(methodOrFieldItem);

                } else if (line.contains(" -> ")) {
                    int p1 = line.indexOf(" -> ");
                    String clazzName1 = line.substring(0, p1);
                    String clazzName2 = line.substring(p1 + 4, line.lastIndexOf(":"));
                    lastClazzItem = new proguardItem();
                    lastClazzItem.name1 = clazzName1;
                    lastClazzItem.name2 = clazzName2;
                    lastClazzItem.type = TYPE_CLASS;
                    lastClazzItem.children = new ArrayList<proguardItem>();

                    clazzMap.put(clazzName2, lastClazzItem);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
