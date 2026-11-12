package io.github.keel.common.core.result;

import java.util.List;

/**
 * 统一分页响应。
 *
 * @param items 当前页数据
 * @param total 总记录数
 * @param page  当前页码，从 1 开始
 * @param size  每页条数
 */
public record PageResult<T>(List<T> items, long total, long page, long size) {
}
