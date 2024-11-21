package nc.gateway.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nc.gateway.filter.AntiSpamFilter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@Data
public class ZookeeperConfigItem {

    @Value("${zookeeper.config.host}")
    private String host;

    @Value("${zookeeper.config.port}")
    private String port;

    @PostConstruct
    private void init() throws Exception {
        initZookeeperServer();
    }

    private void initZookeeperServer() throws Exception {
        String ServerHost = host + ":" + port;
        //构建客户端实例
        CuratorFramework curatorFramework= CuratorFrameworkFactory.builder()
                .connectString(ServerHost)
                .retryPolicy(new ExponentialBackoffRetry(1000,3)) // 设置重试策略
                .build();
        //启动客户端
        curatorFramework.start();

        String path = "/gateway-config";
        Stat stat = curatorFramework.checkExists().forPath(path);
        if (stat != null) {
            // 删除节点
            curatorFramework.delete()
                    .deletingChildrenIfNeeded()  // 如果存在子节点，则删除所有子节点
                    .forPath(path);  // 删除指定节点
        }

        // 创建节点
        curatorFramework.create()
                .creatingParentsIfNeeded()  // 如果父节点不存在，则创建父节点
                .withMode(CreateMode.PERSISTENT)
                .forPath(path, "Init Data".getBytes());

        final NodeCache nodeCache = new NodeCache(curatorFramework, path);
        // 启动NodeCache并立即从服务端获取最新数据
        nodeCache.start(true);

        // 注册节点变化监听器
        nodeCache.getListenable().addListener(new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {
                byte[] newData = nodeCache.getCurrentData().getData();
                System.out.println("Refresh cache when node data changed: " + new String(newData));
                AntiSpamFilter.refreshCaches();
            }
        });
    }
}
