package xyz.luohaibin.rp.creature;

import xyz.luohaibin.util.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

public class Creature{
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

    final LinkedList<Server> servers = new LinkedList<>();
    {
        new Thread(()->{
            try{
                while(!Thread.interrupted()){
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
                    Thread.sleep(60000);
                }
            } catch (InterruptedException e) {}
        }, "[RP] check connection").start();
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
