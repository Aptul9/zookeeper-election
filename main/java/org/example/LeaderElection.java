package org.example;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class LeaderElection implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "zoo1:2181"; //localhost o zoo1
    private ZooKeeper zooKeeper;
    private static final int SESSION_TIMEOUT = 3000;
    private static final String ELECTION_NAMESPACE = "/election";
    private String currentZNodeName;

    public void volunteerForLeadership() throws KeeperException, InterruptedException {
        String zNodePrefix = ELECTION_NAMESPACE + "/c_";
        String zNodeFullPath = zooKeeper.create(zNodePrefix, new byte[] {}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("ZNODE_NAME: " + zNodeFullPath);
        this.currentZNodeName = zNodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
    }

    public void reelectLeader() throws KeeperException, InterruptedException {
        Stat predecessorStat = null;
        String predecessorZnodeName = "";
        while (predecessorStat == null) {
            List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);
            Collections.sort(children);
            String smallestChild = children.get(0);
            if (smallestChild.equals(currentZNodeName)) {
                Thread.sleep (2000);
                System.out.println("Sono il coordinatore :)");
            } else {
                Thread.sleep(2000);
                System.out.println("Non sono il coordinatore :)");
                int a = (int) Math.floor(Math.random() * (100 - 1 + 1) + 1);
                int b = (int) Math.floor(Math.random() * (100 - 1 + 1) + 1);
                int somma = a + b;
                System.out.println("Il coordinatore mi ha detto di fare " + a + " + " + b + " = " + somma);
                System.out.println("Adesso rimango in attesa");
                int predecessorIndex = Collections.binarySearch(children, currentZNodeName) - 1;
                predecessorZnodeName = children.get(predecessorIndex);
                predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZnodeName, this);
            }
        }
        System.out.println("Osservo e attendo: " + predecessorZnodeName);
        System.out.println();
    }

    public void watchTargetZNode() throws KeeperException, InterruptedException {
        Stat stat = zooKeeper.exists(ELECTION_NAMESPACE, this);
        if (stat == null) {
            return;
        }
        byte[] data = zooKeeper.getData(ELECTION_NAMESPACE, this, stat);
        List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, this);
        System.out.println("Data: " + new String(data) + " dei figli: " + children);
    }

    public void connectToZookeper() throws IOException{
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    public void process(WatchedEvent event) {
        switch(event.getType()) {
            case None:
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Connessione riuscita a Zookeeper!");
                } else {
                    synchronized (zooKeeper) {
                        System.out.println("Disconnesso da Zookeeper!");
                        zooKeeper.notifyAll();
                    }
                } break;
            case NodeDeleted:
                try {
                    reelectLeader();
                } catch(KeeperException e) {
                } catch(InterruptedException e) {
                }
                System.out.println(ELECTION_NAMESPACE + " è stato eliminato!!");
            case NodeCreated:
                System.out.println(ELECTION_NAMESPACE + " è stato creato!!");
            case NodeDataChanged:
                System.out.println(ELECTION_NAMESPACE + " è stato cambiato!!");
            case NodeChildrenChanged:
                System.out.println(ELECTION_NAMESPACE + " figli cambiati!!");
            default:
                break;
        }

        try {
            watchTargetZNode();
        } catch (KeeperException e) {e.printStackTrace();}
        catch (InterruptedException e) {e.printStackTrace();}
    }
}