package cabe11;

import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A custom ComboBox control that supports additional features like editing, adding, and removing items.
 *
 * @param <T> the type of the items contained in the ComboBox
 */
public class ComboBoxEx<T> extends CustomControl<HBox> implements InputControl<T> {
    public ComboBoxEx(@Nullable UnaryOperator<T> edit, @Nullable Supplier<T> add, @Nullable BiPredicate<ComboBoxEx<T>, T> remove, Function<T, String> format, Collection<T> items) {

        Callback<ListView<T>, ListCell<T>> cellFactory = new Callback<>() {

            @Override
            public ListCell<T> call(@Nullable ListView<T> lv) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(@Nullable T item, boolean empty) {
                        super.updateItem(item, empty);
                    }
                };
            }
        };
    }
}
