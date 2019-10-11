package net.querz.mcaselector.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.converter.ByteStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.LongStringConverter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class EditArrayDialog<T> extends Dialog<EditArrayDialog.Result> {

	private TableView<Row> table = new TableView<>();

	private T array;

	@SuppressWarnings("unchecked")
	public EditArrayDialog(T array, Stage primaryStage) {
		// we only want to work on a copy of the array
		this.array = (T) Array.newInstance(array.getClass().getComponentType(), Array.getLength(array));
		// noinspection SuspiciousSystemArraycopy
		System.arraycopy(array, 0, this.array, 0, Array.getLength(array));

		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("array-editor-dialog-pane");
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

		setResultConverter(b -> {
			if (b == ButtonType.APPLY) {
				return new Result(this.array);
			}
			return null;
		});

		table.setPlaceholder(new Label());
		table.getStyleClass().add("array-editor-table-view");
		table.setEditable(true);

		TableColumn<Row, Integer> indexColumn = new TableColumn<>("Index");
		TableColumn<Row, ? extends Number> valueColumn = createValueColumn();
		if (valueColumn == null) {
			throw new IllegalArgumentException("invalid array type, could not create column");
		}
		indexColumn.setSortable(false);
		valueColumn.setSortable(false);
		indexColumn.setResizable(false);
		valueColumn.setResizable(false);
		valueColumn.setEditable(true);
		indexColumn.setPrefWidth(50);
		valueColumn.setPrefWidth(200);

		// noinspection unchecked
		table.getColumns().addAll(indexColumn, valueColumn);
		indexColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().index));
		applyArray();

		// TODO: create icons
		Label addBefore = new Label("^+");
		Label addAfter = new Label("v+");
		Label delete = new Label("del");

		getDialogPane().setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.SHIFT) {
				addBefore.setText("^+16");
				addAfter.setText("v+16");
			}
		});

		getDialogPane().setOnKeyReleased(e -> {
			if (e.getCode() == KeyCode.SHIFT) {
				addBefore.setText("^+");
				addAfter.setText("v+");
			}
		});

		addAfter.setOnMouseClicked(e -> add(addAfter.getText().equals("v+") ? 1 : 16, true));
		addBefore.setOnMouseClicked(e -> add(addBefore.getText().equals("^+") ? 1 : 16, false));
		delete.setOnMouseClicked(e -> delete());

		HBox options = new HBox();
		options.getChildren().addAll(addBefore, addAfter, delete);

		VBox content = new VBox();
		content.getChildren().addAll(options, table);

		getDialogPane().setContent(content);
	}

	@SuppressWarnings("unchecked")
	private TableColumn<Row, ? extends Number> createValueColumn() {
		if (array instanceof byte[]) {
			ByteStringConverter converter = new ByteStringConverter() {
				@Override
				public Byte fromString(String s) {
					try {
						return super.fromString(s);
					} catch (NumberFormatException ex) {
						return null;
					}
				}
			};

			TableColumn<Row, Byte> column = new TableColumn<>("Value");
			column.setCellFactory(t -> {
				TextFieldTableCell<Row, Byte> cell = new TextFieldTableCell<Row, Byte>() {
					@Override
					public void commitEdit(Byte b) {
						if (b == null) {
							return;
						}
						super.commitEdit(b);
						table.getItems().get(getIndex()).setValue(b);
					}
				};
				cell.setConverter(converter);
				return cell;
			});
			column.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>((Byte) c.getValue().value));
			return column;

		} else if (array instanceof int[]) {
			IntegerStringConverter converter = new IntegerStringConverter() {
				@Override
				public Integer fromString(String s) {
					try {
						return super.fromString(s);
					} catch (NumberFormatException ex) {
						return null;
					}
				}
			};

			TableColumn<Row, Integer> column = new TableColumn<>("Value");
			column.setCellFactory(t -> {
				TextFieldTableCell<Row, Integer> cell = new TextFieldTableCell<Row, Integer>() {
					@Override
					public void commitEdit(Integer b) {
						if (b == null) {
							return;
						}
						super.commitEdit(b);
						table.getItems().get(getIndex()).setValue(b);
					}
				};
				cell.setConverter(converter);
				return cell;
			});
			column.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>((Integer) c.getValue().value));
			return column;
		} else if (array instanceof long[]) {
			LongStringConverter converter = new LongStringConverter() {
				@Override
				public Long fromString(String s) {
					try {
						return super.fromString(s);
					} catch (NumberFormatException ex) {
						return null;
					}
				}
			};

			TableColumn<Row, Long> column = new TableColumn<>("Value");
			column.setCellFactory(t -> {
				TextFieldTableCell<Row, Long> cell = new TextFieldTableCell<Row, Long>() {
					@Override
					public void commitEdit(Long b) {
						if (b == null) {
							return;
						}
						super.commitEdit(b);
						table.getItems().get(getIndex()).setValue(b);
					}
				};
				cell.setConverter(converter);
				return cell;
			});
			column.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>((Long) c.getValue().value));
			return column;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void applyArray() {
		if (array instanceof byte[]) {
			byte[] ba = (byte[]) array;
			for (int i = 0; i < ba.length; i++) {
				table.getItems().add(new Row(i, ba[i]));
			}
		} else if (array instanceof int[]) {
			int[] ia = (int[]) array;
			for (int i = 0; i < ia.length; i++) {
				table.getItems().add(new Row(i, ia[i]));
			}
		} else if (array instanceof long[]) {
			long[] la = (long[]) array;
			for (int i = 0; i < la.length; i++) {
				table.getItems().add(new Row(i, la[i]));
			}
		}
	}

	private <V extends Number> void updateArrayIndex(int index, V value) {
		Array.set(array, index, value);
	}

	private void delete() {
		int selected = table.getSelectionModel().getSelectedIndex();
		if (selected == -1) {
			return;
		}
		table.getItems().remove(selected);
		for (int i = selected; i < table.getItems().size(); i++) {
			table.getItems().get(i).index--;
		}
		deleteArrayIndex(selected);
	}

	@SuppressWarnings({"SuspiciousSystemArraycopy", "unchecked"})
	private void deleteArrayIndex(int index) {
		int length = Array.getLength(array);
		T newArray = (T) Array.newInstance(array.getClass().getComponentType(), length - 1);
		System.arraycopy(array, 0, newArray, 0, index);
		System.arraycopy(array, index + 1, newArray, index, length - index - 1);
		array = newArray;
	}

	@SuppressWarnings("unchecked")
	private void add(int amount, boolean after) {
		int selected = table.getSelectionModel().getSelectedIndex();
		List<Row> newRows = new ArrayList<>();
		int a = after ? selected != -1 ? 1 : 0 : 0;
		if (selected == -1) {
			selected = after ? table.getItems().size() : 0;
		}
		for (int i = 0; i < amount; i++) {
			newRows.add(new Row(i + selected + a, 0));
		}
		table.getItems().addAll(selected + a, newRows);
		for (int i = selected + amount + a; i < table.getItems().size(); i++) {
			table.getItems().get(i).index += amount;
		}
		addArrayIndices(selected + a, amount);
	}

	@SuppressWarnings({"SuspiciousSystemArraycopy", "unchecked"})
	private void addArrayIndices(int index, int amount) {
		int length = Array.getLength(array);
		T newArray = (T) Array.newInstance(array.getClass().getComponentType(), length + amount);
		System.arraycopy(array, 0, newArray, 0, index);
		System.arraycopy(array, index, newArray, index + amount, length - index);
		array = newArray;
	}

	public class Result {
		private T array;

		Result(T array) {
			this.array = array;
		}

		public T getArray() {
			return array;
		}
	}

	private class Row<V extends Number> {
		int index;
		V value;

		Row(int index, V value) {
			this.index = index;
			this.value = value;
		}

		void setValue(V value) {
			this.value = value;
			updateArrayIndex(index, value);
		}
	}
}
