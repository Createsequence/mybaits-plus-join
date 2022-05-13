package top.xiajibagao.crane.core.annotation;

import org.springframework.core.annotation.AliasFor;
import top.xiajibagao.crane.core.container.KeyValueContainer;

import java.lang.annotation.*;

/**
 * 字段使用{@link KeyValueContainer}容器装配
 *
 * @see Assemble
 * @author huangchengxing
 * @date 2022/05/13 17:44
 */
@Assemble
@MateAnnotation
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AssembleKV {

    /**
     * 装配容器实例在spring中的名称
     */
    @AliasFor(annotation = Assemble.class, attribute = "containerName")
    String containerName() default "";

    /**
     * 指定容器中数据源对应的命名空间
     */
    @AliasFor(annotation = Assemble.class, attribute = "namespace")
    String namespace() default "";

    /**
     * 指定注解的key字段别名。<br />
     * 仅当无法根据注解字段名找到key字段时，才尝试通过别名找到至少一个存在的字段。
     */
    @AliasFor(annotation = Assemble.class, attribute = "aliases")
    String[] aliases() default {};

    /**
     * 字段映射配置
     */
    @AliasFor(annotation = Assemble.class, attribute = "props")
    Prop[] props() default {};

    /**
     * 字段映射配置模板 <br />
     * 指定类型，将解析类上的{@link PropsTemplate}注解，并将其{@link PropsTemplate#value()}加入
     * 当前配置中，效果等同于直接在当前{@link #props()}配置中追加配置。
     */
    @AliasFor(annotation = Assemble.class, attribute = "propTemplates")
    Class[] propTemplates() default {};

}