package xyz.luohaibin.rp.server;

import xyz.luohaibin.util.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Server extends Socket{
    static final int ALIVE = -1;
    static final int NEWLINK = -2;
    static final int ERROR = -3;
    static final int SUCCESS = -4;

    String host;
    int port;
    DataInputStream in;
    DataOutputStream out;
    long lastestCorrespond = System.currentTimeMillis();
    int localPort;
    Thread aliveChecker = new Thread(()->{
        try {
            while(!Thread.interrupted()){
                out(ALIVE);
                if(lastestCorrespond<System.currentTimeMillis()-10000)
                    close();
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {}
    });

    Thread inputThread = new Thread(()->{
        try {
            while(!Thread.interrupted()){
                int cmd = in.readInt();
                lastestCorrespond = System.currentTimeMillis();
                switch (cmd){
                    case NEWLINK:
                        Logger.i("获得新链接请求");
                        new Link(host, port, localPort);
                        break;
                    case ERROR:
                        Logger.f("发生错误");
                        break;
                }
                if(cmd>0){
                    if(cmd<65536)
                        Logger.i("本地端口 "+localPort+" 绑定服务器 "+host+":"+port+" 端口 "+cmd);
                    else if(cmd==65536)
                        Logger.e("绑定服务器 "+host+":"+port+" 端口失败");
                    else
                        Logger.e("绑定服务器 "+host+":"+port+" 端口 "+(cmd-65536)+" 失败: 服务器端口 "+(cmd-65536)+" 正在使用中");
                }
            }
        } catch (IOException e) {
            close();
        }
    });
    public Server(String host, int port, int localPort) throws IOException {
        super(host, port);
        this.host=host;
        this.port=port;
        this.localPort =localPort;
        in = new DataInputStream(getInputStream());
        out = new DataOutputStream(getOutputStream());
        aliveChecker.start();
        inputThread.start();
    }

    public Server bind(int port){
        assert port>=0 && port<65536;
        out(port);
        return this;
    }

    private static void deamon(String host, int port, int localPort, int remotePort) {
        deamon(host, port, localPort, remotePort, 5);
    }
    private static void deamon(String host, int port, int localPort, int remotePort, int times) {
        try{
            new Server(host, port, localPort).bind(remotePort).setCallback(()->{
                if(times>0)
                    deamon(host, port, localPort, remotePort, times-1);
                else{
                    try {
                        Thread.sleep(600000);
                        deamon(host, port, localPort, remotePort, 5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            Logger.i("服务器 "+host+":"+port+" 不可用");
            try {
                Thread.sleep(600000);
                deamon(host, port, localPort, remotePort, 5);
            } catch (InterruptedException e2) {
                e2.printStackTrace();
            }
        }
    }

    public static void main(String[] agrs) throws Exception{
//        deamon("103.11.89.69", 4959, 3389, 1080);
//        deamon("103.11.89.69", 4959, 8069, 8069);
        deamon("luohaibin.xyz", 4959, 3389, 3390);
//        deamon("luohaibin.xyz", 4959, 8069, 8069);
//        deamon("127.0.0.1", 4959, 80, 81);
        //new Server("127.0.0.1", 4959, 80).bind(81);

        //new Server("127.0.0.1", 4959, 80).bind(81);
    }

    synchronized void out(int code){
        try {
            long t = System.currentTimeMillis();
            Logger.d("发送 "+code);
            out.writeInt(code);
            Logger.d("发送 "+code+" - "+(System.currentTimeMillis()-t));
        } catch (IOException e) {
            close();
        }
    }

    Runnable runnable=null;
    void setCallback(Runnable run){
        runnable=run;
    }

    @Override
    public void close(){
        try {
            super.close();
            inputThread.interrupt();
            aliveChecker.interrupt();
            Logger.i("本地端口 "+localPort+" 与服务器 "+host+":"+port+" 断开通信");
        } catch (IOException e) {}
        catch (Exception e){}
        if(runnable!=null)
            runnable.run();
    }
}
