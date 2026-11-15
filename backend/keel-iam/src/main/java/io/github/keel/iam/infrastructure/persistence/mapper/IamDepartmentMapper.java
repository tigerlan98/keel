package io.github.keel.iam.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import io.github.keel.iam.infrastructure.persistence.po.IamDepartmentPO;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface IamDepartmentMapper extends BaseMapper<IamDepartmentPO> {
    @Select("SELECT * FROM iam_department ORDER BY sort, id LIMIT 1001")
    List<IamDepartmentPO> tree();

    @Select("SELECT * FROM iam_department ORDER BY id LIMIT 1001 FOR UPDATE")
    List<IamDepartmentPO> lockTree();

    @Select("SELECT * FROM iam_department ORDER BY id LIMIT 1001 FOR SHARE")
    List<IamDepartmentPO> shareTree();

    @Select("SELECT * FROM iam_department WHERE code = 'ROOT' FOR UPDATE")
    IamDepartmentPO lockRoot();

    @Select("SELECT * FROM iam_department WHERE code = 'ROOT' FOR SHARE")
    IamDepartmentPO shareRoot();

    @Select("SELECT * FROM iam_department WHERE id = #{id} FOR SHARE")
    IamDepartmentPO lockAssignment(@Param("id") long id);

    @Select("SELECT id FROM iam_user WHERE department_id = #{id} LIMIT 1 FOR UPDATE")
    Long lockFirstUser(@Param("id") long id);

    @Select(
            "SELECT department_id, COUNT(*) AS total FROM iam_user WHERE department_id IS NOT NULL"
                + " GROUP BY department_id LIMIT 1001")
    List<DepartmentUserCount> userCounts();
}
