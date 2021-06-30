/**
 * Copyright (c) 2011-2020, hubin (jobob@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.waterdrop.mybatisreactive.pageplugin;


import org.apache.ibatis.session.RowBounds;

import java.beans.Transient;
import java.io.Serializable;

/**
 * <p>
 * 简单分页模型
 * </p>
 * 用户可以通过继承 org.apache.ibatis.session.RowBounds实现自己的分页模型<br>
 * 注意：插件仅支持RowBounds及其子类作为分页参数
 *
 */
public class Pagination extends RowBounds implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总数
     */
    private long total;

    /**
     * 每页显示条数，默认 10
     */
    private int size = 10;

    /**
     * 当前页
     */
    private int current = 1;

    /**
     * 查询总记录数（默认 true）
     */
    private boolean searchCount = true;

    /**
     * 优化 Count Sql 设置 false 执行 select count(1) from (listSql)
     */
    private boolean optimizeCountSql = true;


    public Pagination() {
        super();
    }

    /**
     * <p>
     * 分页构造函数
     * </p>
     *
     * @param current 当前页
     * @param size    每页显示条数
     */
    public Pagination(int current, int size) {
        this(current, size, true);
    }

    public Pagination(int current, int size, boolean searchCount) {
        super(offsetCurrent(current, size), size);
        if (current > 1) {
            this.current = current;
        }
        this.size = size;
        this.searchCount = searchCount;
    }

    private static int offsetCurrent(int current, int size) {
        if (current > 0) {
            return (current - 1) * size;
        }
        return 0;
    }

    public boolean hasPrevious() {
        return this.current > 1;
    }

    public boolean hasNext() {
        return this.current < this.getPages();
    }

    public long getTotal() {
        return total;
    }

    public Pagination setTotal(long total) {
        this.total = total;
        return this;
    }

    public int getSize() {
        return size;
    }

    public Pagination setSize(int size) {
        this.size = size;
        return this;
    }

    /**
     * 总页数
     * fixed github /issues/309
     */
    public long getPages() {
        if (this.size == 0) {
            return 0L;
        }
        long pages = this.total / this.size;
        if (this.total % this.size != 0) {
            pages++;
        }
        return pages;
    }

    public int getCurrent() {
        return current;
    }

    public Pagination setCurrent(int current) {
        this.current = current;
        return this;
    }

    @Transient
    public boolean isSearchCount() {
        return searchCount;
    }

    public Pagination setSearchCount(boolean searchCount) {
        this.searchCount = searchCount;
        return this;
    }

    @Transient
    public boolean isOptimizeCountSql() {
        return optimizeCountSql;
    }

    public void setOptimizeCountSql(boolean optimizeCountSql) {
        this.optimizeCountSql = optimizeCountSql;
    }

    @Override
    @Transient
    public int getOffset() {
        return super.getOffset();
    }

    @Override
    @Transient
    public int getLimit() {
        return super.getLimit();
    }

    @Override
    public String toString() {
        return "Pagination { total=" + total + " ,size=" + size + " ,pages=" + this.getPages() + " ,current=" + current + " }";
    }

}
