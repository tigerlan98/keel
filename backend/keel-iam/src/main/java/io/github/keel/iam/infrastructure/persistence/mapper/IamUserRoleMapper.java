package io.github.keel.iam.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamUserRolePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * IAM 用户-角色关联 Mapper。
 */
@Mapper
public interface IamUserRoleMapper extends BaseMapper<IamUserRolePO> {
}
