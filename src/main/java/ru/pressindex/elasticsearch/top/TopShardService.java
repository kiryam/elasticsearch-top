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
import java.util.ArrayList;

public class TopShardService extends AbstractIndexShardComponent implements Closeable {
    private final ArrayList<Engine.Index> indexQueue = new ArrayList<>();
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
            //HashMap<String, Object> indexAction = new HashMap<>();
            //indexAction.put("id", index.id());
            //indexAction.put("shardId", shardId);
            //indexAction.put("startedAt", LocalDateTime.now());
            //indexAction.put("startTime", index.startTime());
            //indexQueue.add(indexAction);
            indexQueue.add(index);

            return index;
        }

        @Override
        public void postIndex(Engine.Index index) {
            indexQueue.remove(index);
            super.postIndex(index);
        }
    }
}