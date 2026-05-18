package org.example.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.pojo.entity.Tag;
import org.example.server.service.TagService;
import org.example.server.mapper.TagMapper;
import org.springframework.stereotype.Service;

/**
* @author Zou
* @description 针对表【tag(标签)】的数据库操作Service实现
* @createDate 2026-05-17 15:54:03
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




