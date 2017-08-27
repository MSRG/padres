package de.tum.msrg.itt;

import org.apache.zookeeper.ZooKeeper;

// connects to deployer and periodically sends stats about this broker
class StatsCollectorThread extends Thread {
    private volatile boolean stopped = false;

    public StatsCollectorThread(IttAgent ittAgent, String brokerStatsZNode, ZooKeeper zk) {

    }

    @Override
    public void run() {
        // connect to deployer and periodically send stats
        // uses zk, assumes /itt/brokers/broker_id/stats/param(1...n) exists
        // only uses setData to write stats to the related path
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }
}

