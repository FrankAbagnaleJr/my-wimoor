package com.wimoor.admin.service.impl;

 
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wimoor.admin.common.constants.GlobalConstants;
import com.wimoor.admin.common.constants.SystemConstants;
import com.wimoor.admin.mapper.SysDeptMapper;
import com.wimoor.admin.pojo.entity.SysDept;
import com.wimoor.admin.pojo.vo.DeptVO;
import com.wimoor.admin.service.ISysDeptService;
import com.wimoor.common.SelectVO;
import com.wimoor.common.TreeSelectVO;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 部门业务类
 *
 * @author <a href="mailto:xianrui0365@163.com">xianrui</a>
 * @date 2021-08-22
 */
@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService {


    /**
     * 部门表格（Table）层级列表
     *
     * @param name 部门名称
     * @return
     */
    @Override
    public List<DeptVO> listTable(Integer status, String name) {
        List<SysDept> deptList = this.list(
                new LambdaQueryWrapper<SysDept>()
                        .like(StrUtil.isNotBlank(name), SysDept::getName, name)
                        .eq(SysDept::getStatus, status)
                        .orderByAsc(SysDept::getSort)
        );
        return recursion(deptList);
    }

    /**
     * 递归生成部门表格层级列表
     *
     * @param deptList 部门列表
     * @return 部门列表
     */
    private static List<DeptVO> recursion(List<SysDept> deptList) {
        List<DeptVO> deptTableList = new ArrayList<>();
        // 保存所有节点的 id
        Set<String> nodeIdSet = deptList.stream()
                .map(SysDept::getId)
                .collect(Collectors.toSet());
        for (SysDept sysDept : deptList) {
            // 不在节点 id 集合中存在的 id 即为顶级节点 id, 递归生成列表
            String parentId = sysDept.getParentId();
            if (!nodeIdSet.contains(parentId)) {
                deptTableList.addAll(recursionTableList(parentId, deptList));
                nodeIdSet.add(parentId);
            }
        }
        // 如果结果列表为空说明所有的节点都是独立分散的, 直接转换后返回
        if (deptTableList.isEmpty()) {
            return deptList.stream()
                    .map(item -> {
                        DeptVO deptVO = new DeptVO();
                        BeanUtil.copyProperties(item, deptVO);
                        return deptVO;
                    })
                    .collect(Collectors.toList());
        }
        return deptTableList;
    }

    /**
     * 递归生成部门表格层级列表
     *
     * @param parentId
     * @param deptList
     * @return
     */
    public static List<DeptVO> recursionTableList(String parentId, List<SysDept> deptList) {
        List<DeptVO> deptTableList = new ArrayList<>();
        Optional.ofNullable(deptList).orElse(new ArrayList<>())
                .stream()
                .filter(dept -> dept.getParentId().equals(parentId))
                .forEach(dept -> {
                    DeptVO deptVO = new DeptVO();
                    BeanUtil.copyProperties(dept, deptVO);
                    List<DeptVO> children = recursionTableList(dept.getId(), deptList);
                    deptVO.setChildren(children);
                    deptTableList.add(deptVO);
                });
        return deptTableList;
    }


    /**
     * 部门下拉（Select）层级列表
     *
     * @return
     */
    @Override
    public List<TreeSelectVO> listTreeSelect() {
        List<SysDept> deptList = this.list(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getStatus, GlobalConstants.STATUS_YES)
                .orderByAsc(SysDept::getSort)
        );
        List<TreeSelectVO> deptSelectList = recursionTreeSelectList(SystemConstants.ROOT_DEPT_ID, deptList);
        return deptSelectList;
    }


    /**
     * 递归生成部门表格层级列表
     *
     * @param parentId
     * @param deptList
     * @return
     */
    public static List<TreeSelectVO> recursionTreeSelectList(String parentId, List<SysDept> deptList) {
        List<TreeSelectVO> deptTreeSelectList = new ArrayList<>();
        Optional.ofNullable(deptList).orElse(new ArrayList<>())
                .stream()
                .filter(dept -> dept.getParentId().equals(parentId))
                .forEach(dept -> {
                    TreeSelectVO treeSelectVO = new TreeSelectVO(dept.getId(), dept.getName());
                    List<TreeSelectVO> children = recursionTreeSelectList(dept.getId(), deptList);
                    if (CollectionUtil.isNotEmpty(children)) {
                        treeSelectVO.setChildren(children);
                    }
                    deptTreeSelectList.add(treeSelectVO);
                });
        return deptTreeSelectList;
    }


    /**
     * 保存（新增/修改）部门
     *
     * @param dept
     * @return
     */
    @Override
    public String saveDept(SysDept dept) {
        String treePath = getDeptTreePath(dept);
        dept.setTreePath(treePath);
        this.saveOrUpdate(dept);
        return dept.getId();
    }

    /**
     * 删除部门
     *
     * @param ids 部门ID，多个以英文逗号,拼接字符串
     * @return
     */
    @Override
    public boolean deleteByIds(String ids) {
        AtomicBoolean result = new AtomicBoolean(true);
        List<String> idList = Arrays.asList(ids.split(","));
        // 删除部门及子部门
        Optional.ofNullable(idList).orElse(new ArrayList<>()).forEach(id ->
                result.set(this.remove(new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getId, id)
                        .or()
                        .apply("concat (',',tree_path,',') like concat('%,',{0},',%')", id)))
        );
        return result.get();
    }


    /**
     * 获取部门级联路径
     *
     * @param dept
     * @return
     */
    private String getDeptTreePath(SysDept dept) {
        String parentId = dept.getParentId();
        String treePath;
        if (parentId.equals(SystemConstants.ROOT_DEPT_ID)) {
            treePath = String.valueOf(SystemConstants.ROOT_DEPT_ID);
        } else {
            SysDept parentDept = this.getById(parentId);
            treePath = Optional.ofNullable(parentDept).map(item -> item.getTreePath() + "," + item.getId()).orElse(Strings.EMPTY);
        }
        return treePath;
    }

	@Override
	public List<SelectVO> listSelect() {
		// TODO Auto-generated method stub
		  List<SysDept> deptList = this.list(new LambdaQueryWrapper<SysDept>()
	                .eq(SysDept::getStatus, GlobalConstants.STATUS_YES)
	                .orderByAsc(SysDept::getSort)
	        );
	        List<SelectVO> deptSelectList = recursionSelectList(SystemConstants.ROOT_DEPT_ID, deptList);
	        return deptSelectList;
	}
 

	    /**
	     * 递归生成部门表格层级列表
	     *
	     * @param parentId
	     * @param deptList
	     * @return
	     */
	    public static List<SelectVO> recursionSelectList(String parentId, List<SysDept> deptList) {
	        List<SelectVO> deptTreeSelectList = new ArrayList<>();
	        Optional.ofNullable(deptList).orElse(new ArrayList<>())
	                .stream()
	                .filter(dept -> dept.getParentId().equals(parentId))
	                .forEach(dept -> {
	                	SelectVO treeSelectVO = new SelectVO(dept.getId(), dept.getName());
	                    List<SelectVO> children = recursionSelectList(dept.getId(), deptList);
	                    if (CollectionUtil.isNotEmpty(children)) {
	                        treeSelectVO.setChildren(children);
	                    }
	                    deptTreeSelectList.add(treeSelectVO);
	                });
	        return deptTreeSelectList;
	    }

}
