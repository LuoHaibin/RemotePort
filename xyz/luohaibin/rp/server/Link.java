package xyz.luohaibin.rp.server;

import xyz.luohaibin.util.Logger;
import xyz.luohaibin.util.StreamForward;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

public class Link {
    SocketChannel socketChannel = SocketChannel.open();
    Socket socket = socketChannel.socket();

    public Link(String host, int port, int localPort) throws IOException {
        socketChannel.connect(new InetSocketAddress(host, port));
        new DataOutputStream(socket.getOutputStream()).writeInt(Server.NEWLINK);
        Logger.d("完成发送链接");
        if (new DataInputStream(socket.getInputStream()).readInt()== Server.SUCCESS){
            Logger.d("开始准备本地链接");
            SocketChannel local = SocketChannel.open();
            local.connect(new InetSocketAddress("127.0.0.1", localPort));
            Logger.d("开始转发流量");
            StreamForward.forward(local, socketChannel);
        }
    }
}
