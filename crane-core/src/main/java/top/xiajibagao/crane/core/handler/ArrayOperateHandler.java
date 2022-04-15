package top.xiajibagao.crane.core.handler;

import cn.hutool.core.util.ArrayUtil;
import top.xiajibagao.crane.core.handler.interfaces.OperateHandler;
import top.xiajibagao.crane.core.handler.interfaces.OperateHandlerChain;
import top.xiajibagao.crane.core.parser.interfaces.AssembleOperation;
import top.xiajibagao.crane.core.parser.interfaces.AssembleProperty;
import top.xiajibagao.crane.core.parser.interfaces.Operation;

import java.util.Arrays;
import java.util.Objects;

/**
 * 处理数组类型的数据源与待处理对象
 *
 * @author huangchengxing
 * @date 2022/04/08 16:39
 */
public class ArrayOperateHandler extends CollectionOperateHandler implements OperateHandler {

    public ArrayOperateHandler(OperateHandlerChain handlerChain) {
        super(handlerChain);
    }

    @Override
    public boolean sourceCanRead(Object source, AssembleProperty property, Operation operation) {
        return ArrayUtil.isArray(source);
    }

    @Override
    public boolean targetCanWrite(Object sourceData, Object target, AssembleProperty property, AssembleOperation operation) {
        return ArrayUtil.isArray(target);
    }

    @Override
    public Object readFromSource(Object source, AssembleProperty property, Operation operation) {
        if (Objects.isNull(source) || ArrayUtil.isEmpty(source)) {
            return null;
        }
        return super.readFromSource(Arrays.asList((Object[])source), property, operation);
    }

    @Override
    public void writeToTarget(Object sourceData, Object target, AssembleProperty property, AssembleOperation operation) {
        if (ArrayUtil.isNotEmpty(target)) {
            super.writeToTarget(sourceData, Arrays.asList((Object[])target), property, operation);
        }
    }

}