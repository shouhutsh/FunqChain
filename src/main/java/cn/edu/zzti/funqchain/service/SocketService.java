package cn.edu.zzti.funqchain.service;

import cn.edu.zzti.funqchain.exception.BizException;
import cn.edu.zzti.funqchain.model.Block;
import cn.edu.zzti.funqchain.model.Message;
import cn.edu.zzti.funqchain.util.StrUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SocketService {

    @Value("${web.socket.server.port}")
    private int WEB_SOCKET_SERVER_PORT;

    @Autowired
    private BlockService blockService;

    private static List<WebSocket> sockets = new ArrayList<>();

    private enum MessageType {
        QUERY_LAST_BLOCK,
        QUERY_ALL_BLOCK,
        RESPONSE_LAST_BLOCK,
        RESPONSE_ALL_BLOCK;

        static MessageType byType(String type) {
            for (MessageType t : values()) {
                if (StrUtils.equals(t.name(), type)){
                    return t;
                }
            }
            throw new BizException("没有这种类型！");
        }
    }

    @PostConstruct
    public void init(){
        final WebSocketServer socketServer = new WebSocketServer(new InetSocketAddress(WEB_SOCKET_SERVER_PORT)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                sockets.add(conn);
                write(conn, buildQueryLastBlockMessage());
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                sockets.remove(conn);
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                handlerMessage(conn, message);
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {

            }

            @Override
            public void onStart() {

            }
        };
        socketServer.start();
    }

    public boolean connect(String peer) {
        try {
            WebSocketClient socketClient = new WebSocketClient(new URI(peer)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    sockets.add(this);
                    write(this, buildQueryLastBlockMessage());
                }

                @Override
                public void onMessage(String message) {
                    handlerMessage(this, message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    sockets.remove(this);
                }

                @Override
                public void onError(Exception ex) {

                }
            };
            socketClient.connect();
            return true;
        } catch (URISyntaxException e) {
            throw new BizException("创建WebSocket连接失败！");
        }
    }

    public List<WebSocket> listSockets() {
        return sockets;
    }

    public void broadcast(String message) {
        for (WebSocket s : sockets) {
            write(s, message);
        }
    }

    public String buildQueryLastBlockMessage(){
        return JSONObject.toJSONString(new Message(MessageType.QUERY_LAST_BLOCK.name()));
    }

    public String buildQueryAllBlockMessage(){
        return JSONObject.toJSONString(new Message(MessageType.QUERY_ALL_BLOCK.name()));
    }

    public String buildResponseLastBlockMessage(){
        return JSONObject.toJSONString(
                new Message(
                        MessageType.RESPONSE_LAST_BLOCK.name(),
                        JSONObject.toJSONString(getLocalLastBlock())));
    }

    public String buildResponseAllBlockMessage(){
        return JSONObject.toJSONString(
                new Message(
                        MessageType.RESPONSE_ALL_BLOCK.name(),
                        JSONArray.toJSONString(getLocalAllBlock())));
    }

    private void handlerMessage(WebSocket webSocket, String message) {
        Message reqMsg = JSONObject.parseObject(message, Message.class);
        switch (MessageType.byType(reqMsg.getType())) {
            case QUERY_LAST_BLOCK:
                handleQueryLastBlock(webSocket);
                break;
            case QUERY_ALL_BLOCK:
                handleQueryAllBlock(webSocket);
                break;
            case RESPONSE_LAST_BLOCK:
                handleResponseLastBlock(webSocket, JSONObject.parseObject(reqMsg.getMessage(), Block.class));
                break;
            case RESPONSE_ALL_BLOCK:
                handleResponseAllBlock(webSocket, JSONArray.parseArray(reqMsg.getMessage(), Block.class));
                break;
            default:
                throw new BizException("无法处理该类型消息！");
        }
    }

    private void handleQueryLastBlock(WebSocket webSocket){
        write(webSocket, buildResponseLastBlockMessage());
    }

    private void handleQueryAllBlock(WebSocket webSocket){
        write(webSocket, buildResponseAllBlockMessage());
    }

    private void handleResponseLastBlock(WebSocket webSocket, Block otherLastBlock){
        Block localLastBlock = getLocalLastBlock();
        if (otherLastBlock.getIndex() > localLastBlock.getIndex()) {
            if (StrUtils.equals(otherLastBlock.getPreHash(), localLastBlock.getHash())) {
                if (blockService.addBlock(otherLastBlock)) {
                    broadcast(buildResponseLastBlockMessage());
                }
            } else {
                broadcast(buildQueryAllBlockMessage());
            }
        } else if (otherLastBlock.getIndex() < localLastBlock.getIndex()) {
            write(webSocket, buildResponseAllBlockMessage());
        } else {
            // do nothing
        }
    }

    private void handleResponseAllBlock(WebSocket webSocket, List<Block> otherBlocks){
        Block otherLastBlock = otherBlocks.get(otherBlocks.size() - 1);
        Block localLastBlock = getLocalLastBlock();
        if (otherLastBlock.getIndex() > localLastBlock.getIndex()) {
            if (blockService.replaceBlocks(otherBlocks)) {
                broadcast(buildResponseAllBlockMessage());
            }
        } else if (otherLastBlock.getIndex() < localLastBlock.getIndex()){
            write(webSocket, buildResponseAllBlockMessage());
        } else {
            // do nothing
        }
    }

    private Block getLocalLastBlock(){
        return blockService.getLastBlock();
    }

    private List<Block> getLocalAllBlock(){
        return blockService.listBlocks();
    }

    private void write(WebSocket ws, String message) {
        ws.send(message);
    }
}
