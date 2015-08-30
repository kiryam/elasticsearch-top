package ru.pressindex.elasticsearch.top;

import org.elasticsearch.common.inject.AbstractModule;

public class TopShardModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TopShardService.class).asEagerSingleton();
    }
}
