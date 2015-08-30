package ru.pressindex.elasticsearch.top;

import org.elasticsearch.common.inject.AbstractModule;

public class TopModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TopService.class).asEagerSingleton();
    }
}
