package de.tum.msrg.itt;

import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.Sleep;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by pxsalehi on 19.09.16.
 */
public class BatchClient extends Client {
    enum CommandType {PAUSE, ADVERTISE, SUBSCRIBE, PUBLISH, INVALID}
    public static final String PAUSE_CMD_STR = "sleep";
    public static final String ADV_CMD_STR = "adv";
    public static final String PUB_CMD_STR = "pub";
    public static final String SUB_CMD_STR = "sub";
    // command line options
    public static final String ID_OPT_STR = "-i";
    public static final String BROKER_OPT_STR = "-b";
    public static final String MSG_RATE_OPT_STR = "-r";
    public static final String EXPECTED_PUB_OPT_STR = "-e";
    public static final String BATCH_FILE_OPT_STR = "-f";
    public static final String OUT_FILE_OPT_STR = "-o";

    class Command {
        private CommandType type = CommandType.INVALID;
        private int pauseLenSec = 0;
        private String cmdStr = "";

        public Command(int pauseLenSec) {
            this.type = CommandType.PAUSE;
            this.pauseLenSec = pauseLenSec;
        }

        public Command(CommandType type, String cmdStr) {
            this.type = type;
            this.cmdStr = cmdStr;
        }

        public String getCmdStr() {
            return cmdStr;
        }

        public void setCmdStr(String cmdStr) {
            this.cmdStr = cmdStr;
        }

        public int getPauseLenSec() {
            return pauseLenSec;
        }

        public void setPauseLenSec(int pauseLenSec) {
            this.pauseLenSec = pauseLenSec;
        }

        public CommandType getType() {
            return type;
        }

        public void setType(CommandType type) {
            this.type = type;
        }

        @Override
        public String toString() {
            switch(type) {
                case PUBLISH:
                case SUBSCRIBE:
                case ADVERTISE:
                    return type.name() + " " + cmdStr;
                case PAUSE:
                    return type.name() + " " + pauseLenSec + "sec";
                default:
                    return "Invalid command";
            }
        }
    }

    private List<Command> commands;
    private int msgRate;
    private int expectedPubCount = -1;
    private int receivedPubCount = 0;
    private Map<Long, Long> deliveryLatencies = new HashMap<>();

    public BatchClient(String id) throws ClientException {
        super(id);
    }

    public void readCommands(String filepath) throws IOException {
        try(BufferedReader in = new BufferedReader(new FileReader(filepath))) {
            commands = in.lines().map(this::parseCommand).collect(Collectors.toList());
        }
    }

    private Command parseCommand(String str) {
        if(str.toLowerCase().startsWith(PAUSE_CMD_STR.toLowerCase()))
            return new Command(Integer.parseInt(str.substring(PAUSE_CMD_STR.length()).trim()));
        if(str.toLowerCase().startsWith(ADV_CMD_STR.toLowerCase()))
            return new Command(CommandType.ADVERTISE, str.substring(ADV_CMD_STR.length()).trim());
        if(str.toLowerCase().startsWith(PUB_CMD_STR.toLowerCase()))
            return new Command(CommandType.PUBLISH, str.substring(PUB_CMD_STR.length()).trim());
        if(str.toLowerCase().startsWith(SUB_CMD_STR.toLowerCase()))
            return new Command(CommandType.SUBSCRIBE, str.substring(SUB_CMD_STR.length()).trim());
        throw new RuntimeException("Invalid command " + str);
    }

    @Override
    public void processMessage(Message msg) {
        super.processMessage(msg);
        if(msg.getType().equals(MessageType.PUBLICATION)) {
            long recTime = new Date().getTime();
            long pubTime = ((PublicationMessage)msg).getPublication().getTimeStamp().getTime();
            deliveryLatencies.put(recTime, recTime - pubTime);
            ++receivedPubCount;
            System.out.println(String.format("%s received a publication, id=%s, pub'd=%d, rec'd=%d, latency=%d ms",
                                getCurrentTimestamp(), msg.getMessageID(), pubTime, recTime, (recTime - pubTime)));
        }
    }

    public void start() throws ParseException, ClientException {
        int pause = msgRate == 0 ? 0 : 1000 / msgRate;
        for(Command cmd: commands) {
            System.out.println(cmd);
            switch (cmd.type) {
                case PAUSE:
                    System.out.println(getCurrentTimestamp() + " Pausing for " + cmd.getPauseLenSec());
                    Sleep.sleep(cmd.getPauseLenSec() * 1000);
                    break;
                case ADVERTISE:
                    System.out.println(getCurrentTimestamp() + " Sending advertisement " + cmd.getCmdStr());
                    advertise(MessageFactory.createAdvertisementFromString(cmd.getCmdStr()));
                    break;
                case PUBLISH:
                    System.out.println(getCurrentTimestamp() + " Sending publication " + cmd.getCmdStr());
                    Publication pub = MessageFactory.createPublicationFromString(cmd.getCmdStr());
                    pub.setTimeStamp(new Date());
                    publish(pub);
                    break;
                case SUBSCRIBE:
                    System.out.println(getCurrentTimestamp() + " Sending subscription " + cmd.getCmdStr());
                    subscribe(MessageFactory.createSubscriptionFromString(cmd.getCmdStr()));
                    break;
                default:
                    throw new RuntimeException("Cannot handle command " + cmd);
            }
            // pause between each command to respect the msg rate
            Sleep.sleep(pause);
        }
    }

    public void writeOutput(String filepath) throws IOException {
        try(BufferedWriter out = new BufferedWriter((new FileWriter(filepath)))) {
            out.write("received_pubs:" + receivedPubCount + "\n");
            out.write("expected_pubs:" + expectedPubCount + "\n");
            out.write("delivery_latencies:" + deliveryLatencies + "\n");
            out.flush();
        }
    }

    public int getMsgRate() {
        return msgRate;
    }

    public void setMsgRate(int msgRate) {
        this.msgRate = msgRate;
    }

    public int getExpectedPubCount() {
        return expectedPubCount;
    }

    public void setExpectedPubCount(int expectedPubCount) {
        this.expectedPubCount = expectedPubCount;
    }

    public static String getCurrentTimestamp() {
        return "" + System.currentTimeMillis();
    }

    public static void main(String[] args) throws ClientException, IOException, ParseException {
        if(args.length < 6*2) {
            System.err.println("Some parameters are missing!");
            System.err.println("Usage: client -i id -b broker -f batch_file -o out_file -r msgRate -e expectedPubCount");
            System.exit(1);
        }
        Map<String, String> opts = new HashMap<>();
        for(int i = 0; i < args.length; i += 2)
            opts.put(args[i], args[i+1]);
        String id = opts.get(ID_OPT_STR);
        String broker = opts.get(BROKER_OPT_STR);
        String cmdFile = opts.get(BATCH_FILE_OPT_STR);
        int msgRate = Integer.parseInt(opts.get(MSG_RATE_OPT_STR));
        int expectedPubs = Integer.parseInt(opts.get(EXPECTED_PUB_OPT_STR));
        String outFile = opts.get(OUT_FILE_OPT_STR);
        System.out.println(getCurrentTimestamp() + " Starting client...");
        BatchClient bc = new BatchClient(id);
        bc.setMsgRate(msgRate);
        bc.setExpectedPubCount(expectedPubs);
        System.out.println(getCurrentTimestamp() + " Parsing command file...");
        bc.readCommands(cmdFile);
        System.out.println(getCurrentTimestamp() + " Connecting to broker " + broker);
        bc.connect(broker);
        System.out.println(getCurrentTimestamp() + " Executing commands...");
        bc.start();
        System.out.println(getCurrentTimestamp() + " Writing output to " + outFile);
        bc.writeOutput(outFile);
        System.out.println(getCurrentTimestamp() + " All done!");
        bc.shutdown();
    }
}
