package top.xiajibagao.crane.parse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import top.xiajibagao.crane.annotation.Assemble;
import top.xiajibagao.crane.annotation.Disassemble;
import top.xiajibagao.crane.annotation.PropsTemplate;
import top.xiajibagao.crane.annotation.Sort;
import top.xiajibagao.crane.config.CraneGlobalConfiguration;
import top.xiajibagao.crane.container.Container;
import top.xiajibagao.crane.exception.CraneException;
import top.xiajibagao.crane.helper.CollUtils;
import top.xiajibagao.crane.helper.ObjectUtils;
import top.xiajibagao.crane.operator.interfaces.Disassembler;
import top.xiajibagao.crane.operator.interfaces.OperatorFactory;
import top.xiajibagao.crane.parse.interfaces.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 类型配置解析器
 *
 * @author huangchengxing
 * @date 2022/03/01 16:13
 */
@Slf4j
@RequiredArgsConstructor
public class BeanOperateConfigurationParser implements OperateConfigurationParser<BeanOperationConfiguration> {

    private final CraneGlobalConfiguration configuration;
    private final BeanFactory beanFactory;

    @Override
    public BeanOperationConfiguration parse(Class<?> targetClass, OperatorFactory operatorFactory) {
        return parse(targetClass, operatorFactory, new ParseContext());
    }

    private BeanOperationConfiguration parse(Class<?> targetClass, OperatorFactory operatorFactory, ParseContext parseContext) {
        BeanOperationConfiguration operationConfiguration = createConfiguration(targetClass, operatorFactory);
        List<AssembleOperation> sortedAssembleOperations = new ArrayList<>();
        List<DisassembleOperation> sortedDisassembleOperations = new ArrayList<>();
        // 解析属性注解获取操作配置
        for (Field field : targetClass.getDeclaredFields()) {
            sortedAssembleOperations.addAll(parseAssemblerAnnotation(field, operationConfiguration));
            sortedDisassembleOperations.addAll(parseDisassembleAnnotation(field, operationConfiguration, parseContext));
        }
        // 按sort排序
        Collections.sort(sortedAssembleOperations);
        operationConfiguration.getAssembleOperations().addAll(sortedAssembleOperations);
        Collections.sort(sortedDisassembleOperations);
        operationConfiguration.getDisassembleOperations().addAll(sortedDisassembleOperations);
        return operationConfiguration;
    }

    /**
     * 创建一个配置
     *
     * @param targetClass 目标类型
     * @param operatorFactory 操作者工厂
     * @return T
     * @author huangchengxing
     * @date 2022/3/1 16:24
     */
    protected BeanOperationConfiguration createConfiguration(Class<?> targetClass, OperatorFactory operatorFactory) {
        return new BeanOperationConfiguration(configuration, targetClass, operatorFactory, new ArrayList<>(), new ArrayList<>());
    }

    // =========================== 装配 ===========================

    /**
     * 解析{@link Assemble}注解
     *
     * @param property 属性
     * @param configuration 配置
     * @return java.util.List<top.xiajibagao.crane.parse.interfaces.AssembleOperation>
     * @author huangchengxing
     * @date 2022/3/1 16:55
     */
    protected List<AssembleOperation> parseAssemblerAnnotation(Field property, BeanOperationConfiguration configuration) {
        Assemble assemble = AnnotatedElementUtils.getMergedAnnotation(property, Assemble.class);
        List<AssembleOperation> operations = ObjectUtils.computeIfNotNull(
            AnnotatedElementUtils.getMergedAnnotation(property, Assemble.List.class),
            list -> CollUtils.toList(
                Arrays.asList(list.value()), a -> createAssembleOperation(property, a, configuration)
            ), new ArrayList<>()
        );
        if (Objects.nonNull(assemble)) {
            operations.add(createAssembleOperation(property, assemble, configuration));
        }

        CraneException.throwIfFalse(
            CollectionUtils.isEmpty(operations) || !AnnotatedElementUtils.hasAnnotation(property, Disassemble.class),
            "[%s]属性[$s]无法同时被%s和%s注解标记",
            configuration.getTargetClass(), property, Assemble.class, Disassemble.class
        );
        return operations;
    }
    
    /**
     * 根据{@link Assemble}注解创建{@link AssembleOperation}
     *
     * @param property 属性
     * @param annotation 注解
     * @param configuration 配置
     * @return top.xiajibagao.crane.parse.interfaces.AssembleOperation
     * @author huangchengxing
     * @date 2022/3/1 17:14
     */
    protected AssembleOperation createAssembleOperation(Field property, Assemble annotation, BeanOperationConfiguration configuration) {
        Set<String> aliases = CollUtils.toSet(Arrays.asList(annotation.aliases()));
        Container container = (Container)beanFactory.getBean(annotation.container());
        // 解析属性配置
        List<AssembleProperty> properties = new ArrayList<>(CollUtils.toList(
            Arrays.asList(annotation.props()), p -> new BeanAssembleProperty(p.value(), p.src())
        ));
        // 若存在属性模板，则解析模板，并将属性配置加入当前配置
        Stream.of(annotation.propTemplates())
            .map(this::parsePropsTemplateAnnotation)
            .filter(CollUtils::isNotEmpty)
            .flatMap(Collection::stream)
            .forEach(properties::add);

        return new BeanAssembleOperation(
            ObjectUtils.computeIfNotNull(
                AnnotatedElementUtils.getMergedAnnotation(property, Sort.class),
                Sort::value, Integer.MAX_VALUE
            ),
            configuration,
            property, aliases,
            annotation.namespace(), container,
            configuration.getOperatorFactory().getAssembler(), properties
        );
    }
    
    /**
     * 解析{@link PropsTemplate}注解
     *
     * @param targetClass 目标类型
     * @return java.util.List<top.xiajibagao.crane.parse.interfaces.AssembleProperty>
     * @author huangchengxing
     * @date 2022/3/3 15:05
     */
    protected List<AssembleProperty> parsePropsTemplateAnnotation(Class<?> targetClass) {
        PropsTemplate annotation = AnnotatedElementUtils.findMergedAnnotation(targetClass, PropsTemplate.class);
        if (Objects.isNull(annotation)) {
            return Collections.emptyList();
        }
        return Stream.of(annotation.value())
            .map(p -> new BeanAssembleProperty(p.value(), p.src()))
            .collect(Collectors.toList());
    }

    // =========================== 拆卸 ===========================
    
    /**
     * 解析{@link Disassemble}注解
     *
     * @param property 属性
     * @param configuration 当前正在构建的配置
     * @param parseContext 解析上下文，用于缓存配置处理循环依赖
     * @return java.util.List<top.xiajibagao.crane.parse.interfaces.DisassembleOperation>
     * @author huangchengxing
     * @date 2022/3/1 17:49
     */
    protected List<DisassembleOperation> parseDisassembleAnnotation(Field property, BeanOperationConfiguration configuration, ParseContext parseContext) {
        Disassemble disassemble = AnnotatedElementUtils.findMergedAnnotation(property, Disassemble.class);
        if (Objects.isNull(disassemble)) {
            return Collections.emptyList();
        }
        CraneException.throwIfTrue(
            AnnotatedElementUtils.hasAnnotation(property, Assemble.class),
            "[%s]属性[$s]无法同时被%s和%s注解标记", configuration.getTargetClass(), property, Assemble.class, Disassemble.class
        );

        // 递归解析拆卸字段类型
        parseContext.lookingFor(configuration);
        Class<?> operateClass = disassemble.value();
        OperationConfiguration operationConfiguration;
        if (parseContext.isInLooking(operateClass)) {
            // 存在循环依赖，则先通过缓存获取引用
            log.warn("类{}与嵌套的成员变量类型{}形成循环依赖...", configuration.getTargetClass(), operateClass);
            operationConfiguration = parseContext.getLookingForConfig(operateClass);
        } else {
            operationConfiguration = createConfiguration(operateClass, configuration.getOperatorFactory());
        }
        DisassembleOperation operation = createDisassembleOperation(property, disassemble, configuration, operationConfiguration, parseContext);
        parseContext.found(configuration);

        return Collections.singletonList(operation);
    }
    
    /**
     * 将{@link Disassemble}注解解析为{@link DisassembleOperation}
     *
     * @param property 属性
     * @param annotation {@link Disassemble}注解
     * @param configuration 当前正在构建的配置
     * @param operationConfiguration 待拆卸属性的类型配置
     * @param parseContext 解析上下文，用于缓存配置处理循环依赖
     * @return top.xiajibagao.crane.parse.interfaces.DisassembleOperation
     * @author huangchengxing
     * @date 2022/3/1 17:50
     */
    protected DisassembleOperation createDisassembleOperation(
        Field property, Disassemble annotation, BeanOperationConfiguration configuration, OperationConfiguration operationConfiguration, ParseContext parseContext) {
        Disassembler disassembler = operationConfiguration.getOperatorFactory().getDisassembler();
        Set<String> aliases = CollUtils.toSet(Arrays.asList(annotation.aliases()));
        return new BeanDisassembleOperation(
            ObjectUtils.computeIfNotNull(
                AnnotatedElementUtils.getMergedAnnotation(property, Sort.class),
                Sort::value, Integer.MAX_VALUE
            ),
            configuration,
            disassembler,
            operationConfiguration,
            property, aliases
        );
    }

    /**
     * 解析上下文，用于处理循环依赖问题
     *
     * @author huangchengxing
     * @date 2022/02/26 13:17
     */
    public static class ParseContext {

        private final Map<Class<?>, OperationConfiguration> inLookingOption = new HashMap<>();

        public boolean isInLooking(Class<?> target) {
            return inLookingOption.containsKey(target);
        }

        public void lookingFor(OperationConfiguration target) {
            inLookingOption.put(target.getTargetClass(), target);
        }

        public OperationConfiguration getLookingForConfig(Class<?> target) {
            OperationConfiguration config = inLookingOption.get(target);
            Assert.notNull(config, String.format("类[%s]不处于解析状态，或已经完成解析", target));
            return config;
        }

        public void found(OperationConfiguration config) {
            Class<?> foundClass = config.getTargetClass();
            Assert.notNull(config, String.format("类[%s]不处于解析状态，或已经完成解析", foundClass));
        }

    }

}