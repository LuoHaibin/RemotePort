package xyz.luohaibin.rp.creature;

import xyz.luohaibin.util.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Creature{
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

    final LinkedList<Server> servers = new LinkedList<>();
    {
        ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "[RP] check connection");
            thread.setDaemon(true);
            return thread;
        });
        pool.scheduleWithFixedDelay(()->{
            long time = System.currentTimeMillis();
            synchronized (servers){
                for(int i=0;i<servers.size();i++){
                    Server server = servers.get(i);
                    if(server.lastestCorrespond<time-10000){
                        server.close();
                        servers.remove(server);
                        i--;
                    }
                }
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    public Creature(int port) throws IOException {
        serverSocketChannel.bind(new InetSocketAddress(port));
        new Thread(()->{
            Logger.i("代理服务器启动端口 "+port);
            while(true){
                try {
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    synchronized (servers){
                        Server server = new Server(this, socketChannel);
                        servers.add(server);
                        Logger.i("id-"+server.link_id+":"+server.host.getHostName()+" 连接服务器");
                    }
                } catch (IOException e) {}
            }
        }, "[RP] Creature Server").start();
    }

    int _n=0;
    synchronized int getLinkId() {
        return _n++;
    }

    static public void main(String[] agrs) throws Exception {
        Creature server = new Creature(4959);
    }
}
