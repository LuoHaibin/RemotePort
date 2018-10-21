package xyz.luohaibin.rp.creature;

import xyz.luohaibin.util.Logger;
import xyz.luohaibin.util.StreamForward;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Port {
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    int port;
    Server server;
    Thread accept = new Thread(()->{
        while(!Thread.interrupted()){
            try {
                SocketChannel client = serverSocketChannel.accept();
                final int id = server.creature.getLinkId();
                long t = System.currentTimeMillis();
                Socket client_socket = client.socket();
                final InetAddress host = client_socket.getInetAddress();
                String threadTitle = String.format("[RP] Port %d - Server: %s - Client: %s",
                        port,
                        server.host.getHostName(),
                        client_socket.getInetAddress().getHostName());
                Logger.i(
                        String.format("客户端 id-%d:%s 通过端口 %d 请求连接服务器 id-%d %s",
                                id,
                                host.getHostName(),
                                port,
                                server.link_id,
                                server.host.getHostName()));
                new Thread(()->{
                    try {
                        SocketChannel link = server.getLink();
                        Logger.i("客户端 id-"+id+":"+host.getHostName()+" 获得链接, 耗时 "+ (System.currentTimeMillis()-t) +"ms");
                        StreamForward.forward(link, client);
                    } catch (InterruptedException e) {}
                    catch (IOException e) {}
                }, threadTitle).start();
            } catch (IOException e) {
                return;
            }
        }
    });

    Port(int port, Server server) throws IOException {
        serverSocketChannel.bind(new InetSocketAddress(port));
        this.port = port;
        this.server = server;
        accept.setName(String.format("[RP] Port %d - Server: %s", port, server.host.getHostName()));
        accept.start();
    }

    public void close() throws IOException {
        serverSocketChannel.close();
    }

    public int getLocalPort(){
        return serverSocketChannel.socket().getLocalPort();
    }
}
