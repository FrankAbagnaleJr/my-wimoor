package com.wimoor.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.admin.pojo.entity.SysRolePermission;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigInteger;
import java.util.List;

@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {


    @Select({"<script>",
            " SELECT",
            " 	t1.permission_id ",
            " FROM",
            " 	t_sys_role_permission t1",
            " 	INNER JOIN t_sys_permission t2 ON t1.permission_id = t2.id ",
            " WHERE 1=1 ",
            " <if test='menuId !=null '>",
            "    AND t2.menu_id = #{menuId} ",
            " </if>",
            " <if test='roleId !=null '>",
            "   AND t1.role_id = #{roleId} ",
            " </if>",
            "</script>"})
    List<BigInteger> listPermissionId(BigInteger menuId, BigInteger roleId);

    @Select({"<script>",
        " SELECT",
        " 	t2.id ",
        " FROM",
        "   t_sys_permission t2",
        " WHERE 1=1 ",
        " <if test='menuId !=null '>",
        "    AND t2.menu_id = #{menuId} ",
        " </if>",
        "</script>"})
    List<BigInteger> listMenuPermissionId(BigInteger menuId);

}
