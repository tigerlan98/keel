package io.github.keel.iam.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamRolePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * IAM 角色 Mapper。
 */
@Mapper
public interface IamRoleMapper extends BaseMapper<IamRolePO> {
}
