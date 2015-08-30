package ru.pressindex.elasticsearch.top;

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.rest.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class TopService extends AbstractComponent implements RestHandler {
    private ConcurrentHashMap<ShardId, ArrayList<Engine.Index>> indexQueues = new ConcurrentHashMap<>();

    @Inject
    public TopService(Settings Settings, RestController restController) {
        super(Settings);

        restController.registerHandler(RestRequest.Method.GET, "/_top", this);
    }

    public void registerIndexQueueList(ShardId shardId, ArrayList<Engine.Index> indexQueue){
        indexQueues.put(shardId, indexQueue);
    }

    public void unregisterIndexQueueList(ShardId shardId){
        indexQueues.remove(shardId);
    }

    @Override
    public void handleRequest(RestRequest request, RestChannel channel) throws Exception {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        HashMap<String,  HashMap<ShardId, ArrayList<Engine.Index>>> indexQueriesByIndex = new HashMap<>();

        for(Map.Entry<ShardId, ArrayList<Engine.Index>> queryEntry: indexQueues.entrySet()){
            ShardId shardId = queryEntry.getKey();

            for(Engine.Index entry : queryEntry.getValue()) {

                if (!indexQueriesByIndex.containsKey(shardId.getIndex())) {
                    indexQueriesByIndex.put(shardId.getIndex(), new HashMap<ShardId, ArrayList<Engine.Index>>());
                }

                if (!indexQueriesByIndex.get(shardId.getIndex()).containsKey(shardId)) {
                    indexQueriesByIndex.get(shardId.getIndex()).put(shardId, new ArrayList<Engine.Index>());
                }
                indexQueriesByIndex.get(shardId.getIndex()).get(shardId).add(entry);
            }
        }

        builder.startObject();
        builder.startObject("indices");

        for (Map.Entry<String, HashMap<ShardId, ArrayList<Engine.Index>>> entry : indexQueriesByIndex.entrySet()){
            builder.startArray(entry.getKey());

            for(Map.Entry<ShardId, ArrayList<Engine.Index>> shardEntry : entry.getValue().entrySet()){
                for(Engine.Index indexAction : shardEntry.getValue()){
                    if(indexAction != null){
                        builder.startObject();
                        builder.field("id", indexAction.id());
                        builder.field("shardId", shardEntry.getKey().toString());
                        builder.field("startTime", indexAction.startTime());
                        builder.field("startedAt", new Timestamp(indexAction.timestamp()).toLocalDateTime());
                        builder.field("runnedNano", System.nanoTime() - indexAction.startTime());
                        builder.endObject();
                    }
                }
            }
            builder.endArray();
        }
        builder.endObject();
        builder.endObject();
        channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder.string()));
    }
}