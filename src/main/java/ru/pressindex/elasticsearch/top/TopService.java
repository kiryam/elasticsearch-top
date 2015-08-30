package ru.pressindex.elasticsearch.top;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.indexing.IndexingOperationListener;
import org.elasticsearch.index.indexing.ShardIndexingService;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.index.shard.AbstractIndexShardComponent;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.rest.*;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class TopService extends AbstractIndexShardComponent implements Closeable, RestHandler {
    private final ConcurrentHashMap<Engine.Index, HashMap<String, Object>> indexQueries = new ConcurrentHashMap<>();
    private final RealTimePercolatorOperationListener realTimePercolatorOperationListener = new RealTimePercolatorOperationListener();
    private final ShardIndexingService indexingService;

    @Inject
    public TopService(ShardId shardId, @IndexSettings Settings indexSettings, ShardIndexingService indexingService, RestController restController) {
        super(shardId, indexSettings);
        this.indexingService = indexingService;
        indexingService.addListener(realTimePercolatorOperationListener);

        restController.registerHandler(RestRequest.Method.GET, "/_top", this);
    }

    public synchronized TopStats stats() {
        return new TopStats(indexQueries);
    }

    @Override
    public void close() throws IOException {
        indexingService.removeListener(realTimePercolatorOperationListener);
    }

    @Override
    public void handleRequest(RestRequest request, RestChannel channel) throws Exception {
        channel.sendResponse(new BytesRestResponse(RestStatus.OK, stats().toString()));
    }

    private class RealTimePercolatorOperationListener extends IndexingOperationListener {
        @Override
        public Engine.Index preIndex(Engine.Index index) {
            HashMap<String, Object> properties = new HashMap<>();
            properties.put("startedAt", LocalDateTime.now());
            indexQueries.put(index, properties);

            return index;
        }

        @Override
        public void postIndex(Engine.Index index) {
            indexQueries.remove(index);
            super.postIndex(index);
        }
    }
}