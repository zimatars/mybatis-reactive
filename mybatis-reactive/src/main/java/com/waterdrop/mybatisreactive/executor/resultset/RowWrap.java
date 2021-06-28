package com.waterdrop.mybatisreactive.executor.resultset;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;

public class RowWrap {
    private Row row;
    private RowMetadata rowMetadata;

    public RowWrap() {
    }

    public RowWrap(Row row, RowMetadata rowMetadata) {
        this.row = row;
        this.rowMetadata = rowMetadata;
    }

    public Row getRow() {
        return row;
    }

    public void setRow(Row row) {
        this.row = row;
    }

    public RowMetadata getRowMetadata() {
        return rowMetadata;
    }

    public void setRowMetadata(RowMetadata rowMetadata) {
        this.rowMetadata = rowMetadata;
    }
}
