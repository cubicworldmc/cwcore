package space.cubicworld.core.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CoreCommandAnnotation {

    /**
     * Name of the command
     *
     * @return Name
     */
    String name() default "";

    /**
     * Permission to enter this command.
     * Usually equals to the name of command because in the node command used as end parameter
     *
     * @return Permission
     */
    String permission() default "";

    /**
     * Should we give to all permission by default or not.
     * True if no, false if yes
     *
     * @return Admin
     */
    boolean admin() default false;

}
