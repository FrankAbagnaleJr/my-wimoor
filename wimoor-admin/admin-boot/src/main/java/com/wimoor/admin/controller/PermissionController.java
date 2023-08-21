package com.wimoor.admin.controller;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wimoor.admin.pojo.entity.SysPermission;
import com.wimoor.admin.pojo.vo.PermissionVO;
import com.wimoor.admin.service.ISysPermissionService;
import com.wimoor.common.result.Result;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;

@Api(tags = "权限接口")
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final ISysPermissionService iSysPermissionService;

    @ApiOperation(value = "列表分页")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", defaultValue = "1", value = "页码", paramType = "query", dataType = "Integer"),
            @ApiImplicitParam(name = "limit", defaultValue = "10", value = "每页数量", paramType = "query", dataType = "Integer"),
            @ApiImplicitParam(name = "name", value = "权限名称", paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "menuId", value = "菜单ID", paramType = "query", dataType = "Long")
    })
    @GetMapping("/page")
    public Result<?> pageList( Integer page,Integer limit, String name,  Long menuId) {
    	IPage<PermissionVO> result = null;
    	try {
    		
    		 result = iSysPermissionService.list(new Page<>(page, limit),name,menuId);
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
        return Result.success(result.getRecords(), result.getTotal());
    }

    @ApiOperation(value = "权限列表")
    @ApiImplicitParam(name = "menuId", value = "菜单ID", paramType = "query", dataType = "Long")
    @GetMapping
    public Result<?> list(BigInteger menuId) {
        List<SysPermission> list = iSysPermissionService.list(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getMenuId, menuId));
        return Result.success(list);
    }

    @ApiOperation(value = "权限详情")
    @ApiImplicitParam(name = "id", value = "权限ID", required = true, paramType = "path", dataType = "Long")
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable BigInteger id) {
        SysPermission permission = iSysPermissionService.getById(id);
        return Result.success(permission);
    }

    @ApiOperation(value = "新增权限")
    @PostMapping
    public Result<?> add(@RequestBody SysPermission permission) {
        boolean result = iSysPermissionService.save(permission);
        if (result) {
            iSysPermissionService.refreshPermRolesRules();
        }
        return Result.judge(result);
    }

    @ApiOperation(value = "修改权限")
    @PutMapping(value = "/{id}")
    public Result<?> update(
            @PathVariable Long id,
            @RequestBody SysPermission permission) {
        boolean result = iSysPermissionService.updateById(permission);
        if (result) {
            iSysPermissionService.refreshPermRolesRules();
        }
        return Result.judge(result);
    }

    @ApiOperation(value = "删除权限")
    @ApiImplicitParam(name = "ids", value = "id集合", required = true, paramType = "query", dataType = "Long")
    @DeleteMapping("/{ids}")
    public Result<?> delete(@PathVariable String ids) {
        boolean status = iSysPermissionService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.judge(status);
    }
    

    @ApiOperation(value = "刷新权限")
    @GetMapping("refresh")
    public Result<?> refresh(BigInteger menuId) {
    	  iSysPermissionService.refreshPermRolesRules();
        return Result.success();
    }
    
 
}
