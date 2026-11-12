package io.github.keel.iam.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * IAM 角色-资源关联持久化对象。
 */
@Getter
@Setter
@TableName("iam_role_resource")
public class IamRoleResourcePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;

    private Long resourceId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
}
