package com.wimoor.admin.controller;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wimoor.admin.pojo.entity.SysDict;
import com.wimoor.admin.pojo.entity.SysDictItem;
import com.wimoor.admin.service.ISysDictItemService;
import com.wimoor.admin.service.ISysDictService;
import com.wimoor.common.result.Result;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;

@Api(tags = "字典接口")
@RestController
@RequestMapping("/api/v1/dicts")
@RequiredArgsConstructor
public class DictController {

    private final ISysDictService iSysDictService;
    private final ISysDictItemService iSysDictItemService;

    @ApiOperation(value = "列表分页")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "页码", paramType = "query", dataType = "Integer"),
            @ApiImplicitParam(name = "limit", value = "每页数量", paramType = "query", dataType = "Integer"),
            @ApiImplicitParam(name = "name", value = "字典名称", paramType = "query", dataType = "String"),
    })
    @GetMapping("/page")
    public Result<List<SysDict>> list( Integer page,Integer limit, String name) {
        Page<SysDict> result = iSysDictService.page(new Page<>(page, limit), new LambdaQueryWrapper<SysDict>()
                .like(StrUtil.isNotBlank(name), SysDict::getName, StrUtil.trimToNull(name))
                .orderByDesc(SysDict::getGmtModified)
                .orderByDesc(SysDict::getGmtCreate));
        return Result.success(result.getRecords(), result.getTotal());
    }

    
    @ApiOperation(value = "字典列表")
    @GetMapping
    public Result<List<SysDict>> list() {
        List<SysDict> list = iSysDictService.list( new LambdaQueryWrapper<SysDict>()
                .orderByDesc(SysDict::getGmtModified)
                .orderByDesc(SysDict::getGmtCreate));
        return Result.success(list);
    }


    @ApiOperation(value = "字典详情")
    @ApiImplicitParam(name = "id", value = "字典id", required = true, paramType = "path", dataType = "Long")
    @GetMapping("/{id}")
    public Result<SysDict> detail(@PathVariable BigInteger id) {
        SysDict dict = iSysDictService.getById(id);
        return Result.success(dict);
    }

    @ApiOperation(value = "新增字典")
    @ApiImplicitParam(name = "dictItem", value = "实体JSON对象", required = true, paramType = "body", dataType = "SysDictItem")
    @PostMapping
    public Result<Boolean> add(@RequestBody SysDict dict) {
        boolean status = iSysDictService.save(dict);
        return Result.judge(status);
    }

    @ApiOperation(value = "修改字典")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "字典id", required = true, paramType = "path", dataType = "Long"),
            @ApiImplicitParam(name = "dictItem", value = "实体JSON对象", required = true, paramType = "body", dataType = "SysDictItem")
    })
    @PutMapping(value = "/{id}")
    public Result<Boolean> update(
            @PathVariable BigInteger id,
            @RequestBody SysDict dict) {

        boolean status = iSysDictService.updateById(dict);
        if (status) {
            SysDict dbDict = iSysDictService.getById(id);
            // 字典code更新，同步更新字典项code
            if (!StrUtil.equals(dbDict.getCode(), dict.getCode())) {
                iSysDictItemService.update(new LambdaUpdateWrapper<SysDictItem>().eq(SysDictItem::getDictCode, dbDict.getCode())
                        .set(SysDictItem::getDictCode, dict.getCode()));
            }
        }
        return Result.judge(status);
    }

    @ApiOperation(value = "删除字典")
    @ApiImplicitParam(name = "ids", value = "以,分割拼接字符串", required = true, paramType = "query", dataType = "String")
    @DeleteMapping("/{ids}")
    public Result<Boolean> delete(@PathVariable String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        List<String> codeList = iSysDictService.listByIds(idList).stream().map(item -> item.getCode()).collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(codeList)) {
            long count = iSysDictItemService.count(new LambdaQueryWrapper<SysDictItem>().in(SysDictItem::getDictCode, codeList));
            Assert.isTrue(count == 0, "删除字典失败，请先删除关联字典数据");
        }
        boolean status = iSysDictService.removeByIds(idList);
        return Result.judge(status);
    }

    @ApiOperation(value = "选择性更新字典")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "用户ID", required = true, paramType = "path", dataType = "Long"),
            @ApiImplicitParam(name = "dictItem", value = "实体JSON对象", required = true, paramType = "body", dataType = "SysDictItem")
    })
    @PatchMapping(value = "/{id}")
    public Result<Boolean> patch(@PathVariable BigInteger id, @RequestBody SysDict dict) {
        LambdaUpdateWrapper<SysDict> updateWrapper = new LambdaUpdateWrapper<SysDict>().eq(SysDict::getId, id);
        updateWrapper.set(dict.getStatus() != null, SysDict::getStatus, dict.getStatus());
        boolean update = iSysDictService.update(updateWrapper);
        return Result.success(update);
    }
}
