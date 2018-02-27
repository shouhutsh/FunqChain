package cn.edu.zzti.funqchain.service;

import cn.edu.zzti.funqchain.model.Block;
import cn.edu.zzti.funqchain.util.CryptoUtils;
import cn.edu.zzti.funqchain.util.StrUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class BlockService {

    private static List<Block> blockChain = new ArrayList<>();

    @PostConstruct
    public void init(){
        Block first = new Block();
        first.setIndex(1);
        first.setPreHash("");
        first.setTimestamp(0L);
        first.setData("First Block");
        first.setHash(calculateHash(first));
        blockChain.add(first);
    }

    public boolean addBlock(Block newBlock) {
        if (isEffectiveBlock(newBlock, getLastBlock())) {
            blockChain.add(newBlock);
            return true;
        }
        return false;
    }

    public Block getLastBlock(){
        return blockChain.get(blockChain.size() - 1);
    }

    public Block createBlock(String data) {
        Block lastBlock = getLastBlock();
        Block newBlock = new Block();
        newBlock.setIndex(lastBlock.getIndex() + 1);
        newBlock.setPreHash(lastBlock.getHash());
        newBlock.setTimestamp(System.currentTimeMillis());
        newBlock.setData(data);
        newBlock.setHash(calculateHash(newBlock));
        return newBlock;
    }

    public List<Block> listBlocks(){
        return blockChain;
    }

    public boolean replaceBlocks(List<Block> otherBlocks) {
        if (isEffectiveBlocks(otherBlocks)) {
            blockChain = new CopyOnWriteArrayList<>(otherBlocks);
            return true;
        }
        return false;
    }

    private boolean isEffectiveBlock(Block newBlock, Block preBlock) {
        if (newBlock.getIndex() != preBlock.getIndex() + 1) {
            return false;
        }
        if (!StrUtils.equals(newBlock.getPreHash(), preBlock.getHash())) {
            return false;
        }
        if (!StrUtils.equals(newBlock.getHash(), calculateHash(newBlock))) {
            return false;
        }
        return true;
    }

    private boolean isEffectiveBlocks(List<Block> otherBlocks) {
        if (otherBlocks.size() > blockChain.size()) {
            if (otherBlocks.get(0).equals(blockChain.get(0))) {
                for (int i = 1; i < otherBlocks.size(); ++i) {
                    if (!isEffectiveBlock(otherBlocks.get(i), otherBlocks.get(i - 1))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private String calculateHash(Block block) {
        StringBuilder str = new StringBuilder();
        str.append(block.getIndex()).append(block.getPreHash()).append(block.getTimestamp()).append(block.getData());
        return CryptoUtils.hash(str.toString());
    }
}
