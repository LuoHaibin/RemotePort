package xyz.luohaibin.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamForward {
    private static ExecutorService pool = Executors.newCachedThreadPool(r->new Thread(r, "xyz.luohaibin.util.StreamForward"));

    public static Runnable toFinalize(final Socket s1, final Socket s2) {
        return () -> {
            try{
                if(!s1.isClosed()) s1.shutdownInput();
                if(!s2.isClosed()) s2.shutdownOutput();
            } catch (IOException e) {}
        };
    }
    public static Runnable toFinalize(final Channel sc1, final Channel sc2) {
        return () -> {
            try{
                sc1.close();
                sc2.close();
            } catch (IOException e) {}
        };
    }

    public static void mutualWithFinalize(Socket s1, Socket s2) throws IOException{
        mutual(s1, s2, toFinalize(s1, s2));
    }

    public static void mutualWithFinalize(ByteChannel sc1, ByteChannel sc2) {
        mutual(sc1, sc2, toFinalize(sc1, sc2));
    }

    public static void mutual(Socket s1, Socket s2, Runnable callback) throws IOException{
        RunOnce runOnce = new RunOnce(callback);

        InputStream in1 = s1.getInputStream();
        OutputStream out1 = s1.getOutputStream();
        InputStream in2 = s2.getInputStream();
        OutputStream out2 = s2.getOutputStream();

        parallel(in1, out2, runOnce);
        parallel(in2, out1, runOnce);
    }

    public static void mutual(ByteChannel sc1, ByteChannel sc2, Runnable callback) {
        RunOnce runOnce = new RunOnce(callback);
        parallel(sc1, sc2, runOnce);
        parallel(sc2, sc1, runOnce);
    }

    public static void parallel(InputStream in,OutputStream out, Runnable callback){
        pool.submit(()->run(in, out, callback));
    }

    public static void parallel(ReadableByteChannel from,WritableByteChannel to, Runnable callback){
        pool.submit(()->run(from, to, callback));
    }

    public static void run(InputStream in,OutputStream out, Runnable callback){
        run(Channels.newChannel(in), Channels.newChannel(out), callback);
    }

    public static void run(ReadableByteChannel in, WritableByteChannel out, Runnable callback){
        try{
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            while(in.read(buffer)!=-1){
                buffer.flip();
                out.write(buffer);
                buffer.clear();
                Thread.sleep(1);
            }
        }catch(Exception ignored){}finally{
            callback.run();
        }
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

    private static class RunOnce implements Runnable{
        final Runnable entity;
        AtomicBoolean hadCalled = new AtomicBoolean();

        public RunOnce(Runnable entity) {
            this.entity = entity;
        }

        @Override
        public void run() {
            if (!hadCalled.getAndSet(true)){
                entity.run();
            }
        }
    }
}