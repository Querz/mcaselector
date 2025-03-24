package net.querz.mcaselector.ui.dialog;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.converter.ByteStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.LongStringConverter;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.component.NBTTreeView;
import net.querz.mcaselector.util.math.Bits;
import java.lang.reflect.Array;
import java.util.Objects;
import java.util.Optional;

public class EditArrayDialog extends Dialog<NBTTreeView.EditArrayResult> {

	private final TableView<Row> table = new TableView<>();
	private final ComboBox<BitCount> bits = new ComboBox<>();
	private final CheckBox overlapping = new CheckBox();
	private static final Image editIcon = FileHelper.getIconFromResources("img/edit");

	private final Object handle;
	private int[] values;

	@SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
	public EditArrayDialog(Object array, Stage stage) {
		Class<?> clazz = array.getClass();
		if (clazz != long[].class && clazz != int[].class && clazz != byte[].class) {
			throw new IllegalArgumentException("invalid or unsupported array type: " + clazz);
		}
		handle = Array.newInstance(array.getClass().getComponentType(), Array.getLength(array));
		System.arraycopy(array, 0, handle, 0, Array.getLength(array));

		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("edit-array-dialog-pane");
		getDialogPane().getStyleClass().addAll(stage.getScene().getStylesheets());
		getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

		setResizable(true);
		setResultConverter(a -> a == ButtonType.APPLY ? new NBTTreeView.EditArrayResult(handle) : null);

		table.setPlaceholder(new Label());
		table.getStyleClass().add("edit-array-table-view");
		table.setEditable(true);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

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

		BorderPane content = new BorderPane();
		BorderPane.setAlignment(table, Pos.TOP_LEFT);
		content.setCenter(table);

		Button editButton = new Button(null, new ImageView(editIcon));
		editButton.setOnAction(e -> {
			if (table.getSelectionModel().getSelectedItems() != null) {
				if (table.getSelectionModel().getSelectedItems().size() > 1) {
					long min = 0, max = 0;
					BitCount b = bits.getSelectionModel().getSelectedItem();
					if (b == null || b == BitCount.NONE) {
						if (handle instanceof byte[]) {
							min = Byte.MIN_VALUE;
							max = Byte.MAX_VALUE;
						} else if (handle instanceof int[]) {
							min = Integer.MIN_VALUE;
							max = Integer.MAX_VALUE;
						} else if (handle instanceof long[]) {
							min = Long.MIN_VALUE;
							max = Long.MAX_VALUE;
						}
					} else {
						max = b.bits == 1 ? 1 : (long) Math.pow(2, b.bits);
					}
					Optional<Long> n = new RequestNumberDialog(stage, Translation.DIALOG_REQUEST_NUMBER_TITLE_ENTER_NUMBER, min, max).showAndWait();
					n.ifPresent(v -> table.getSelectionModel().getSelectedItems().forEach(r -> r.setValue(v)));
					table.refresh();
				} else {
					table.edit(table.getSelectionModel().getSelectedIndex(), valueColumn);
				}
			}
		});
		table.getSelectionModel().selectedItemProperty().addListener((v, o, n) -> editButton.setDisable(n == null));
		editButton.setDisable(true);

		HBox options = new HBox();
		options.getStyleClass().add("edit-array-options");
		options.getChildren().add(editButton);

		if (handle instanceof long[]) {
			bits.getItems().addAll(BitCount.values());
			bits.setPromptText("bits");
			bits.valueProperty().addListener((v, o, n) -> {
				if (n == null) {
					return;
				} else if (n == BitCount.NONE) {
					Platform.runLater(() -> bits.getSelectionModel().clearSelection());
					overlapping.setDisable(true);
				} else {
					overlapping.setDisable(64 % n.bits == 0);
				}
				loadBitValues(n);
				table.getItems().clear();
				createRows();
			});
			Label overlap = new Label("overlapping:");
			overlap.getStyleClass().add("overlap-label");
			overlapping.selectedProperty().addListener((v, o, n) -> {
				loadBitValues(bits.getValue());
				table.getItems().clear();
				createRows();
			});
			overlapping.setDisable(true);
			options.getChildren().addAll(new Separator(), bits, new Separator(), overlap, overlapping);
		}

		content.setBottom(options);

		createRows();

		getDialogPane().setContent(content);
		getDialogPane().getStylesheets().addAll(stage.getScene().getStylesheets());
		getDialogPane().getStylesheets().add(Objects.requireNonNull(
				EditArrayDialog.class.getClassLoader().getResource("style/component/edit-array-dialog.css")).toExternalForm());
	}

	private TableColumn<Row, ? extends Number> createValueColumn() {
		if (handle instanceof byte[]) {
			ByteStringConverter converter = new ByteStringConverter() {
				@Override
				public Byte fromString(String s) {
					try {
						return super.fromString(s);
					} catch (NumberFormatException e) {
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
						table.getItems().get(getItem()).setValue(b);
					}
				};
				cell.setConverter(converter);
				return cell;
			});
			column.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().value.byteValue()));
			return column;
		} else if (handle instanceof int[]) {
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
		} else if (handle instanceof long[]) {
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

	private void loadBitValues(BitCount bits) {
		if (bits != BitCount.NONE) {
			if (overlapping.isSelected()) {
				long[] la = (long[]) handle;
				int bitValuesLength = (int) (64D / bits.bits * la.length);
				values = new int[bitValuesLength];
				for (int i = 0; i < bitValuesLength; i++) {
					double blockStatesIndex = i / ((double) bitValuesLength / la.length);
					int longIndex = (int) blockStatesIndex;
					int startBit = (int) ((blockStatesIndex - Math.floor(blockStatesIndex)) * 64D);
					if (startBit + bits.bits > 64) {
						long prev = Bits.bitRange(la[longIndex], startBit, 64);
						long next = Bits.bitRange(la[longIndex + 1], 0, startBit + bits.bits - 64);
						values[i] = (int) ((next << 64 - startBit) + prev);
					} else {
						values[i] = (int) Bits.bitRange(la[longIndex], startBit, startBit + bits.bits);
					}
				}
			} else {
				long[] la = (long[]) handle;
				int indicesPerLong = Math.floorDiv(64, bits.bits);
				int bitValuesLength = la.length * indicesPerLong;
				values = new int[bitValuesLength];
				for (int i = 0; i < bitValuesLength; i++) {
					int blockStatesIndex = i / indicesPerLong;
					int startBit = (i % indicesPerLong) * bits.bits;
					values[i] = (int) Bits.bitRange(la[blockStatesIndex], startBit, startBit + bits.bits);
				}
			}
		} else {
			values = null;
		}
	}

	private void createRows() {
		if (handle instanceof byte[] ba) {
			for (int i = 0; i < ba.length; i++) {
				table.getItems().add(new Row(i, ba[i]));
			}
		} else if (handle instanceof int[] ia) {
			for (int i = 0; i < ia.length; i++) {
				table.getItems().add(new Row(i, ia[i]));
			}
		} else if (handle instanceof long[] la) {
			if (bits.getValue() == BitCount.NONE || bits.getValue() == null) {
				for (int i = 0; i < la.length; i++) {
					table.getItems().add(new Row(i, la[i]));
				}
			} else {
				for (int i = 0; i < values.length; i++) {
					table.getItems().add(new Row(i, values[i]));
				}
			}
		}
	}


	class Row {

		int index;
		Number value;

		Row(int index, Number value) {
			this.index = index;
			this.value = value;
		}

		void setValue(Number value) {
			this.value = value;
			// update handle AND values
			if (handle instanceof long[] hndl && bits.getValue() != BitCount.NONE && bits.getValue() != null) {
				if (overlapping.isSelected()) {
					int bits = EditArrayDialog.this.bits.getValue().bits;
					double blockStatesIndex = index / ((double) values.length / hndl.length);
					int longIndex = (int) blockStatesIndex;
					int startBit = (int) ((blockStatesIndex - (double) longIndex) * 64D);
					if (startBit + bits > 64) {
						hndl[longIndex] = Bits.setBits(value.shortValue(), hndl[longIndex], startBit, 64);
						hndl[longIndex + 1] = Bits.setBits(value.shortValue() >> (64 - startBit),
								hndl[longIndex + 1], 0, startBit + bits - 64);
					} else {
						hndl[longIndex] = Bits.setBits(value.shortValue(), hndl[longIndex], startBit, startBit + bits);
					}
				} else {
					int bits = EditArrayDialog.this.bits.getValue().bits;
					int indicesPerLong = (int) (64D / bits);
					int blockStatesIndex = index / indicesPerLong;
					int startBit = (index % indicesPerLong) * bits;
					hndl[blockStatesIndex] = Bits.setBits(value.longValue(), hndl[blockStatesIndex], startBit, startBit + bits);
				}
				values[index] = value.shortValue();
			} else {
				switch (handle) {
				case long[] longs -> longs[index] = value.longValue();
				case int[] ints -> ints[index] = value.intValue();
				case byte[] bytes -> bytes[index] = value.byteValue();
				default -> throw new IllegalStateException("unexpected value: " + handle.getClass());
				}
			}
		}
	}

	enum BitCount {
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
