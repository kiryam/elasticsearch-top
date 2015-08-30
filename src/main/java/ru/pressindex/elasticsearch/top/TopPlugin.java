package ru.pressindex.elasticsearch.top;


import com.google.common.collect.Lists;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;

public class TopPlugin extends AbstractPlugin {
    @Override public String name() {
        return "top-plugin";
    }

    @Override public String description() {
        return "Displays top index queries";
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = Lists.newArrayList();
        modules.add(TopModule.class);
        return modules;
    }

    @Override
    public Collection<Class<? extends Module>> shardModules() {
        Collection<Class<? extends Module>> modules = Lists.newArrayList();
        modules.add(TopShardModule.class);
        return modules;
    }
}