package xyz.luohaibin.rp.creature;

import xyz.luohaibin.util.Logger;
import xyz.luohaibin.util.StreamForward;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class Port {
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    final int port;
    final Server server;
    final Thread accept;

    Port(int port, Server server) throws IOException {
        serverSocketChannel.bind(new InetSocketAddress(port));
        this.port = port;
        this.server = server;
        accept = new Thread(()->{
            while(!Thread.interrupted()){
                try {
                    SocketChannel client = serverSocketChannel.accept();
                    final int id = server.creature.getLinkId();
                    long t = System.currentTimeMillis();
                    Socket client_socket = client.socket();
                    final InetAddress client_host = client_socket.getInetAddress();
                    String threadTitle = String.format("[RP] Port %d - Server: %s - Client: %s",
                            port,
                            server.host.getHostName(),
                            client_host.getHostName());

                    ExecutorService pool = Executors.newCachedThreadPool(r->new Thread(r, threadTitle));
                    Logger.i(
                            String.format("客户端 id-%d:%s 通过端口 %d 请求连接服务器 id-%d %s",
                                    id,
                                    client_host.getHostName(),
                                    port,
                                    server.link_id,
                                    server.host.getHostName()));
                    pool.submit(()->{
                        try {
                            SocketChannel link = server.getLink(client);
                            Logger.i("客户端 id-"+id+":"+client_host.getHostName()+" 获得链接, 耗时 "+ (System.currentTimeMillis()-t) +"ms");
                            StreamForward.mutualWithFinalize(link, client);
                        } catch (InterruptedException ignored) {}
                        catch (IOException e) {
                            Logger.i("客户端链接已关闭");
                        }
                        catch (TimeoutException e) {
                            Logger.i("客户端 id-"+id+":"+client_host.getHostName()+" 获取链接超时");
                        }
                    });
                } catch (IOException e) {
                    return;
                }
            }
        }, String.format("[RP] Port %d - Server: %s", port, server.host.getHostName()));
        accept.start();
    }

    public void close() throws IOException {
        serverSocketChannel.close();
    }

    public int getLocalPort(){
        return serverSocketChannel.socket().getLocalPort();
    }
}
