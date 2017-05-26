package tech.ganyaozi.dicegirl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.ByteString;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ganyaozi.dicegirl.client.DiceIMClient;
import tech.ganyaozi.dicegirl.proto.BaseMessage;
import tech.ganyaozi.dicegirl.utils.ClientReconnectTask;
import tech.ganyaozi.dicegirl.utils.ConsoleTool;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class LaunchClient {

    private static final Logger logger = LoggerFactory.getLogger(LaunchClient.class);

    private static final String SERVER_LIST_CONFIG_PATH = "server_list.json";
    private static final Gson gson = new Gson();
    private static ArrayList<InetSocketAddress> serverList = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException, IOException {
        initServerList();
        ArrayList<String> strs = new ArrayList<>();
        serverList.forEach(inetSocketAddress -> strs.add(inetSocketAddress.toString()));
        int index = ConsoleTool.showInConsole(strs);
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(new ClientReconnectTask(serverList.get(index)));
        DiceIMClient.startServer(serverList.get(index));

        while (true) {
            System.out.println("Please input you message >> ");
            String cmd = ConsoleTool.readLine();
            if (!StringUtils.equals(cmd, "q")) {
                if (DiceIMClient.channel == null || !DiceIMClient.channel.isActive()) {
                    logger.error("channel not ready");
                    continue;
                }
                DiceIMClient.channel.writeAndFlush(BaseMessage.baseMessage.newBuilder()
                        .setCmd(BaseMessage.Commands.IM_CREATE_ROOM_REQ)
                        .setAck(false)
                        .setDstID("-1")
                        .setSrcID(UUID.randomUUID().toString())
                        .setContent(ByteString.copyFrom(cmd.getBytes()))
                        .build());
            } else {
                DiceIMClient.shutdownGracefully();
                service.shutdown();
                break;
            }
        }
    }

    private static void initServerList() throws FileNotFoundException {
        List<ServerNode> list = gson.fromJson(new FileReader(SERVER_LIST_CONFIG_PATH),
                new TypeToken<List<ServerNode>>() {
                }.getType());
        serverList.addAll(list.stream().map(ServerNode::convert).collect(Collectors.toList()));
    }

    private class ServerNode {
        private String name;
        private String host;
        private int port;

        public ServerNode(String name, String host, int port) {
            this.name = name;
            this.host = host;
            this.port = port;
        }

        InetSocketAddress convert() {
            return new InetSocketAddress(host, port);
        }
    }

}
