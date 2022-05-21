package top.xiajibagao.crane.core.annotation;

import org.springframework.core.annotation.AliasFor;
import top.xiajibagao.crane.core.operator.BeanReflexDisassembler;
import top.xiajibagao.crane.core.operator.interfaces.Disassembler;

import java.lang.annotation.*;

/**
 * 数据装卸注解
 * <p>注解在嵌套字段上，将会在处理数据时将注解字段递归并展开为复数需要进行装配操作的对象
 *
 * @author huangchengxing
 * @date 2022/02/28 18:00
 */
@MateAnnotation
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Disassemble {

    /**
     * 待处理的目标类型
     */
    @AliasFor("targetClass")
    Class<?> value() default Void.class;

    /**
     * 待处理的目标类型
     */
    @AliasFor("value")
    Class<?> targetClass() default Void.class;

    /**
     * 待处理的字段，当注解在类属性上时该属性将强制覆盖该值
     */
    String key() default "";

    /**
     * 字段别名。
     * 仅当无法根据注解字段名找到key字段时，才尝试通过别名找到至少一个存在的字段。
     */
    String[] aliases() default {};

    /**
     * 装卸器
     */
    Class<? extends Disassembler> disassembler() default BeanReflexDisassembler.class;

    /**
     * 装卸器bean名称
     */
    String disassemblerName() default "";

}
