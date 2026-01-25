import org.jspecify.annotations.NullMarked;
import cabe022.LogFactoryJcl;

@NullMarked
module cabe022app {
    exports cabe022.app;
    requires org.jspecify;
    requires cabe022lib;
}
