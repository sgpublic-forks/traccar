/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.storage.query;

public class Request {

    private final Columns columns;
    private final LeftJoin leftJoin;
    private final Condition condition;
    private final Order order;
    private final Limit limit;

    public Request(Columns columns) {
        this(columns, null, null);
    }

    public Request(Condition condition) {
        this(null, condition, null);
    }

    public Request(Columns columns, Condition condition) {
        this(columns, condition, null);
    }

    public Request(Columns columns, Order order) {
        this(columns, null, order);
    }

    public Request(Columns columns, Condition condition, Order order) {
        this(columns, null, condition, order);
    }

    public Request(Columns columns, Condition condition, Order order, Limit limit) {
        this(columns, null, condition, order, limit);
    }

    public Request(Columns columns, LeftJoin leftJoin, Condition condition, Order order) {
        this(columns, leftJoin, condition, order, Limit.ALL);
    }

    public Request(Columns columns, LeftJoin leftJoin, Condition condition, Order order, Limit limit) {
        this.columns = columns;
        this.leftJoin = leftJoin;
        this.condition = condition;
        this.order = order;
        this.limit = limit;
    }

    public Columns getColumns() {
        return columns;
    }

    public LeftJoin getLeftJoin() {
        return leftJoin;
    }

    public Condition getCondition() {
        return condition;
    }

    public Order getOrder() {
        return order;
    }

    public Limit getLimit() {
        return limit;
    }
}
