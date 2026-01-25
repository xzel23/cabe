import org.jspecify.annotations.NullMarked;
import cabe022.LogFactoryJcl;

@NullMarked
module cabe022lib {
    exports cabe022;
    requires org.jspecify;

    requires static org.apache.commons.logging;

    provides org.apache.commons.logging.LogFactory with LogFactoryJcl;
}
