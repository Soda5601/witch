package me.soda.magictcp;

import me.soda.magictcp.packet.DisconnectPacket;
import me.soda.magictcp.packet.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Connection {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private DisconnectPacket disconnectPacket;
    private final boolean compress;

    public Connection(Socket socket, boolean compress) throws IOException {
        this.compress = compress;
        connect(socket);
    }

    public Connection(boolean compress) {
        this.compress = compress;
    }

    public void connect(Socket socket) throws IOException {
        disconnectPacket = null;
        this.socket = socket;
        initIO();
    }

    private void initIO() throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void forceClose() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close(DisconnectPacket.Reason reason) {
        close(reason, "");
    }

    public void close(DisconnectPacket.Reason reason, String message) {
        if (isConnected())
            send(new DisconnectPacket(reason, message));
        else
            forceClose();
    }

    public <T> void send(T data) {
        try {
            Packet<T> packet = new Packet<>(data);
            if (compress) packet.compress();
            out.writeObject(packet);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //todo: event system
    @SuppressWarnings("unchecked")
    public <T> T read(Class<T> tClass) throws Exception {
        Object o = in.readObject();
        if (!(o instanceof Packet<?>)) throw new Exception("Not a MagicTcp Packet");
        T o2 = ((Packet<T>) o).get();
        if (o2 instanceof DisconnectPacket o3) {
            disconnectPacket = o3;
            forceClose();
        }
        return tClass.cast(o2);
    }

    public boolean isConnected() {
        return socket.isConnected() && !socket.isClosed();
    }

    public InetSocketAddress getRemoteSocketAddress() {
        return (InetSocketAddress) socket.getRemoteSocketAddress();
    }

    public DisconnectPacket getDisconnectPacket() {
        return disconnectPacket;
    }
}