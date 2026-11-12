package io.github.keel.iam.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.keel.common.persistence.entity.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * IAM 角色持久化对象。
 */
@Getter
@Setter
@TableName("iam_role")
public class IamRolePO extends BasePO {

    private String code;

    private String name;

    private String description;

    private String status;

    private Integer sort;

    @Version
    private Integer version;
}
