package ru.pressindex.elasticsearch.top;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.engine.Engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class TopStats implements ToXContent {
    private final ConcurrentHashMap<Engine.Index, HashMap<String, Object>> indexQueries;

    public TopStats(ConcurrentHashMap<Engine.Index, HashMap<String, Object>> indexQueries) {
        this.indexQueries = indexQueries;
    }

    @Override
    public String toString() {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().prettyPrint();
            builder.startObject();
            toXContent(builder, EMPTY_PARAMS);
            builder.endObject();
            return builder.string();
        } catch (IOException e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        ArrayList<HashMap<String, Object>> queries = new ArrayList<>();

        for( Map.Entry<Engine.Index, HashMap<String, Object>> entry : indexQueries.entrySet() ){
            HashMap<String, Object> query = entry.getValue();
            query.put("id", entry.getKey().id());
            query.put("runnedNano", System.nanoTime() - entry.getKey().startTime());
            queries.add(query);
        }

        builder.field("index_queries", queries);
        return builder;
    }
}
