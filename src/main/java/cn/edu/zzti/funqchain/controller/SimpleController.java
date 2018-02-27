package cn.edu.zzti.funqchain.controller;

import cn.edu.zzti.funqchain.model.Block;
import cn.edu.zzti.funqchain.service.BlockService;
import cn.edu.zzti.funqchain.service.SocketService;
import org.java_websocket.WebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class SimpleController {

    @Autowired
    private BlockService blockService;
    @Autowired
    private SocketService socketService;

    @RequestMapping("addPeer")
    public boolean addPeer(@RequestParam String peer) {
        return socketService.connect(peer);
    }

    @RequestMapping("peers")
    public List<String> peers() {
        List<String> peers = new ArrayList<>();
        for (WebSocket s : socketService.listSockets()) {
            peers.add(s.getRemoteSocketAddress().toString());
        }
        return peers;
    }

    @RequestMapping("addBlock")
    public Block addBlock(@RequestParam String data) {
        Block newBlock = blockService.createBlock(data);
        blockService.addBlock(newBlock);
        socketService.broadcast(socketService.buildResponseLastBlockMessage());
        return newBlock;
    }

    @RequestMapping("blocks")
    public List<Block> blocks() {
        return blockService.listBlocks();
    }
}
