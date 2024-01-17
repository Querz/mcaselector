package net.querz.mcaselector.ui.dialog;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.converter.ByteStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.LongStringConverter;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.math.Bits;
import net.querz.mcaselector.text.Translation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("rawtypes")
public class EditArrayDialog<T> extends Dialog<EditArrayDialog.Result> {

	private final TableView<Row> table = new TableView<>();
	private final ImageView addBefore, addAfter, add16Before, add16After, delete;
	private boolean addMultiple = false;
	private T array;
	private short[] bitValues;

	private final CheckBox overlapping = new CheckBox();
	private final ComboBox<BitCount> bits = new ComboBox<>();

	@SuppressWarnings({"SuspiciousSystemArraycopy", "unchecked"})
	public EditArrayDialog(T array, Stage primaryStage) {
		// we only want to work on a copy of the array
		this.array = (T) Array.newInstance(array.getClass().getComponentType(), Array.getLength(array));
		System.arraycopy(array, 0, this.array, 0, Array.getLength(array));

		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("array-editor-dialog-pane");
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

		setResultConverter(b -> b == ButtonType.APPLY ? new Result(this.array) : null);

		setResizable(true);

		table.setPlaceholder(new Label());
		table.getStyleClass().add("array-editor-table-view");
		table.setEditable(true);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);


		TableColumn<Row, Integer> indexColumn = new TableColumn<>();
		indexColumn.textProperty().bind(Translation.DIALOG_EDIT_ARRAY_INDEX.getProperty());
		TableColumn<Row, ? extends Number> valueColumn = createValueColumn();
		if (valueColumn == null) {
			throw new IllegalArgumentException("invalid array type, could not create column");
		}
		indexColumn.setSortable(false);
		valueColumn.setSortable(false);
		indexColumn.setResizable(false);
		valueColumn.setResizable(false);
		valueColumn.setEditable(true);
		valueColumn.prefWidthProperty().bind(table.widthProperty().subtract(70));
		indexColumn.setPrefWidth(50);

		table.getColumns().addAll(indexColumn, valueColumn);
		indexColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().index));

		// load icons
		this.addBefore = new ImageView(FileHelper.getIconFromResources("img/nbt/add_before"));
		this.add16Before = new ImageView(FileHelper.getIconFromResources("img/nbt/add_16_before"));
		this.addAfter = new ImageView(FileHelper.getIconFromResources("img/nbt/add_after"));
		this.add16After = new ImageView(FileHelper.getIconFromResources("img/nbt/add_16_after"));
		this.delete = new ImageView(FileHelper.getIconFromResources("img/delete"));

		Label addBefore = createAddRowLabel("array-editor-add-tag-label", this.addBefore, this.add16Before);
		Label addAfter = createAddRowLabel("array-editor-add-tag-label", this.addAfter, this.add16After);

		Label delete = new Label("", this.delete);
		delete.setDisable(true);
		delete.getStyleClass().add("array-editor-delete-tag-label");
		this.delete.setPreserveRatio(true);
		delete.setOnMouseEntered(e -> {
			if (!delete.isDisabled()) {
				this.delete.setFitWidth(24);
			}
		});
		delete.setOnMouseExited(e -> {
			if (!delete.isDisabled()) {
				this.delete.setFitWidth(22);
			}
		});
		delete.disableProperty().addListener((i, o, n) -> {
			if (o.booleanValue() != n.booleanValue()) {
				if (n) {
					delete.getStyleClass().remove("array-editor-delete-tag-label-enabled");
				} else {
					delete.getStyleClass().add("array-editor-delete-tag-label-enabled");
				}
			}
		});

		table.getSelectionModel().selectedItemProperty().addListener(
				(i, o, n) -> delete.setDisable(n == null || bits.getValue() != null && bits.getValue() != BitCount.NONE));

		getDialogPane().setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.SHIFT) {
				addBefore.setGraphic(this.add16Before);
				addAfter.setGraphic(this.add16After);
				addMultiple = true;
			}
		});

		getDialogPane().setOnKeyReleased(e -> {
			if (e.getCode() == KeyCode.SHIFT) {
				addBefore.setGraphic(this.addBefore);
				addAfter.setGraphic(this.addAfter);
				addMultiple = false;
			}
		});

		getDialogPane().getScene().getWindow().sizeToScene();

		((Stage) getDialogPane().getScene().getWindow()).setMinWidth(array instanceof long[] ? 324 : 200);
		((Stage) getDialogPane().getScene().getWindow()).setMinHeight(500);

		addAfter.setOnMouseClicked(e -> add(addMultiple ? 16 : 1, true));
		addBefore.setOnMouseClicked(e -> add(addMultiple ? 16 : 1, false));
		delete.setOnMouseClicked(e -> delete());

		bits.getItems().addAll(BitCount.values());
		bits.setPromptText("bits");

		bits.valueProperty().addListener((v, o, n) -> {
			if (n == null) {
				// do nothing when clearing the selection, because we already are on BitCount.NONE
				return;
			} else if (n == BitCount.NONE) {
				Platform.runLater(() -> bits.getSelectionModel().clearSelection());
				addBefore.setDisable(false);
				addAfter.setDisable(false);
				overlapping.setDisable(true);
				delete.setDisable(false);
			} else {
				addBefore.setDisable(true);
				addAfter.setDisable(true);
				overlapping.setDisable(64 % n.bits == 0);
				delete.setDisable(true);
			}
			reloadBitValues(n);
			table.getItems().clear();
			applyArray();
		});

		Label overlap = new Label("overlap:");
		overlap.getStyleClass().add("overlap-label");

		overlapping.selectedProperty().addListener((v, o, n) -> {
			reloadBitValues(bits.getValue());
			table.getItems().clear();
			applyArray();
		});

		overlapping.setDisable(true);

		HBox options = new HBox();
		options.getStyleClass().add("array-editor-options");
		options.getChildren().addAll(delete, new Separator(), addBefore, addAfter);

		if (array instanceof long[]) {
			options.getChildren().addAll(new Separator(), bits, new Separator(), overlap, overlapping);
		}

		BorderPane content = new BorderPane();
		BorderPane.setAlignment(table, Pos.TOP_LEFT);
		content.setCenter(table);
		content.setBottom(options);

		applyArray();

		getDialogPane().setContent(content);

		getDialogPane().getStylesheets().add(EditArrayDialog.class.getClassLoader().getResource("style/component/edit-array-dialog.css").toExternalForm());
	}

	private void reloadBitValues(BitCount bits) {
		if (bits != BitCount.NONE) {
			if (overlapping.isSelected()) {
				long[] la = (long[]) array;
				int bitValuesLength = (int) (64D / bits.bits * la.length);
				bitValues = new short[bitValuesLength];
				for (int i = 0; i < bitValuesLength; i++) {
					double blockStatesIndex = i / ((double) bitValuesLength / la.length);
					int longIndex = (int) blockStatesIndex;
					int startBit = (int) ((blockStatesIndex - Math.floor(blockStatesIndex)) * 64D);
					if (startBit + bits.bits > 64) {
						long prev = Bits.bitRange(la[longIndex], startBit, 64);
						long next = Bits.bitRange(la[longIndex + 1], 0, startBit + bits.bits - 64);
						bitValues[i] = (short) ((next << 64 - startBit) + prev);
					} else {
						bitValues[i] = (short) Bits.bitRange(la[longIndex], startBit, startBit + bits.bits);
					}
				}
			} else {
				long[] la = (long[]) array;
				int indicesPerLong = Math.floorDiv(64, bits.bits);
				int bitValuesLength = la.length * indicesPerLong;
				bitValues = new short[bitValuesLength];
				for (int i = 0; i < bitValuesLength; i++) {
					int blockStatesIndex = i / indicesPerLong;
					int startBit = (i % indicesPerLong) * bits.bits;
					bitValues[i] = (short) Bits.bitRange(la[blockStatesIndex], startBit, startBit + bits.bits);
				}
			}
		} else {
			bitValues = null;
		}
	}

	private Label createAddRowLabel(String styleClass, ImageView icon, ImageView altIcon) {
		Label label = new Label("", icon);
		label.getStyleClass().add(styleClass);
		icon.setPreserveRatio(true);
		label.setOnMouseEntered(e -> {
			icon.setFitWidth(18);
			altIcon.setFitWidth(18);
		});
		label.setOnMouseExited(e -> {
			icon.setFitWidth(16);
			altIcon.setFitWidth(16);
		});
		return label;
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

			TableColumn<Row, Byte> column = new TableColumn<>();
			column.textProperty().bind(Translation.DIALOG_EDIT_ARRAY_VALUE.getProperty());
			column.setCellFactory(t -> {
				TextFieldTableCell<Row, Byte> cell = new TextFieldTableCell<>() {
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
			column.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().value.byteValue()));
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

			TableColumn<Row, Integer> column = new TableColumn<>();
			column.textProperty().bind(Translation.DIALOG_EDIT_ARRAY_VALUE.getProperty());
			column.setCellFactory(t -> {
				TextFieldTableCell<Row, Integer> cell = new TextFieldTableCell<>() {
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
			column.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().value.intValue()));
			return column;
		} else if (array instanceof long[]) {
			LongStringConverter converter = new LongStringConverter() {
				@Override
				public Long fromString(String s) {
					try {
						long l = super.fromString(s);
						if (bits.getValue() != BitCount.NONE) {
							int max = (int) Math.pow(2, bits.getValue().bits);
							if (l < max && l >= 0) {
								return l;
							} else {
								return null;
							}
						}
						return l;
					} catch (NumberFormatException ex) {
						return null;
					}
				}
			};

			TableColumn<Row, Long> column = new TableColumn<>();
			column.textProperty().bind(Translation.DIALOG_EDIT_ARRAY_VALUE.getProperty());
			column.setCellFactory(t -> {
				TextFieldTableCell<Row, Long> cell = new TextFieldTableCell<>() {
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
			column.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().value.longValue()));
			return column;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void applyArray() {
		if (array instanceof byte[] ba) {
			for (int i = 0; i < ba.length; i++) {
				table.getItems().add(new Row(i, ba[i]));
			}
		} else if (array instanceof int[] ia) {
			for (int i = 0; i < ia.length; i++) {
				table.getItems().add(new Row(i, ia[i]));
			}
		} else if (array instanceof long[] la) {
			if (bits.getValue() == BitCount.NONE || bits.getValue() == null) {
				for (int i = 0; i < la.length; i++) {
					table.getItems().add(new Row(i, la[i]));
				}
			} else {
				for (int i = 0; i < bitValues.length; i++) {
					table.getItems().add(new Row(i, bitValues[i]));
				}
			}
		}
	}

	private <V extends Number> void updateArrayIndex(int index, V value) {
		if (array instanceof long[] && bits.getValue() != BitCount.NONE) {
			if (overlapping.isSelected()) {
				long[] l = (long[]) array;
				int bits = this.bits.getValue().bits;
				double blockStatesIndex = index / ((double) bitValues.length / l.length);
				int longIndex = (int) blockStatesIndex;
				int startBit = (int) ((blockStatesIndex - Math.floor(longIndex)) * 64D);
				if (startBit + bits > 64) {
					l[longIndex] = Bits.setBits(value.shortValue(), l[longIndex], startBit, 64);
					l[longIndex + 1] = Bits.setBits(value.shortValue() >> (64 - startBit), l[longIndex + 1], 0, startBit + bits - 64);
				} else {
					l[longIndex] = Bits.setBits(value.shortValue(), l[longIndex], startBit, startBit + bits);
				}
			} else {
				long[] l = (long[]) array;
				int bits = this.bits.getValue().bits;
				int indicesPerLong = (int) (64D / bits);
				int blockStatesIndex = index / indicesPerLong;
				int startBit = (index % indicesPerLong) * bits;
				l[blockStatesIndex] = Bits.setBits(value.longValue(), l[blockStatesIndex], startBit, startBit + bits);
			}
			bitValues[index] = value.shortValue();
		} else {
			Array.set(array, index, value);
		}
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
		private final T array;

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

	private enum BitCount {
		NONE(-1, ""),
		ONE(1),
		TWO(2),
		THREE(3),
		FOUR(4),
		FIVE(5),
		SIX(6),
		SEVEN(7),
		EIGHT(8),
		NINE(9),
		TEN(10),
		ELEVEN(11),
		TWELVE(12),
		THIRTY_TWO(32);

		final int bits;
		final String string;

		BitCount(int bits) {
			this(bits, "" + bits);
		}

		BitCount(int bits, String string) {
			this.bits = bits;
			this.string = string;
		}

		@Override
		public String toString() {
			return string;
		}
	}
}
