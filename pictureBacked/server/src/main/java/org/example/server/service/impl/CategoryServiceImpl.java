package org.example.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.pojo.entity.Category;
import org.example.server.service.CategoryService;
import org.example.server.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

/**
* @author Zou
* @description 针对表【category(分类)】的数据库操作Service实现
* @createDate 2026-05-17 15:54:12
*/
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>
    implements CategoryService{

}




