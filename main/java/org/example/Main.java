package org.example;

import java.io.IOException;
import org.apache.zookeeper.KeeperException;
import org.apache.log4j.*;

public class Main {
    public static void main( String[] args ) throws IOException, InterruptedException, KeeperException
    {
        LeaderElection leaderElection  = new LeaderElection();
        leaderElection.connectToZookeper();
        leaderElection.volunteerForLeadership();
        leaderElection.reelectLeader();
        leaderElection.run();
        leaderElection.close();
        System.out.println("Disconnesso da Zookeeper, uscita dall'applicazione :)");
    }
}