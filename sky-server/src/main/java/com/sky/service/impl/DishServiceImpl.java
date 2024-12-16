package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Transactional
    @Override
    public void saveWithFlavor(DishDTO dto) {
        Dish dish=new Dish();
        BeanUtils.copyProperties(dto,dish);
        //向菜品表插入1条数据
        dishMapper.insert(dish);
        //获取insert语句获得的主键值
        Long id = dish.getId();

        //向口味表插入n条数据
        List<DishFlavor> flavors = dto.getFlavors();
        if(flavors!=null && flavors.size()>0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(id); // 外键关联
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page=dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            //判断菜品是否能够删除 起售中--不能删除
            Dish dish= dishMapper.getById(id);
            if(dish.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //被套餐关联 不能删除
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);

        if(setmealIds.size()>0 && setmealIds!=null){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //删除菜品以及关联的口味
        for(Long id:ids){
            dishMapper.deleteById(id);
            dishFlavorMapper.deleteByDishId(id);
        }
    }

    @Override
    public DishVO getByIdWithFlavor(Long id) {
        DishVO dishVO = new DishVO();
        Dish dish = dishMapper.getById(id);
        BeanUtils.copyProperties(dish,dishVO);

        List<DishFlavor> dishFlavors= dishFlavorMapper.getByDishId(id);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);

        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        //向口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors!=null && flavors.size()>0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId()); // 外键关联
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    @Override
    public List<Dish> list(Long categoryId) {
        Dish dish=Dish.builder().categoryId(categoryId).status(StatusConstant.ENABLE).build();
        return dishMapper.list(dish);
    }
}
