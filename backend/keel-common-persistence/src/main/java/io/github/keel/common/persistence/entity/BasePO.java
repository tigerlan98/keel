package io.github.keel.common.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 可变业务表的基础持久化对象，统一主键与审计字段。
 *
 * <p>追加型日志表不继承本类，只保留 {@code createdAt}。</p>
 */
@Getter
@Setter
public abstract class BasePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
