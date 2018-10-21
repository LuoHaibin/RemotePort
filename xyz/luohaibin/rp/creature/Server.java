package xyz.luohaibin.rp.creature;

import xyz.luohaibin.util.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

class Server {
    static final int ALIVE = -1;
    static final int NEWLINK = -2;
    static final int ERROR = -3;
    static final int SUCCESS = -4;

    int link_id;

    long lastestCorrespond = System.currentTimeMillis();
    Creature creature;
    InetAddress host;
    int remotePost;
    SocketChannel socketChannel;
    DataInputStream in;
    DataOutputStream out;
    final LinkedList<Port> ports = new LinkedList<>();
    final LinkedList<SocketChannel> links = new LinkedList<>();
    boolean firstCmd;
    Thread inputThread = new Thread(()->{
        try {
            while(!Thread.interrupted()){
                int cmd = in.readInt();
                lastestCorrespond = System.currentTimeMillis();
                if(cmd==ALIVE){
                    out(ALIVE);
                }else if(cmd == NEWLINK && !firstCmd) {
                    boolean isSucceeded = false;
                    synchronized (creature.servers){
                        LinkedList<Server> servers = creature.servers;
                        for(Server server:servers){
                            if(server!= Server.this
                                    && host != null && host.equals(server.host)){
                                servers.remove(Server.this);
                                out(SUCCESS);
                                Logger.i("id-"+link_id+":"+host.getHostName()+" 加入 id-"+server.link_id+" 端口储备");
                                synchronized (server.links){
                                    server.links.add(socketChannel);
                                    server.links.notify();
                                }
                                return;
                            }
                        }
                    }
                    out(ERROR);
                    close();
                }else if(cmd>=0 && cmd<65536) {
                    try{
                        Port p = new Port(cmd, this);
                        synchronized (ports){
                            ports.add(p);
                        }
                        out(p.getLocalPort());
                        Logger.i("id-"+link_id+":"+host.getHostName()+" 绑定端口 "+cmd);
                    }catch(BindException e){
                        out(cmd+65536);
                        Logger.i("id-"+link_id+":"+host.getHostName()+" 绑定端口 "+cmd+" 失败");
                    }
                }
                firstCmd = true;
            }
        } catch (IOException e) {
            close();
        }
    });

    Server(Creature creature, SocketChannel socketChannel) throws IOException{
        link_id = creature.getLinkId();
        this.creature = creature;
        this.socketChannel = socketChannel;
        Socket socket = socketChannel.socket();
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        host = socket.getInetAddress();
        remotePost = socket.getPort();
        inputThread.setName("[RD] InputThread - " + host.getHostName());
        inputThread.start();
    }

    SocketChannel getLink() throws InterruptedException{
        newLink();
        Logger.d("id-"+link_id+" 请求新链接");
        synchronized (links){
            while (links.isEmpty())
                links.wait();
            return links.pop();
        }
    }

    public void newLink() {
        out(NEWLINK);
    }

    synchronized void out(int code){
        try {
            long t = System.currentTimeMillis();
            Logger.d("id-"+link_id+" 发送code "+code);
            out.writeInt(code);
            Logger.d("id-"+link_id+" 发送code "+code+" - "+(System.currentTimeMillis()-t));
        } catch (IOException e) {
            close();
        }
    }

    void close() {
        synchronized (creature.servers){
            creature.servers.remove(this);
        }
        synchronized (ports){
            for (Port port: ports) {
                try {
                    port.close();
                } catch (IOException e) {}
            }
        }
        synchronized (links){
            SocketChannel s;
            while((s = links.poll())!=null){
                try {
                    s.close();
                } catch (IOException e) {}
            }
        }
        inputThread.interrupt();
        Logger.i("id-"+link_id+":"+host.getHostName()+" 断开服务器");
    }
}
