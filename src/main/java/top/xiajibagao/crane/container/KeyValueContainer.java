package top.xiajibagao.crane.container;

import org.springframework.util.CollectionUtils;
import top.xiajibagao.crane.helper.TableMap;
import top.xiajibagao.crane.parse.interfaces.AssembleOperation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 通过命名空间与键值获取唯一值的{@link Container}实现
 *
 * @author huangchengxing
 * @date 2022/03/02 13:19
 */
public class KeyValueContainer implements Container {

    /**
     * 数据缓存
     */
    public final TableMap<String, String, Object> cache = new TableMap<>();

    /**
     * 注册值
     *
     * @param namespace 命名空间
     * @param values 要添加缓存
     * @author huangchengxing
     * @date 2022/2/25 14:57
     */
    public void register(String namespace, Map<String, ?> values) {
        if (CollectionUtils.isEmpty(values)) {
            return;
        }
        values.forEach((k, v) -> cache.put(namespace, k, v));
    }

    /**
     * 获取值
     *
     * @param namespace 命名空间
     * @param key key
     * @return T
     * @author huangchengxing
     * @date 2022/2/25 15:01
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String namespace, String key) {
        Map<String, Object> container = cache.get(namespace);
        return Objects.isNull(container) ? null : (T) container.get(key);
    }

    @Override
    public void process(List<Object> targets, List<AssembleOperation> operations) {
        if (CollectionUtils.isEmpty(targets) || CollectionUtils.isEmpty(operations)) {
            return;
        }
        targets.forEach(t -> operations.forEach(o -> {
            Object key = o.getAssembler().getKey(t, o);
            if (Objects.isNull(key)) {
                return;
            }
            Object val = get(o.getNamespace(), key.toString());
            if (Objects.nonNull(val)) {
                o.getAssembler().execute(t, val, o);
            }
        }));
    }

}