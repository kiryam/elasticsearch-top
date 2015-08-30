package ru.pressindex.elasticsearch.top;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.indexing.IndexingOperationListener;
import org.elasticsearch.index.indexing.ShardIndexingService;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.index.shard.AbstractIndexShardComponent;
import org.elasticsearch.index.shard.ShardId;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class TopShardService extends AbstractIndexShardComponent implements Closeable {
    private final ConcurrentHashMap<Engine.Index, HashMap<String, Object>> indexQueue = new ConcurrentHashMap<>();
    private final RealTimeIndexListener realTimePercolatorOperationListener = new RealTimeIndexListener();
    private final ShardIndexingService indexingService;
    private final TopService topService;

    @Inject
    public TopShardService(ShardId shardId, @IndexSettings Settings indexSettings, ShardIndexingService indexingService, TopService topService) {
        super(shardId, indexSettings);
        this.indexingService = indexingService;
        this.topService = topService;
        indexingService.addListener(realTimePercolatorOperationListener);
        topService.registerIndexQueueList(shardId, indexQueue);
    }

    @Override
    public void close() throws IOException {
        indexingService.removeListener(realTimePercolatorOperationListener);
        topService.unregisterIndexQueueList(shardId);
    }

    private class RealTimeIndexListener extends IndexingOperationListener {
        @Override
        public Engine.Index preIndex(Engine.Index index) {
            HashMap<String, Object> properties = new HashMap<>();
            properties.put("id", index.id());
            properties.put("shardId", shardId);
            properties.put("startedAt", LocalDateTime.now());
            properties.put("startTime", index.startTime());
            indexQueue.put(index, properties);

            return index;
        }

        @Override
        public void postIndex(Engine.Index index) {
            indexQueue.remove(index);
            super.postIndex(index);
        }
    }
}