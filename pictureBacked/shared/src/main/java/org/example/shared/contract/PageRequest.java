package org.example.shared.contract;

import lombok.Data;

@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段（仅允许字母、数字、下划线，防 SQL 注入）
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";

    /**
     * 校验 sortField 是否安全（仅允许列名字符集）
     */
    public void setSortField(String sortField) {
        if (sortField != null && !sortField.isBlank()
                && !sortField.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("非法的排序字段: " + sortField);
        }
        this.sortField = sortField;
    }
}
