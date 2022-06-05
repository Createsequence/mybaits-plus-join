package top.xiajibagao.crane.core.handler;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import top.xiajibagao.crane.core.handler.interfaces.SourceOperateInterceptor;
import top.xiajibagao.crane.core.helper.ExpressionUtils;
import top.xiajibagao.crane.core.parser.interfaces.AssembleOperation;
import top.xiajibagao.crane.core.parser.interfaces.Operation;
import top.xiajibagao.crane.core.parser.interfaces.PropertyMapping;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 数据源表达式预处理拦截器
 *
 * @author huangchengxing
 * @date 2022/06/04 23:37
 */
public class ExpressionPreprocessingInterceptor implements SourceOperateInterceptor {

    private ContextFactory contextFactory;

    public ExpressionPreprocessingInterceptor(@Nonnull ContextFactory contextFactory) {
        Objects.requireNonNull(contextFactory);
        this.contextFactory = contextFactory;
    }

    public ExpressionPreprocessingInterceptor setContextFactory(@Nonnull ContextFactory contextFactory) {
        Objects.requireNonNull(contextFactory);
        this.contextFactory = contextFactory;
        return this;
    }

    @Override
    public boolean supportInterceptReadSource(@Nullable Object source, PropertyMapping property, Operation operation) {
        return false;
    }

    @Override
    public boolean supportInterceptSourceWrite(@Nullable Object sourceData, @Nullable Object target, PropertyMapping property, AssembleOperation operation) {
        return CharSequenceUtil.isNotBlank(property.getExp());
    }

    @Override
    public Object interceptSourceWrite(@Nullable Object sourceData, @Nullable Object target, PropertyMapping property, AssembleOperation operation) {
        SourceWriteOperationContext context = new SourceWriteOperationContext(sourceData, target, property, operation);
        EvaluationContext evaluationContext = contextFactory.get(context);
        return ExpressionUtils.execute(property.getExp(), evaluationContext, property.getExpType(), true);
    }

    /**
     * SpEL表达式上下文工厂
     *
     * @implNote 实现类应当继承{@link DefaultContextFactory}，或支持该默认工厂所支持的全部功能
     * @author huangchengxing 
     * @date 2022/6/4 23:43
     */
    @FunctionalInterface
    public interface ContextFactory {

        /**
         * 根据本次执行的待处理对象与数据源还有各项配置，生成带有所需方法及参数的上下文
         *
         * @param sourceWriteOperationContext 上下文
         * @return org.springframework.expression.spel.support.StandardEvaluationContext
         * @author huangchengxing
         * @date 2022/6/4 21:34
         */
        EvaluationContext get(SourceWriteOperationContext sourceWriteOperationContext);

    }

    @Getter
    @RequiredArgsConstructor
    public static class SourceWriteOperationContext {
        private final Object sourceData;
        private final Object target;
        private final PropertyMapping property;
        private final AssembleOperation operation;
    }

    /**
     * 默认的上下文工厂，将自动根据当初操作项上下文中注册以下五个变量:
     * <ul>
     *     <li>#source: 数据源对象；</li>
     *     <li>#target: 待处理对象；</li>
     *     <li>#key: key字段的值；</li>
     *     <li>#src: 指定的参数值；</li>
     *     <li>#ref: 指定的参数值；</li>
     * </ul>
     * 并注册{@link MapAccessor}以便根据“xx.xx”格式的表达式访问Map集合数据
     *
     * @author huangchengxing 
     * @date 2022/6/4 23:54
     */
    public static class DefaultContextFactory implements ContextFactory {

        @Override
        public StandardEvaluationContext get(SourceWriteOperationContext sourceWriteOperationContext) {
            Object sourceData = sourceWriteOperationContext.getSourceData();
            Object target = sourceWriteOperationContext.getTarget();
            PropertyMapping property = sourceWriteOperationContext.getProperty();
            AssembleOperation operation = sourceWriteOperationContext.getOperation();

            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("target", target);
            context.setVariable("source", sourceData);
            context.setVariable("key", operation.getAssembler().getKey(target, operation));
            context.setVariable("src", property.getSource());
            context.setVariable("ref", property.getReference());

            context.addPropertyAccessor(new MapAccessor());

            return context;
        }
    }
}