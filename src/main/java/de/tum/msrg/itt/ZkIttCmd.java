package de.tum.msrg.itt;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pxsalehi on 04.08.16.
 */

// represents an incremental topology transformation command deployed via zookeeper
public class ZkIttCmd {
    public enum Type {SHIFT, MOVE}
    public enum Status {NONE, STARTED, FINISHED, ERROR}

    public Type type;
    public List<String> operands = new ArrayList<>();
    public static final String SEPARATOR = "#";

    public ZkIttCmd() {}

    public ZkIttCmd(Type type, List<String> operands) {
        this.type = type;
        this.operands = operands;
    }

    public static ZkIttCmd parse(byte[] cmd) throws IttException {
        return parse(IttUtility.ZK.byteToString(cmd));
    }

    public static ZkIttCmd parse(String cmd) throws IttException {
        String[] toks = cmd.split(SEPARATOR);
        if (toks.length < 4)
            throw new IttException("Cannot parse ZK command " + cmd + ". Not enough arguments!");
        if (!toks[0].equalsIgnoreCase("shift") && !toks[0].equalsIgnoreCase("move"))
            throw new IttException("Cannot parse ZK command " + cmd + ". Invalid command!");
        ZkIttCmd zkCmd = new ZkIttCmd();
        zkCmd.type = Type.valueOf(toks[0].toUpperCase());
        for(int i = 1; i < toks.length; i++)
            zkCmd.operands.add(toks[i]);
        return zkCmd;
    }

    @Override
    public String toString() {
        String str = type.name() + SEPARATOR;
        for(int i = 0; i < operands.size(); i++) {
            str += operands.get(i);
            if(i != operands.size() - 1)
                str += SEPARATOR;
        }
        return str;
    }

    public byte[] toBytes() {
        return IttUtility.ZK.StringToByte(toString());
    }

    public static void main(String[] args) throws IttException {
        ZkIttCmd cmd =
                ZkIttCmd.parse("shift#localhost:234/broker1#localhost:878/broker2#34.121.98.11:888/broker3");
        System.out.println(cmd);
    }
}
