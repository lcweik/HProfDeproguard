package HProfDeproguard.hprof;

import HProfDeproguard.common.parser;
import HProfDeproguard.ProguardMapping.PMapParse;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static HProfDeproguard.ProguardMapping.PMapParse.proguardItem.TYPE_FIELD;

public class HProfParse extends parser {
    static final byte HPROF_TAG_STRING = 0x01;
    static final byte HPROF_TAG_LOAD_CLASS = 0x02;
    static final byte HPROF_TAG_UNLOAD_CLASS = 0x03;
    static final byte HPROF_TAG_STACK_FRAME = 0x04;
    static final byte HPROF_TAG_STACK_TRACE = 0x05;
    static final byte HPROF_TAG_ALLOC_SITES = 0x06;
    static final byte HPROF_TAG_HEAP_SUMMARY = 0x07;
    static final byte HPROF_TAG_START_THREAD = 0x0A;
    static final byte HPROF_TAG_END_THREAD = 0x0B;
    static final byte HPROF_TAG_HEAP_DUMP = 0x0C;
    static final byte HPROF_TAG_HEAP_DUMP_SEGMENT = 0x1C;
    static final byte HPROF_TAG_HEAP_DUMP_END = 0x2C;
    static final byte HPROF_TAG_CPU_SAMPLES = 0x0D;
    static final byte HPROF_TAG_CONTROL_SETTINGS = 0x0E;

    static final byte HPROF_ROOT_UNKNOWN = (byte)0xFF;
    static final byte HPROF_ROOT_JNI_GLOBAL = 0x01;
    static final byte HPROF_ROOT_JNI_LOCAL = 0x02;
    static final byte HPROF_ROOT_JAVA_FRAME = 0x03;
    static final byte HPROF_ROOT_NATIVE_STACK = 0x04;
    static final byte HPROF_ROOT_STICKY_CLASS = 0x05;
    static final byte HPROF_ROOT_THREAD_BLOCK = 0x06;
    static final byte HPROF_ROOT_MONITOR_USED = 0x07;
    static final byte HPROF_ROOT_THREAD_OBJECT = 0x08;
    static final byte HPROF_CLASS_DUMP = 0x20;
    static final byte HPROF_INSTANCE_DUMP = 0x21;
    static final byte HPROF_OBJECT_ARRAY_DUMP = 0x22;
    static final byte HPROF_PRIMITIVE_ARRAY_DUMP = 0x23;

    static final byte HPROF_HEAP_DUMP_INFO = (byte)0xfe;
    static final byte HPROF_ROOT_INTERNED_STRING = (byte)0x89;
    static final byte HPROF_ROOT_FINALIZING = (byte)0x8a;  // Obsolete.
    static final byte HPROF_ROOT_DEBUGGER = (byte)0x8b;
    static final byte HPROF_ROOT_REFERENCE_CLEANUP = (byte)0x8c;  // Obsolete.
    static final byte HPROF_ROOT_VM_INTERNAL = (byte)0x8d;
    static final byte HPROF_ROOT_JNI_MONITOR = (byte)0x8e;
    static final byte HPROF_UNREACHABLE = (byte)0x90;  // Obsolete.
    static final byte HPROF_PRIMITIVE_ARRAY_NODATA_DUMP = (byte)0xc3;  // Obsolete.


    public HProfParse(String fileName) {
        super(fileName);
        try {
            dosFile = new DataOutputStream(new FileOutputStream(fileName + ".out"));
        } catch (Exception e) {
            System.out.println("File not found: " + fileName);
        }
    }

    public void write() {
        try {
            dosFile.write(HProfFileHeader);
            Iterator iterator = hprofTagStringMap.entrySet().iterator();
            while(iterator.hasNext()) {
                HashMap.Entry entry = (HashMap.Entry) iterator.next();
                Integer key = (Integer) entry.getKey();
                String val = (String) entry.getValue();
                dosFile.writeByte(HPROF_TAG_STRING);
                dosFile.writeInt(0);
                dosFile.writeInt(val.getBytes().length + 4);
                dosFile.writeInt(key.intValue());
                dosFile.write(val.getBytes());
            }

            for (HashMap.Entry entry: hprofTagLoadClassMap.entrySet()) {
                Integer key = (Integer) entry.getKey();
                HProfLoadClass val = (HProfLoadClass)entry.getValue();
                dosFile.writeByte(HPROF_TAG_LOAD_CLASS);
                dosFile.writeInt(val.timeStamp);
                dosFile.writeInt(16);
                dosFile.writeInt(val.sn);
                dosFile.writeInt(val.objectId);
                dosFile.writeInt(val.stackTraceSerialNumber);
                dosFile.writeInt(val.classNameId);
            }


            for (HashMap.Entry entry: hprofTagStackFrameMap.entrySet()) {
                Integer key = (Integer) entry.getKey();
                HProfStackFrame val = (HProfStackFrame) entry.getValue();
            }


            for (HashMap.Entry entry: hprofTagStackTraceMap.entrySet()) {
                Integer key = (Integer) entry.getKey();
                HProfStackTrace val = (HProfStackTrace) entry.getValue();
                dosFile.writeByte(HPROF_TAG_STACK_TRACE);
                dosFile.writeInt(val.timeStamp);
                dosFile.writeInt(12);
                dosFile.writeInt(val.stackTraceSerialNumber);
                dosFile.writeInt(val.threadSerialNumber);
                dosFile.writeInt(val.numberOfFrames);
            }

            for (HProfHeapDumpSegment hds: hprofHeapDumpSegmentList) {
                dosFile.writeByte(HPROF_TAG_HEAP_DUMP_SEGMENT);
                dosFile.writeInt(hds.TimeStamp);
                dosFile.writeInt(hds.NodeSize);  // may incorrect
                for (RootObject ro: hds.objects) {
                    ro.write(dosFile);

                }

            }

            dosFile.writeByte(HPROF_TAG_HEAP_DUMP_END);
            dosFile.writeInt(0);
            dosFile.writeInt(0);

            dosFile.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    public void parse() {
        try {
            disFile.read(HProfFileHeader);
            System.out.println(new String(HProfFileHeader));
            int numcount = 0;
            while (true) {
                ++numcount;
                byte tagType = disFile.readByte();
                int timeStamp = disFile.readInt();
                int nodeSize = disFile.readInt();

                switch (tagType) {
                    case HPROF_TAG_STRING: {
                        int id = disFile.readInt();
                        byte [] bufferString = new byte[nodeSize -4];
                        disFile.read(bufferString);
                        String str = new String(bufferString);
                        hprofTagStringMap.put(new Integer(id), str);
                        break;
                    }

                    case HPROF_TAG_LOAD_CLASS: {
                        HProfLoadClass hpofLoadClass = new HProfLoadClass();
                        hpofLoadClass.timeStamp = timeStamp;
                        hpofLoadClass.sn = disFile.readInt();
                        hpofLoadClass.objectId = disFile.readInt();
                        hpofLoadClass.stackTraceSerialNumber = disFile.readInt();
                        hpofLoadClass.classNameId = disFile.readInt();
                        hprofTagLoadClassMap.put(new Integer(hpofLoadClass.objectId), hpofLoadClass);
                        break;
                    }

                    case HPROF_TAG_STACK_FRAME: {
                        HProfStackFrame hprofStackFrame = new HProfStackFrame();
                        hprofStackFrame.timeStamp = timeStamp;
                        hprofStackFrame.stackFrameId = disFile.readInt();
                        hprofStackFrame.methodNameId = disFile.readInt();
                        hprofStackFrame.methodSignatureStringId = disFile.readInt();
                        hprofStackFrame.sourceFileNameStringId = disFile.readInt();
                        hprofStackFrame.classSerialNumber = disFile.readInt();
                        hprofStackFrame.lineNumber = disFile.readInt();
                        hprofTagStackFrameMap.put(new Integer(hprofStackFrame.stackFrameId), hprofStackFrame);
                        break;
                    }
                    case HPROF_TAG_STACK_TRACE: {
                        HProfStackTrace hprofStackTrace =  new HProfStackTrace();
                        hprofStackTrace.timeStamp = timeStamp;
                        hprofStackTrace.stackTraceSerialNumber = disFile.readInt();
                        hprofStackTrace.threadSerialNumber = disFile.readInt();
                        hprofStackTrace.numberOfFrames = disFile.readInt();
                        hprofStackTrace.SeriesOfStackFrameIds = new int[hprofStackTrace.numberOfFrames];
                        for (int i = 0; i< hprofStackTrace.numberOfFrames; ++i) {
                            hprofStackTrace.SeriesOfStackFrameIds[i] = disFile.readInt();
                        }
                        if (hprofTagStackTraceMap.get(new Integer(hprofStackTrace.threadSerialNumber)) != null) {
                            break;
                        }
                        hprofTagStackTraceMap.put(new Integer(hprofStackTrace.threadSerialNumber), hprofStackTrace);
                        break;
                    }
                    case HPROF_TAG_HEAP_DUMP_SEGMENT: {
                        hprofHeapDumpSegmentList.add(new HProfHeapDumpSegment(disFile, nodeSize, timeStamp));
                        break;
                    }
                    case HPROF_TAG_HEAP_DUMP_END:
                        break;
                    default: {
                        byte [] bufferUnknownNode = new byte[nodeSize];
                        disFile.read(bufferUnknownNode);
                        UnKnownNode unknownNode = new UnKnownNode();
                        unknownNode.timeStamp = timeStamp;
                        unknownNode.data = bufferUnknownNode;
                        unknownNodes.add(unknownNode);
                        break;
                    }
                }
                if (tagType == HPROF_TAG_HEAP_DUMP_END)
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getStringId(String str1) {

        for (HashMap.Entry entry: hprofTagStringMap.entrySet()) {
            String str2 = (String)entry.getValue();
            if (str2.equals(str1)) {
                return (Integer) entry.getKey();
            }
        }
        while (hprofTagStringMap.get(extrasId) != null) {
            ++extrasId;
        }

        hprofTagStringMap.put(extrasId, str1);
        return extrasId;
    }

    public void Deproguard(PMapParse mp) {
        for (HProfHeapDumpSegment hds: hprofHeapDumpSegmentList) {
            for (RootObject iterRootObject : hds.objects) {
                if (iterRootObject.heapTag == HPROF_CLASS_DUMP) {
                    String clazzName = hprofTagStringMap.get(hprofTagLoadClassMap.get(iterRootObject.classId).classNameId);
                    PMapParse.proguardItem iterProguardItem = mp.clazzMap.get(clazzName);
                    if (iterProguardItem == null)
                        continue;
                    for (HProfFieldItem iterHProfFieldItem : iterRootObject.hprofClassObjectExtras.instanceFields) {
                        String filedName = hprofTagStringMap.get(iterHProfFieldItem.stringId);
                        for (PMapParse.proguardItem cim : iterProguardItem.children) {
                            if (cim.type ==  TYPE_FIELD) {
                                if (cim.name2.equals(filedName)) {
                                    iterHProfFieldItem.stringId = getStringId(cim.name1);
        }}}}}}}


        for (HashMap.Entry entry: hprofTagLoadClassMap.entrySet()) {
            HProfLoadClass val = (HProfLoadClass)entry.getValue();
            String fieldName = hprofTagStringMap.get(val.classNameId);
            PMapParse.proguardItem iterProguardItem = mp.clazzMap.get(fieldName);
            if (iterProguardItem !=  null) {
                if (!iterProguardItem.name1.equals(iterProguardItem.name2)) {
                    val.classNameId = HProfParse.this.getStringId(iterProguardItem.name1);
        }}}
    }

    class HProfFieldItem {
        public int stringId;
        public byte type;
        public byte v1;
        public short v2;
        public int v4;
        public long v8;
    }

    class HProfClassObjectExtras {
        public int class_size_without_overhead;
        public short const_pool;
        public short num_static_fields;

        public ArrayList<HProfFieldItem> staticFields;
        public int iFieldCount;
        public ArrayList<HProfFieldItem> instanceFields;
    }

    class RootObject {
        public int heapTag;
        public int objectId;
        public int threadSerial;
        public int jniObjectId;
        public int tail;
        public int heapType;
        public int nameId;
        public int classId;
        public int superClassId;

        //Array
        public int length;
        public byte arraytype;
        public ArrayList<HProfFieldItem> items;



        public byte extras[];

        public int size;

        public HProfClassObjectExtras hprofClassObjectExtras;


        public RootObject(DataInputStream disFile) throws Exception {
            int r1 = disFile.available();
            heapTag = disFile.readByte();
            switch (heapTag) {
                case HPROF_ROOT_UNKNOWN:
                case HPROF_ROOT_STICKY_CLASS:
                case HPROF_ROOT_MONITOR_USED:
                case HPROF_ROOT_INTERNED_STRING:
                case HPROF_ROOT_DEBUGGER:
                case HPROF_ROOT_VM_INTERNAL:
                    objectId = disFile.readInt();
                    size = 5;
                    break;
                case HPROF_ROOT_JNI_GLOBAL:
                    objectId = disFile.readInt();
                    jniObjectId = disFile.readInt();
                    size = 9;
                    break;
                case HPROF_ROOT_JNI_LOCAL:
                case HPROF_ROOT_JNI_MONITOR:
                case HPROF_ROOT_JAVA_FRAME:
                case HPROF_ROOT_THREAD_OBJECT:
                    objectId = disFile.readInt();
                    threadSerial = disFile.readInt();
                    tail = disFile.readInt();
                    size =13;
                    assert(tail == -1);
                    break;
                case HPROF_ROOT_NATIVE_STACK:
                case HPROF_ROOT_THREAD_BLOCK:
                    objectId = disFile.readInt();
                    threadSerial = disFile.readInt();
                    size = 9;
                    break;
                case HPROF_CLASS_DUMP:
                    classId = disFile.readInt();
                    threadSerial = disFile.readInt();
                    superClassId = disFile.readInt();
                    objectId = disFile.readInt();
                    disFile.readInt();
                    disFile.readInt();
                    disFile.readInt();
                    disFile.readInt();
                    HProfClassObjectExtras hprofClassObjectExtras = new HProfClassObjectExtras();
                    hprofClassObjectExtras.class_size_without_overhead = disFile.readInt();
                    hprofClassObjectExtras.const_pool = disFile.readShort();
                    if (hprofClassObjectExtras.class_size_without_overhead == 0) {
                        //System.out.println("error");
                        //break;
                    }
                    hprofClassObjectExtras.num_static_fields = disFile.readShort();
                    hprofClassObjectExtras.staticFields = new ArrayList<HProfFieldItem>();

                    for (int i = 0; i< hprofClassObjectExtras.num_static_fields; ++i) {
                        HProfFieldItem hprofFieldItem = new HProfFieldItem();
                        hprofFieldItem.stringId = disFile.readInt();
                        hprofFieldItem.type = disFile.readByte();
                        int typesize = computeBasicLen(hprofFieldItem.type);
                        switch (typesize) {
                            case 1:
                                hprofFieldItem.v1 = disFile.readByte();
                                break;
                            case 2:
                                hprofFieldItem.v2 = disFile.readShort();
                                break;
                            case 4:
                                hprofFieldItem.v4 = disFile.readInt();
                                break;
                            case 8:
                                hprofFieldItem.v8 = disFile.readLong();
                                break;
                            default:
                                System.out.println("error");
                                break;
                        }
                        hprofClassObjectExtras.staticFields.add(hprofFieldItem);
                    }
                    hprofClassObjectExtras.iFieldCount = disFile.readShort();
                    hprofClassObjectExtras.instanceFields = new ArrayList<HProfFieldItem>();
                    for (int i = 0; i< hprofClassObjectExtras.iFieldCount; ++i) {
                        HProfFieldItem cf = new HProfFieldItem();
                        cf.stringId = disFile.readInt();
                        cf.type = disFile.readByte();
                        hprofClassObjectExtras.instanceFields.add(cf);
                    }
                    this.hprofClassObjectExtras = hprofClassObjectExtras;
                    size = r1 - disFile.available();
                    break;
                case HPROF_INSTANCE_DUMP: {
                    objectId = disFile.readInt();
                    threadSerial = disFile.readInt();
                    classId = disFile.readInt();
                    int extrasSize = disFile.readInt();
                    extras = new byte[extrasSize];
                    disFile.read(extras);
                    size = 17 + extrasSize;
                    break;
                }
                case HPROF_OBJECT_ARRAY_DUMP: {
                    objectId = disFile.readInt();
                    threadSerial = disFile.readInt();
                    length = disFile.readInt();
                    classId = disFile.readInt();
                    items = new ArrayList<HProfFieldItem>();
                    while (length>0) {
                        --length;
                        HProfFieldItem iv = new HProfFieldItem();
                        iv.v4 = disFile.readInt();
                        items.add(iv);
                    }
                    size = r1 - disFile.available();
                    break;
                }
                case HPROF_PRIMITIVE_ARRAY_DUMP: {
                    objectId = disFile.readInt();
                    threadSerial = disFile.readInt();
                    length = disFile.readInt();
                    arraytype = disFile.readByte();
                    int extrasTypeSize = computeBasicLen(arraytype);
                    int extrasSize = length * extrasTypeSize;
                    extras = new byte[extrasSize];
                    disFile.read(extras);
                    size = 14 + extrasSize;
                    break;
                }
                case HPROF_HEAP_DUMP_INFO:
                    heapType = disFile.readInt();
                    nameId = disFile.readInt();
                    size = 9;
                    break;
                case HPROF_PRIMITIVE_ARRAY_NODATA_DUMP:
                case HPROF_ROOT_FINALIZING:
                case HPROF_ROOT_REFERENCE_CLEANUP:
                case HPROF_UNREACHABLE:
                    size = 1;
                    break;
            }

        }

        public void write(DataOutputStream dosFile) throws Exception{
            dosFile.writeByte(heapTag);

            switch (heapTag) {
                case HPROF_ROOT_UNKNOWN:
                case HPROF_ROOT_STICKY_CLASS:
                case HPROF_ROOT_MONITOR_USED:
                case HPROF_ROOT_INTERNED_STRING:
                case HPROF_ROOT_DEBUGGER:
                case HPROF_ROOT_VM_INTERNAL:
                    dosFile.writeInt(objectId);
                    break;
                case HPROF_ROOT_JNI_GLOBAL:
                    dosFile.writeInt(objectId);
                    dosFile.writeInt(jniObjectId);
                    break;
                case HPROF_ROOT_JNI_LOCAL:
                case HPROF_ROOT_JNI_MONITOR:
                case HPROF_ROOT_JAVA_FRAME:
                case HPROF_ROOT_THREAD_OBJECT:
                    dosFile.writeInt(objectId);
                    dosFile.writeInt(threadSerial);
                    dosFile.writeInt(tail);
                    break;
                case HPROF_ROOT_NATIVE_STACK:
                case HPROF_ROOT_THREAD_BLOCK:
                    dosFile.writeInt(objectId);
                    dosFile.writeInt(threadSerial);
                    break;
                case HPROF_CLASS_DUMP:
                    dosFile.writeInt(classId);
                    dosFile.writeInt(threadSerial);
                    dosFile.writeInt(superClassId);
                    dosFile.writeInt(objectId);
                    dosFile.writeInt(0);
                    dosFile.writeInt(0);
                    dosFile.writeInt(0);
                    dosFile.writeInt(0);
                    dosFile.writeInt(hprofClassObjectExtras.class_size_without_overhead);
                    dosFile.writeShort(hprofClassObjectExtras.const_pool);
                    dosFile.writeShort(hprofClassObjectExtras.num_static_fields);



                    for (HProfFieldItem cf: hprofClassObjectExtras.staticFields) {
                        dosFile.writeInt(cf.stringId);
                        dosFile.writeByte(cf.type);
                        int typesize = computeBasicLen(cf.type);
                        switch (typesize) {
                            case 1:
                                dosFile.writeByte(cf.v1);
                                break;
                            case 2:
                                dosFile.writeShort(cf.v2);
                                break;
                            case 4:
                                dosFile.writeInt(cf.v4);
                                break;
                            case 8:
                                dosFile.writeLong(cf.v8);
                                break;
                            default:
                                System.out.println("error");
                                break;
                        }
                    }

                    dosFile.writeShort(hprofClassObjectExtras.iFieldCount);

                    for (HProfFieldItem iterHProfFieldItem : hprofClassObjectExtras.instanceFields) {
                        dosFile.writeInt(iterHProfFieldItem.stringId);
                        dosFile.writeByte(iterHProfFieldItem.type);
                    }
                    break;
                case HPROF_INSTANCE_DUMP: {
                    dosFile.writeInt(objectId);
                    dosFile.writeInt(threadSerial);
                    dosFile.writeInt(classId);
                    dosFile.writeInt(extras.length);
                    dosFile.write(extras);
                    break;
                }
                case HPROF_OBJECT_ARRAY_DUMP: {
                    dosFile.writeInt(objectId);
                    dosFile.writeInt(threadSerial);
                    dosFile.writeInt(items.size());
                    dosFile.writeInt(classId);

                    for (HProfFieldItem iv: items) {
                        dosFile.writeInt(iv.v4);
                    }
                    break;
                }
                case HPROF_PRIMITIVE_ARRAY_DUMP: {
                    dosFile.writeInt(objectId);
                    dosFile.writeInt(threadSerial);
                    dosFile.writeInt(length);
                    dosFile.writeByte(arraytype);
                    dosFile.write(extras);
                    break;
                }
                case HPROF_HEAP_DUMP_INFO:
                    dosFile.writeInt(heapType);
                    dosFile.writeInt(nameId);
                    break;
                case HPROF_PRIMITIVE_ARRAY_NODATA_DUMP:
                case HPROF_ROOT_FINALIZING:
                case HPROF_ROOT_REFERENCE_CLEANUP:
                case HPROF_UNREACHABLE:
                    size = 1;
                    break;
            }
        }
    }
    static int computeBasicLen(byte basicType) {
        int sizes[] = { -1, -1, 4, -1, 1, 2, 4, 8, 1, 2, 4, 8  };
        int maxSize = sizes.length;
        assert(basicType >= 0);
        if (basicType >= maxSize)
            return -1;
        return sizes[basicType];
    }

    class HProfHeapDumpSegment {
        public int TimeStamp;
        public ArrayList<RootObject> objects;
        public int NodeSize = 0;

        public HProfHeapDumpSegment(DataInputStream disFile, int rest, int TimeStamp) throws Exception {
            objects = new ArrayList<RootObject>();
            NodeSize = rest;
            this.TimeStamp = TimeStamp;
            while (rest > 0) {
                RootObject obj = new RootObject(disFile);
                rest -= obj.size;
                objects.add(obj);
            }
            assert (rest == 0);
        }
    }
    class HProfStackTrace {
        public int timeStamp;
        public int stackTraceSerialNumber;
        public int threadSerialNumber;
        public int numberOfFrames;
        public int []SeriesOfStackFrameIds;
    }

    class HProfStackFrame {
        public int timeStamp;
        public int stackFrameId;
        public int methodNameId;
        public int methodSignatureStringId;
        public int sourceFileNameStringId;
        public int classSerialNumber;
        public int lineNumber;
    }

    class HProfLoadClass {
        public int timeStamp;
        public int sn;
        public int objectId;
        public int stackTraceSerialNumber;
        public int classNameId;
    }

    class UnKnownNode {
        public int timeStamp;
        public byte[] data;
    }



    protected DataOutputStream dosFile;

    int extrasId = 1000000;


    byte[] HProfFileHeader = new byte[31];
    private HashMap<Integer, String> hprofTagStringMap = new HashMap<Integer, String>();
    private HashMap<Integer, HProfLoadClass> hprofTagLoadClassMap = new HashMap<Integer, HProfLoadClass>();
    private HashMap<Integer, HProfStackFrame> hprofTagStackFrameMap = new HashMap<Integer, HProfStackFrame>();
    private HashMap<Integer, HProfStackTrace> hprofTagStackTraceMap = new HashMap<Integer, HProfStackTrace>();
    private ArrayList<HProfHeapDumpSegment> hprofHeapDumpSegmentList = new ArrayList<HProfHeapDumpSegment>();


    private ArrayList<UnKnownNode> unknownNodes = new ArrayList<UnKnownNode>();
}
