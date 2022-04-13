package top.xiajiabagao.crane.starter.extension;

import lombok.Data;
import lombok.experimental.Accessors;
import top.xiajibagao.annotation.Assemble;
import top.xiajibagao.annotation.Disassemble;
import top.xiajibagao.annotation.Prop;
import top.xiajibagao.crane.extension.container.MethodSourceContainer;

import java.util.List;

/**
 * @author huangchengxing
 * @date 2022/04/09 20:23
 */
@Accessors(chain = true)
@Data
public class Classroom {

    @Assemble(container = MethodSourceContainer.class, namespace = "student", props = @Prop(src = "name", ref = "studentNames"))
    @Assemble(container = MethodSourceContainer.class, namespace = "student", props = @Prop(ref = "students"))
    private Integer id;

    List<Member> students;
    List<String> studentNames;

    @Disassemble(Member.class)
    List<Member> teachers;

}