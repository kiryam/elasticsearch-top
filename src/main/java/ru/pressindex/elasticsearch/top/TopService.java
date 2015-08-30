package ru.pressindex.elasticsearch.top;

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.rest.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class TopService extends AbstractComponent implements RestHandler {
    private ConcurrentHashMap<ShardId, ConcurrentHashMap<Engine.Index, HashMap<String, Object>>> indexQueues = new ConcurrentHashMap<>();

    @Inject
    public TopService(Settings Settings, RestController restController) {
        super(Settings);

        restController.registerHandler(RestRequest.Method.GET, "/_top", this);
    }

    public void registerIndexQueueList(ShardId shardId, ConcurrentHashMap<Engine.Index, HashMap<String, Object>> indexQuery){
        indexQueues.put(shardId, indexQuery);
    }

    public void unregisterIndexQueueList(ShardId shardId){
        indexQueues.remove(shardId);
    }

    @Override
    public void handleRequest(RestRequest request, RestChannel channel) throws Exception {
        XContentBuilder builder = XContentFactory.jsonBuilder().prettyPrint();
        HashMap<String,  HashMap<ShardId, ArrayList<HashMap<String, Object>>>> indexQueriesByIndex = new HashMap<>();

        for(Map.Entry<ShardId, ConcurrentHashMap<Engine.Index, HashMap<String, Object>>> queryEntry: indexQueues.entrySet()){
            ShardId shardId = queryEntry.getKey();

            for(Map.Entry<Engine.Index, HashMap<String, Object>> entry : queryEntry.getValue().entrySet()) {

                if (!indexQueriesByIndex.containsKey(shardId.getIndex())) {
                    indexQueriesByIndex.put(shardId.getIndex(), new HashMap<ShardId, ArrayList<HashMap<String, Object>>>());
                }

                if (!indexQueriesByIndex.get(shardId.getIndex()).containsKey(entry.getKey())) {
                    indexQueriesByIndex.get(shardId.getIndex()).put(shardId, new ArrayList<HashMap<String, Object>>());
                }
                indexQueriesByIndex.get(shardId.getIndex()).get(shardId).add(entry.getValue());
            }
        }

        builder.startObject("indices");

        for (Map.Entry<String, HashMap<ShardId, ArrayList<HashMap<String, Object>>>> entry : indexQueriesByIndex.entrySet()){
            builder.startArray(entry.getKey());

            for(Map.Entry<ShardId, ArrayList<HashMap<String, Object>>> shardEntry : entry.getValue().entrySet()){

                for(HashMap<String, Object> query : shardEntry.getValue()){
                    builder.startObject();
                    for( Map.Entry<String, Object> en : query.entrySet() ){
                        builder.field(en.getKey(), en.getValue());
                    }

                    builder.endObject();

                }

            }

            builder.endArray();
        }
        builder.endObject();
        channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder.string()));
    }
}
