package io.github.keel.iam.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import io.github.keel.common.persistence.entity.BasePO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("iam_department")
public class IamDepartmentPO extends BasePO {
    private Long parentId;
    private String code;
    private String name;
    private Integer sort;
    private String status;
    @Version private Integer version;
}
