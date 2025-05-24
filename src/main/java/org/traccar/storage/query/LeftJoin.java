package org.traccar.storage.query;

public class LeftJoin {

    private final Class<?> entity;
    private final Condition condition;

    public LeftJoin(Class<?> entity, Condition condition) {
        this.entity = entity;
        this.condition = condition;
    }

    public Class<?> getEntity() {
        return entity;
    }

    public Condition getCondition() {
        return condition;
    }

}
