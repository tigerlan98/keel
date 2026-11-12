package io.github.keel.iam.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamRoleResourcePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * IAM 角色-资源关联 Mapper。
 */
@Mapper
public interface IamRoleResourceMapper extends BaseMapper<IamRoleResourcePO> {
}
