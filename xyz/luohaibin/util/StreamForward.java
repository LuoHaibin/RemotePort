package xyz.luohaibin.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StreamForward {
    private static ExecutorService pool = Executors.newCachedThreadPool(r->new Thread(r, "xyz.luohaibin.util.StreamForward"));

    public static Runnable toFinalize(final Socket s1, final Socket s2) throws IOException{
        return () -> {
            try{
                if(!s1.isClosed()) s1.shutdownInput();
                if(!s2.isClosed()) s2.shutdownOutput();
            } catch (IOException e) {}
        };
    }
    public static Runnable toFinalize(final Channel sc1, final Channel sc2) throws IOException{
        return () -> {
            try{
                sc1.close();
                sc2.close();
            } catch (IOException e) {}
        };
    }

    public static void forward(final Socket s1, final Socket s2) throws IOException{
        InputStream in1 = s1.getInputStream();
        OutputStream out1 = s1.getOutputStream();
        InputStream in2 = s2.getInputStream();
        OutputStream out2 = s2.getOutputStream();

        forward(in1, out2, toFinalize(s1, s2));
        forward(in2, out1, toFinalize(s2, s1));
    }

    public static void forward(final ByteChannel sc1, final ByteChannel sc2) throws IOException{
        forward(sc1, sc2, toFinalize(sc1, sc2));
        forward(sc2, sc1, toFinalize(sc2, sc1));
    }

    public static void forwardOnlyRun(ByteChannel in,ByteChannel out, Runnable callback){
        try{
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            while(in.read(buffer)!=-1){
                buffer.flip();
                out.write(buffer);
                buffer.clear();
                Thread.sleep(1);
            }
        }catch(Exception e){}finally{
            callback.run();
        }
    }

    public static void forward(ByteChannel from,ByteChannel to, Runnable callback){
        pool.submit(()->forwardOnlyRun(from, to, callback));
    }

    public static void forwardOnlyRun(InputStream in,OutputStream out, Runnable callback){
        try{
            int readByte;

            while((readByte=in.read())!=-1){
                out.write(readByte);
                out.flush();
            }
        }catch(Exception e){}finally{
            callback.run();
        }
    }

    public static void forward(InputStream in,OutputStream out, Runnable callback){
        pool.submit(()->forwardOnlyRun(in, out, callback));
    }

    public static void log(OutputStream logOutput, InputStream in, OutputStream out, Runnable callback){
        pool.submit(()->{
            try{
                int readByte;

                while((readByte=in.read())!=-1){
                    logOutput.write(readByte);
                    out.write(readByte);
                    out.flush();
                }
            }catch(Exception ignored){}
            finally{
                callback.run();
            }
        });
    }
}