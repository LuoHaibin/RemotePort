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
import java.util.concurrent.TimeoutException;

class Server {
    static final int ALIVE = -1;
    static final int NEWLINK = -2;
    static final int ERROR = -3;
    static final int SUCCESS = -4;

    int link_id;

    long lastestCorrespond = System.currentTimeMillis();
    final Creature creature;
    final InetAddress host;
    final int remotePost;
    final SocketChannel socketChannel;
    final DataInputStream in;
    final DataOutputStream out;
    final LinkedList<Port> ports = new LinkedList<>();
    final LinkedList<SocketChannel> links = new LinkedList<>();
    final Thread inputThread;

    Server(Creature creature, SocketChannel socketChannel) throws IOException{
        link_id = creature.getLinkId();
        this.creature = creature;
        this.socketChannel = socketChannel;
        Socket socket = socketChannel.socket();
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        host = socket.getInetAddress();
        remotePost = socket.getPort();
        inputThread = new Thread(()->{
            try {
                boolean firstCmd = true;
                while(!Thread.interrupted()){
                    int cmd = in.readInt();
                    lastestCorrespond = System.currentTimeMillis();
                    if(cmd==ALIVE){
                        out(ALIVE);
                    }else if(cmd == NEWLINK && firstCmd) {
                        initNewLink();
                        return;
                    }else if(cmd>=0 && cmd<65536) {
                        bind(cmd);
                    }
                    firstCmd = false;
                }
            } catch (IOException e) {
                close();
            }
        }, "[RD] InputThread - " + host.getHostName());
        inputThread.start();
    }

    private void bind(int port) throws IOException{
        try{
            Port p = new Port(port, this);
            synchronized (ports){
                ports.add(p);
            }
            out(p.getLocalPort());
            Logger.i("id-"+link_id+":"+host.getHostName()+" 绑定端口 "+port);
        }catch(IOException e){
            if (e instanceof BindException){
                out(port+65536);
                Logger.i("id-"+link_id+":"+host.getHostName()+" 绑定端口 "+port+" 失败");
            }else {
                throw e;
            }
        }
    }

    SocketChannel getLink(SocketChannel client) throws InterruptedException, TimeoutException, IOException {
        newLink();
        Logger.d("id-"+link_id+" 请求新链接");
        long t = System.currentTimeMillis();
        synchronized (links){
            SocketChannel link;
            while (links.isEmpty()){
                links.wait(30000);
                if (System.currentTimeMillis()-t > 30000 && links.isEmpty()){
                    throw new TimeoutException();
                }
            }
            if (!client.isConnected()){
                throw new IOException();
            }
            link = links.pop();
            return link;
        }
    }

    private void newLink() {
        out(NEWLINK);
    }

    private void initNewLink() throws IOException{
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
        throw new IOException();
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
