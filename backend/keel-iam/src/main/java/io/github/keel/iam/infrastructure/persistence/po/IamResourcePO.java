package io.github.keel.iam.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.keel.common.persistence.entity.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * IAM 资源持久化对象。
 */
@Getter
@Setter
@TableName("iam_resource")
public class IamResourcePO extends BasePO {

    private Long parentId;

    private String type;

    private String code;

    private String name;

    private String path;

    private String component;

    private String icon;

    private Integer sort;

    private Boolean visible;

    private String status;

    @Version
    private Integer version;
}
