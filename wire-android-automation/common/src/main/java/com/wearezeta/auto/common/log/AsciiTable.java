package com.wearezeta.auto.common.log;

import java.util.ArrayList;
import java.util.List;

public class AsciiTable {

    private final List<Column> columns = new ArrayList<>();
    private final List<Row> data = new ArrayList<>();
    private int maxColumnWidth = Integer.MAX_VALUE;

    public void calculateColumnWidth() {

        for (Column column : columns) {
            column.width = column.name.length() + 1;
        }

        for (Row row : data) {
            int colIdx = 0;
            for (String value : row.values) {
                Column column = columns.get(colIdx);
                if (value == null) continue;

                column.width = Math.max(column.width, value.length() + 1);
                colIdx++;
            }
        }

        for (Column column : columns) {
            column.width = Math.min(column.width, maxColumnWidth);
        }
    }

    public String render() {
        StringBuilder sb = new StringBuilder();

        writeSeparator(columns, sb);
        writeColumnNames(columns, sb);
        writeSeparator(columns, sb);

        // values
        writeValues(columns, data, sb);

        writeSeparator(columns, sb);

        return sb.toString();
    }

    private void writeColumnNames(final List<Column> columns, final StringBuilder sb) {
        sb.append("|");
        for (Column column : columns) {
            sb.append(String.format(" %-" + (column.width) + "s", column.name));
            sb.append("|");
        }
        sb.append("\n");
    }

    private void writeSeparator(final List<Column> columns, final StringBuilder sb) {
        sb.append("+");
        for (Column column : columns) {
            sb.append(String.format("%-" + (column.width + 1) + "s", "").replace(' ', '-'));
            sb.append("+");
        }
        sb.append("\n");
    }

    private void writeValues(final List<Column> columns, final List<Row> rows, final StringBuilder sb) {
        for (Row row : rows) {
            int columnIdx = 0;
            sb.append("|");
            for (String value : row.values) {

                if (value != null && value.length() > maxColumnWidth)
                    value = value.substring(0, maxColumnWidth - 1);

                sb.append(String.format(" %-" + columns.get(columnIdx).width + "s", value));
                sb.append("|");

                columnIdx++;
            }
            sb.append("\n");
        }
    }

    public List<Column> getColumns() {
        return columns;
    }

    public List<Row> getData() {
        return data;
    }

    public int getMaxColumnWidth() {
        return maxColumnWidth;
    }

    public void setMaxColumnWidth(final int maxColumnWidth) {
        this.maxColumnWidth = maxColumnWidth;
    }

    public static class Column {

        private String name;
        private int width;

        public Column(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Column{" +
                    "name='" + name + '\'' +
                    ", width=" + width +
                    '}';
        }
    }

    public static class Row {

        private final List<String> values = new ArrayList<>();

        public List<String> getValues() {
            return values;
        }

        @Override
        public String toString() {
            return "Row{" +
                    "values=" + values +
                    '}';
        }
    }
}