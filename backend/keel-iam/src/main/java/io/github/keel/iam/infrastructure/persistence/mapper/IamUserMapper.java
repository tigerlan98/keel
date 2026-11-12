package io.github.keel.iam.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamUserPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * IAM 用户 Mapper，仅提供 MyBatis-Plus 基础 CRUD。
 */
@Mapper
public interface IamUserMapper extends BaseMapper<IamUserPO> {

    @Select("""
            SELECT COUNT(*)
            FROM iam_user_role ur
            JOIN iam_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND r.code = 'SUPER_ADMIN'
              AND r.status = 'ENABLED'
            """)
    int countSuperAdminRole(@Param("userId") Long userId);
}
